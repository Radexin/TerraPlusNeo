package de.btegermany.terraplusminus.gen;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.btegermany.terraplusminus.Config;
import net.buildtheearth.terraminusminus.generator.CachedChunkData;
import net.buildtheearth.terraminusminus.generator.ChunkDataLoader;
import net.buildtheearth.terraminusminus.generator.EarthGeneratorSettings;
import net.buildtheearth.terraminusminus.projection.GeographicProjection;
import net.buildtheearth.terraminusminus.projection.transform.OffsetProjectionTransform;
import net.buildtheearth.terraminusminus.substitutes.ChunkPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.min;
import static net.buildtheearth.terraminusminus.substitutes.ChunkPos.blockToCube;
import static net.buildtheearth.terraminusminus.substitutes.ChunkPos.cubeToMinBlock;

public class RealWorldGenerator extends ChunkGenerator {

    public static final MapCodec<RealWorldGenerator> CODEC = RecordCodecBuilder.mapCodec((instance) -> {
        return instance.group(
                BiomeSource.CODEC.fieldOf("biome_source").forGetter((generator) -> generator.biomeSource),
                Codec.INT.fieldOf("y_offset").orElse(0).forGetter((generator) -> generator.yOffset)
        ).apply(instance, RealWorldGenerator::new);
    });

    public final EarthGeneratorSettings settings;
    public final int yOffset;
    private final LoadingCache<ChunkPos, CompletableFuture<CachedChunkData>> cache;
    private final Map<String, BlockState> materialMapping;

    private static final Set<BlockState> GRASS_LIKE_BLOCKS = Set.of(
            Blocks.GRASS_BLOCK.defaultBlockState(),
            Blocks.DIRT_PATH.defaultBlockState(),
            Blocks.FARMLAND.defaultBlockState(),
            Blocks.MYCELIUM.defaultBlockState(),
            Blocks.SNOW.defaultBlockState()
    );

    public RealWorldGenerator(BiomeSource biomeSource, int yOffset) {
        super(biomeSource);
        
        this.yOffset = yOffset == 0 ? Config.terrainOffsetY : yOffset;
        
        EarthGeneratorSettings baseSettings = EarthGeneratorSettings.parse(EarthGeneratorSettings.BTE_DEFAULT_SETTINGS);
        GeographicProjection projection = new OffsetProjectionTransform(
                baseSettings.projection(),
                Config.terrainOffsetX,
                Config.terrainOffsetZ
        );
        
        this.settings = baseSettings.withProjection(projection);
        
        // If our biome source is a CustomBiomeProvider, set its projection
        if (biomeSource instanceof CustomBiomeProvider customBiomeProvider) {
            customBiomeProvider.setProjection(projection);
        }
        
        this.cache = CacheBuilder.newBuilder()
                .expireAfterAccess(5L, TimeUnit.MINUTES)
                .softValues()
                .build(new ChunkDataLoader(this.settings));

        this.materialMapping = Map.of(
                "minecraft:bricks", Config.buildingOutlinesMaterial.defaultBlockState(),
                "minecraft:gray_concrete", Config.roadMaterial.defaultBlockState(),
                "minecraft:dirt_path", Config.pathMaterial.defaultBlockState()
        );
    }

