package de.btegermany.terraplusminus.gen;

import de.btegermany.terraplusminus.Terraplusminus;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.LevelStem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class EarthWorldType {
    // Create a deferred register for world presets
    public static final DeferredRegister<LevelStem> WORLD_TYPES = 
            DeferredRegister.create(Registries.LEVEL_STEM, Terraplusminus.MODID);
    
    // We'll need to properly register the world type in a data generation phase
    // For now, this sets up the basic registration structure
    public static final ResourceKey<LevelStem> EARTH_TYPE = ResourceKey.create(
            Registries.LEVEL_STEM,
            ResourceLocation.fromNamespaceAndPath(Terraplusminus.MODID, "earth")
    );
} 