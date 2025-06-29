package de.btegermany.terraplusminus;

import com.mojang.logging.LogUtils;
import de.btegermany.terraplusminus.commands.OffsetCommand;
import de.btegermany.terraplusminus.commands.TpllCommand;
import de.btegermany.terraplusminus.commands.WhereCommand;
import de.btegermany.terraplusminus.events.PlayerEventHandler;
import de.btegermany.terraplusminus.gen.EarthWorldType;
import de.btegermany.terraplusminus.gen.RealWorldGenerator;
import de.btegermany.terraplusminus.gen.CustomBiomeProvider;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import com.mojang.serialization.MapCodec;
import org.slf4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Mod(Terraplusminus.MODID)
@EventBusSubscriber(modid = Terraplusminus.MODID)
public class Terraplusminus {
    public static final String MODID = "terraplusminus";
    private static final Logger LOGGER = LogUtils.getLogger();
    public static Terraplusminus instance;
    
    // Registries for worldgen
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS = 
            DeferredRegister.create(Registries.CHUNK_GENERATOR, MODID);
    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES = 
            DeferredRegister.create(Registries.BIOME_SOURCE, MODID);
    
    // Register our custom types
    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<RealWorldGenerator>> EARTH_CHUNK_GENERATOR =
            CHUNK_GENERATORS.register("earth", () -> RealWorldGenerator.CODEC);
    public static final DeferredHolder<MapCodec<? extends BiomeSource>, MapCodec<CustomBiomeProvider>> EARTH_BIOME_SOURCE =
            BIOME_SOURCES.register("earth", () -> CustomBiomeProvider.CODEC);

    public Terraplusminus(IEventBus modEventBus, ModContainer modContainer) {
        instance = this;
        
        LOGGER.info("\n╭━━━━╮\n" +
                "┃╭╮╭╮┃\n" +
                "╰╯┃┃┣┻━┳━┳━┳━━╮╭╮\n" +
                "╱╱┃┃┃┃━┫╭┫╭┫╭╮┣╯╰┳━━╮\n" +
                "╱╱┃┃┃┃━┫┃┃┃┃╭╮┣╮╭┻━━╯\n" +
                "╱╱╰╯╰━━┻╯╰╯╰╯╰╯╰╯\n" +
                "Version: " + modContainer.getModInfo().getVersion());

        // Register the config
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // Register mod event listeners
        modEventBus.addListener(this::commonSetup);
        
        // Register world type (commenting out for now as it needs rework)
        // EarthWorldType.WORLD_TYPES.register(modEventBus);
        
        // Register our chunk generator and biome source
        CHUNK_GENERATORS.register(modEventBus);
        BIOME_SOURCES.register(modEventBus);
        
        // Register game event listeners
        NeoForge.EVENT_BUS.register(new PlayerEventHandler());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Copy osm.json5 into terraplusplus/config/
            try {
                Path terraPlusPlusDir = FMLPaths.GAMEDIR.get().resolve("terraplusplus");
                Path configDir = terraPlusPlusDir.resolve("config");
                
                Files.createDirectories(configDir);
                
                Path osmJsonFile = configDir.resolve("osm.json5");
                if (!Files.exists(osmJsonFile)) {
                    copyResourceFile("/assets/terraplusminus/data/osm.json5", osmJsonFile);
                }
            } catch (IOException e) {
                LOGGER.error("Failed to create terraplusplus config directory", e);
            }
        });
        
        LOGGER.info("[T+-] Terraplusminus successfully initialized");
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("[T+-] Server starting with Terraplusminus");
    }

    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event) {
        TpllCommand.register(event.getDispatcher());
        WhereCommand.register(event.getDispatcher());
        OffsetCommand.register(event.getDispatcher());
    }

    private void copyResourceFile(String resourcePath, Path destination) throws IOException {
        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
            if (in == null) {
                LOGGER.error("Resource not found: " + resourcePath);
                return;
            }
            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("Copied resource {} to {}", resourcePath, destination);
        }
    }
}