    @Override
    public MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving step) {
        // No caves, because caves scary
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structureManager, RandomState randomState, ChunkAccess chunk) {
        // Initialize biome provider with registry if needed
        if (biomeSource instanceof CustomBiomeProvider customBiomeProvider && !customBiomeProvider.isRegistryInitialized()) {
            customBiomeProvider.setBiomeRegistry(region.registryAccess().lookupOrThrow(Registries.BIOME));
        }
        
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;
        CachedChunkData terraData = this.getTerraChunkData(chunkX, chunkZ);
        
        int minY = region.getMinBuildHeight();
        int maxY = region.getMaxBuildHeight();
        
        // Use the level seed from the world for deterministic generation
        long worldSeed = region.getSeed();
        Random random = new Random(worldSeed + (long)chunkX * 341873128712L + (long)chunkZ * 132897987541L);
        
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkX * 16 + x;
                int worldZ = chunkZ * 16 + z;
                
                int groundY = terraData.groundHeight(x, z) + this.yOffset;
                int startMountainHeight = random.nextInt(7500, 7520);
                
                if (groundY < minY || groundY >= maxY) {
                    continue;
                }
                
                BlockState material;
                net.buildtheearth.terraminusminus.substitutes.BlockState terraState = terraData.surfaceBlock(x, z);
                
                if (terraState != null) {
                    material = this.materialMapping.get(terraState.getBlock().toString());
                    if (material == null) {
                        material = convertTerraBlockState(terraState);
                    }
                } else if (groundY >= startMountainHeight) {
                    material = Blocks.STONE.defaultBlockState();
                } else {
                    BlockPos pos = new BlockPos(worldX, groundY, worldZ);
                    Holder<Biome> biome = chunk.getNoiseBiome(x >> 2, groundY >> 2, z >> 2);
                    
                    if (biome.is(Biomes.DESERT)) {
                        material = Blocks.SAND.defaultBlockState();
                    } else if (biome.is(Biomes.SNOWY_SLOPES) || biome.is(Biomes.SNOWY_PLAINS) || biome.is(Biomes.FROZEN_PEAKS)) {
                        material = Blocks.SNOW_BLOCK.defaultBlockState();
                    } else {
                        material = Config.surfaceMaterial.defaultBlockState();
                    }
                }
                
                boolean isUnderWater = groundY + 1 >= maxY || chunk.getBlockState(new BlockPos(worldX, groundY + 1, worldZ)).is(Blocks.WATER);
                if (isUnderWater && GRASS_LIKE_BLOCKS.contains(material)) {
                    material = Blocks.DIRT.defaultBlockState();
                }
                
                chunk.setBlockState(new BlockPos(worldX, groundY, worldZ), material, false);
            }
        }
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        // Trees will be handled through features if enabled
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        return CompletableFuture.supplyAsync(() -> {
            int chunkX = chunk.getPos().x;
            int chunkZ = chunk.getPos().z;
            CachedChunkData terraData = this.getTerraChunkData(chunkX, chunkZ);
            
            int minY = chunk.getMinBuildHeight();
            int maxY = chunk.getMaxBuildHeight();
            
            int minSurfaceCubeY = blockToCube(minY - this.yOffset);
            int maxWorldCubeY = blockToCube(maxY - this.yOffset);
            
            if (terraData.aboveSurface(minSurfaceCubeY)) {
                return chunk;
            }
            
            while (minSurfaceCubeY < maxWorldCubeY && terraData.belowSurface(minSurfaceCubeY)) {
                minSurfaceCubeY++;
            }
            
            if (minSurfaceCubeY >= maxWorldCubeY) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = minY; y < maxY; y++) {
                            chunk.setBlockState(new BlockPos(chunkX * 16 + x, y, chunkZ * 16 + z), Blocks.STONE.defaultBlockState(), false);
                        }
                    }
                }
                return chunk;
            } else {
                int fillUpToY = cubeToMinBlock(minSurfaceCubeY) + this.yOffset;
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = minY; y < fillUpToY && y < maxY; y++) {
                            chunk.setBlockState(new BlockPos(chunkX * 16 + x, y, chunkZ * 16 + z), Blocks.STONE.defaultBlockState(), false);
                        }
                    }
                }
            }
            
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int worldX = chunkX * 16 + x;
                    int worldZ = chunkZ * 16 + z;
                    
                    int groundHeight = min(terraData.groundHeight(x, z) + this.yOffset, maxY - 1);
                    int waterHeight = min(terraData.waterHeight(x, z) + this.yOffset, maxY - 1);
                    
                    for (int y = minY; y <= groundHeight && y < maxY; y++) {
                        chunk.setBlockState(new BlockPos(worldX, y, worldZ), Blocks.STONE.defaultBlockState(), false);
                    }
                    
                    for (int y = groundHeight + 1; y <= waterHeight && y < maxY; y++) {
                        chunk.setBlockState(new BlockPos(worldX, y, worldZ), Blocks.WATER.defaultBlockState(), false);
                    }
                }
            }
            
            return chunk;
        });
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types heightmapType, LevelHeightAccessor level, RandomState randomState) {
        int chunkX = blockToCube(x);
        int chunkZ = blockToCube(z);
        int localX = x - cubeToMinBlock(chunkX);
        int localZ = z - cubeToMinBlock(chunkZ);
        
        CachedChunkData terraData = this.getTerraChunkData(chunkX, chunkZ);
        
        return switch (heightmapType) {
            case OCEAN_FLOOR, OCEAN_FLOOR_WG -> terraData.groundHeight(localX, localZ) + this.yOffset;
            default -> terraData.surfaceHeight(localX, localZ) + this.yOffset;
        };
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
        BlockState[] states = new BlockState[level.getHeight()];
        int baseHeight = getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR, level, randomState);
        
        for (int y = level.getMinBuildHeight(); y < baseHeight && y < level.getMaxBuildHeight(); y++) {
            states[y - level.getMinBuildHeight()] = Blocks.STONE.defaultBlockState();
        }
        
        return new NoiseColumn(level.getMinBuildHeight(), states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
        info.add("Terra-- Real World Generator");
        info.add("Y Offset: " + this.yOffset);
    }

    @Override
    public void createStructures(RegistryAccess registryAccess, ChunkGeneratorStructureState structureState, StructureManager structureManager, ChunkAccess chunk, StructureTemplateManager templateManager) {
        // No structures
    }

    @Override
    public void createReferences(WorldGenLevel level, StructureManager structureManager, ChunkAccess chunk) {
        // No structure references
    }

    @Override
    public int getGenDepth() {
        return 384;
    }

    @Override
    public int getSeaLevel() {
        return 63 + this.yOffset;
    }

    @Override
    public int getMinY() {
        return -64;
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        // No mob spawning
    }

    private CachedChunkData getTerraChunkData(int chunkX, int chunkZ) {
        try {
            return this.cache.getUnchecked(new ChunkPos(chunkX, chunkZ)).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new CompletionException("Failed to generate chunk data", e);
        }
    }

    private BlockState convertTerraBlockState(net.buildtheearth.terraminusminus.substitutes.BlockState terraState) {
        String blockName = terraState.getBlock().toString();
        return switch (blockName) {
            case "minecraft:stone" -> Blocks.STONE.defaultBlockState();
            case "minecraft:dirt" -> Blocks.DIRT.defaultBlockState();
            case "minecraft:grass_block" -> Blocks.GRASS_BLOCK.defaultBlockState();
            case "minecraft:water" -> Blocks.WATER.defaultBlockState();
            case "minecraft:sand" -> Blocks.SAND.defaultBlockState();
            case "minecraft:gravel" -> Blocks.GRAVEL.defaultBlockState();
            case "minecraft:bricks" -> Blocks.BRICKS.defaultBlockState();
            case "minecraft:gray_concrete" -> Blocks.GRAY_CONCRETE.defaultBlockState();
            case "minecraft:dirt_path" -> Blocks.DIRT_PATH.defaultBlockState();
            default -> Blocks.STONE.defaultBlockState();
        };
    }
} 