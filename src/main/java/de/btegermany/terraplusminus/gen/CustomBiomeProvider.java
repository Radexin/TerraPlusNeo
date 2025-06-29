package de.btegermany.terraplusminus.gen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.btegermany.terraplusminus.Config;
import de.btegermany.terraplusminus.data.KoppenClimateData;
import net.buildtheearth.terraminusminus.projection.GeographicProjection;
import net.buildtheearth.terraminusminus.projection.OutOfProjectionBoundsException;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

public class CustomBiomeProvider extends BiomeSource {

    public static final MapCodec<CustomBiomeProvider> CODEC = MapCodec.unit(CustomBiomeProvider::new);

    private final KoppenClimateData climateData = new KoppenClimateData();
    private GeographicProjection projection;
    private final BiomeSource fallbackBiomeSource;
    private HolderGetter<Biome> biomeRegistry;

    // Static list of biomes used by this provider
    private static final List<ResourceKey<Biome>> BIOME_KEYS = List.of(
            Biomes.OCEAN, Biomes.JUNGLE, Biomes.BAMBOO_JUNGLE, Biomes.SPARSE_JUNGLE, 
            Biomes.SAVANNA, Biomes.DESERT, Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, 
            Biomes.BEACH, Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.FLOWER_FOREST, 
            Biomes.STONY_PEAKS, Biomes.SAVANNA_PLATEAU, Biomes.WOODED_BADLANDS, 
            Biomes.SNOWY_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA, Biomes.SWAMP, 
            Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.FOREST, Biomes.DARK_FOREST,
            Biomes.TAIGA, Biomes.FROZEN_PEAKS, Biomes.SNOWY_PLAINS, Biomes.ICE_SPIKES,
            Biomes.WINDSWEPT_HILLS, Biomes.SNOWY_SLOPES
    );

    private List<? extends Holder<Biome>> possibleBiomes;

    public CustomBiomeProvider() {
        this(createDefaultBiomeSource());
    }

    public CustomBiomeProvider(BiomeSource fallbackBiomeSource) {
        this.fallbackBiomeSource = fallbackBiomeSource;
    }

    private static BiomeSource createDefaultBiomeSource() {
        // Create a simple fixed biome source as fallback
        return new BiomeSource() {
            @Override
            protected MapCodec<? extends BiomeSource> codec() {
                return null; // Not serializable
            }

            @Override
            protected Stream<Holder<Biome>> collectPossibleBiomes() {
                return Stream.empty();
            }

            @Override
            public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
                return null; // Will never be used
            }
        };
    }

    public CustomBiomeProvider(GeographicProjection projection) {
        this(createDefaultBiomeSource());
        this.projection = projection;
    }

    public void setProjection(GeographicProjection projection) {
        this.projection = projection;
    }

    public void setBiomeRegistry(HolderGetter<Biome> biomeRegistry) {
        this.biomeRegistry = biomeRegistry;
        this.possibleBiomes = BIOME_KEYS.stream()
                .map(biomeRegistry::getOrThrow)
                .toList();
    }
    
    public boolean isRegistryInitialized() {
        return this.biomeRegistry != null;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        if (possibleBiomes != null) {
            return possibleBiomes.stream().map(holder -> (Holder<Biome>) holder);
        }
        // Return empty stream if not initialized yet
        return Stream.empty();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        // Convert from biome coordinates (4x4x4 blocks) to world coordinates
        int worldX = QuartPos.toBlock(x);
        int worldZ = QuartPos.toBlock(z);
        
        // Initialize biome registry if not already done
        if (biomeRegistry == null && sampler != null) {
            // Try to get biome registry from the sampler context
            // This is a fallback initialization
            initializeBiomeRegistry();
        }
        
        if (Config.differentBiomes && projection != null) {
            double[] coords;
            try {
                coords = this.projection.toGeo(worldX, worldZ);
            } catch (OutOfProjectionBoundsException ignored) {
                return getDefaultBiome();
            }
            
            try {
                double biomeData = this.climateData.getAsync(coords[0], coords[1]).get();
                ResourceKey<Biome> biomeKey = koppenDataToBiome(biomeData);
                return getBiomeHolderSafe(biomeKey);
            } catch (InterruptedException | ExecutionException | OutOfProjectionBoundsException e) {
                e.printStackTrace();
                return getDefaultBiome();
            }
        }
        
        return getDefaultBiome();
    }
    
    private void initializeBiomeRegistry() {
        // This method will be called when we need to initialize the registry
        // In practice, the registry should be set by the chunk generator
    }
    
    private Holder<Biome> getDefaultBiome() {
        // Return a default biome holder when registry is not available
        if (possibleBiomes != null && !possibleBiomes.isEmpty()) {
            // Find plains biome in our possible biomes
            for (Holder<Biome> holder : possibleBiomes) {
                if (holder.unwrapKey().map(key -> key.equals(Biomes.PLAINS)).orElse(false)) {
                    return holder;
                }
            }
            // If plains not found, return first available
            return possibleBiomes.get(0);
        }
        // Last resort - this shouldn't happen in normal operation
        return null;
    }
    
    private Holder<Biome> getBiomeHolderSafe(ResourceKey<Biome> biomeKey) {
        if (biomeRegistry != null) {
            return biomeRegistry.getOrThrow(biomeKey);
        }
        // Fallback to default biome
        return getDefaultBiome();
    }

    private Holder<Biome> getBiomeHolder(ResourceKey<Biome> biomeKey) {
        if (biomeRegistry != null) {
            return biomeRegistry.getOrThrow(biomeKey);
        }
        // Fallback - this shouldn't happen in normal operation
        return possibleBiomes != null && !possibleBiomes.isEmpty() ? possibleBiomes.get(0) : null;
    }

    public static ResourceKey<Biome> koppenDataToBiome(double koppenData) {
        return switch ((int) koppenData) {
            case 0 -> Biomes.OCEAN;
            case 1, 12 -> Biomes.JUNGLE;
            case 2 -> Biomes.BAMBOO_JUNGLE;
            case 3, 11 -> Biomes.SPARSE_JUNGLE;
            case 4, 5, 7 -> Biomes.DESERT;
            case 6 -> Biomes.SAVANNA;
            case 8 -> Biomes.PLAINS;
            case 9 -> Biomes.SUNFLOWER_PLAINS;
            case 10 -> Biomes.BEACH;
            case 13 -> Biomes.WINDSWEPT_GRAVELLY_HILLS;
            case 14, 15 -> Biomes.FLOWER_FOREST;
            case 16 -> Biomes.WINDSWEPT_HILLS;
            case 17 -> Biomes.SAVANNA_PLATEAU;
            case 18 -> Biomes.WOODED_BADLANDS;
            case 19 -> Biomes.SNOWY_TAIGA;
            case 20 -> Biomes.OLD_GROWTH_PINE_TAIGA;
            case 21, 22 -> Biomes.SWAMP;
            case 23, 24 -> Biomes.OLD_GROWTH_SPRUCE_TAIGA;
            case 25 -> Biomes.FOREST;
            case 26 -> Biomes.DARK_FOREST;
            case 27 -> Biomes.TAIGA;
            case 28 -> Biomes.SNOWY_SLOPES;
            case 29 -> Biomes.SNOWY_PLAINS;
            case 30 -> Biomes.ICE_SPIKES;
            default -> Biomes.PLAINS;
        };
    }
}
