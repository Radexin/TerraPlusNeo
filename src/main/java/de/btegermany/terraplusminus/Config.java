package de.btegermany.terraplusminus;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.btegermany.terraplusminus.utils.LinkedWorld;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

// TerraPlusNeo configuration class - converted from Paper plugin config.yml
@EventBusSubscriber(modid = terraplusminus.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // Basic settings
    private static final ModConfigSpec.ConfigValue<String> PREFIX = BUILDER
            .comment("The prefix is written in front of every message that is sent to the chat by the plugin.")
            .define("prefix", "§2§lT+- §8» ");

    private static final ModConfigSpec.BooleanValue REDUCED_CONSOLE_MESSAGES = BUILDER
            .comment("If disabled, the plugin will log every fetched data to the console")
            .define("reduced_console_messages", true);

    private static final ModConfigSpec.BooleanValue HEIGHT_DATAPACK = BUILDER
            .comment("If this option is enabled, the plugin will copy a datapack with the name 'world-height-datapack.zip' to the world directory, which expands the world to the maximum possibly with a datapack 2032.")
            .define("height_datapack", false);

    private static final ModConfigSpec.BooleanValue HEIGHT_IN_ACTIONBAR = BUILDER
            .comment("If enabled, it will show the height of the player in the actionbar.")
            .define("height_in_actionbar", false);

    // TPLL bounds - using static block to handle push/pop
    static {
        BUILDER.comment("Set bounds so that players can only tpll within these limits. They will get a message that the area is being worked on by another build team.",
                       "The option is turned off when all values are 0.0")
               .push("tpll_bounds");
    }

    private static final ModConfigSpec.DoubleValue MIN_LATITUDE = BUILDER
            .comment("Minimum latitude for tpll command")
            .defineInRange("min_latitude", 0.0, -90.0, 90.0);

    private static final ModConfigSpec.DoubleValue MAX_LATITUDE = BUILDER
            .comment("Maximum latitude for tpll command")
            .defineInRange("max_latitude", 0.0, -90.0, 90.0);

    private static final ModConfigSpec.DoubleValue MIN_LONGITUDE = BUILDER
            .comment("Minimum longitude for tpll command")
            .defineInRange("min_longitude", 0.0, -180.0, 180.0);

    private static final ModConfigSpec.DoubleValue MAX_LONGITUDE = BUILDER
            .comment("Maximum longitude for tpll command")
            .defineInRange("max_longitude", 0.0, -180.0, 180.0);

    static {
        BUILDER.pop();
    }

    private static final ModConfigSpec.ConfigValue<String> PASSTHROUGH_TPLL = BUILDER
            .comment("Passthrough tpll to other bukkit plugins. It will not passthrough when it's empty. Type in the name of your plugin. E.g. Your plugin name is vanillatpll you set passthrough_tpll: 'vanillatpll'")
            .define("passthrough_tpll", "");

    // Terrain offset
    static {
        BUILDER.comment("Offset your section which fits into the world.")
               .push("terrain_offset");
    }

    private static final ModConfigSpec.IntValue TERRAIN_OFFSET_X = BUILDER
            .comment("X offset for terrain generation")
            .defineInRange("x", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue TERRAIN_OFFSET_Y = (ModConfigSpec.IntValue) BUILDER
            .comment("Y offset for terrain generation")
            .defineInRange("y", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue TERRAIN_OFFSET_Z = BUILDER
            .comment("Z offset for terrain generation")
            .defineInRange("z", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);

    static {
        BUILDER.pop();
    }

    // Linked worlds
    static {
        BUILDER.comment("If the height limit in this world/server is not enough, other worlds/servers can be linked to generate higher or lower sections")
               .push("linked_worlds");
    }

    private static final ModConfigSpec.BooleanValue LINKED_WORLDS_ENABLED = BUILDER
            .comment("Enable linked worlds functionality")
            .define("enabled", false);

    private static final ModConfigSpec.EnumValue<LinkedWorldMethod> LINKED_WORLDS_METHOD = BUILDER
            .comment("Method for linked worlds: SERVER or MULTIVERSE")
            .defineEnum("method", LinkedWorldMethod.MULTIVERSE);

    private static final ModConfigSpec.ConfigValue<List<? extends String>> LINKED_WORLDS_LIST = BUILDER
            .comment("List of linked worlds in format 'worldname:offset'. Example: 'world_upper:2032'")
            .defineListAllowEmpty("worlds", List.of("current_world:0"), Config::validateLinkedWorld);

    static {
        BUILDER.pop();
    }

    // Generation settings
    private static final ModConfigSpec.BooleanValue GENERATE_TREES = BUILDER
            .comment("If disabled, tree generation is turned off.")
            .define("generate_trees", true);

    private static final ModConfigSpec.BooleanValue DIFFERENT_BIOMES = BUILDER
            .comment("The biomes will be generated with Köppen climate classification. If turned off, everything will be plains biome.")
            .define("different_biomes", true);

    // Materials
    static {
        BUILDER.comment("Customize the material, the blocks will be generated with.")
               .push("materials");
    }

    private static final ModConfigSpec.ConfigValue<String> SURFACE_MATERIAL = BUILDER
            .comment("Material for surface blocks")
            .define("surface_material", "minecraft:grass_block", Config::validateBlockName);

    private static final ModConfigSpec.ConfigValue<String> BUILDING_OUTLINES_MATERIAL = BUILDER
            .comment("Material for building outlines")
            .define("building_outlines_material", "minecraft:bricks", Config::validateBlockName);

    private static final ModConfigSpec.ConfigValue<String> ROAD_MATERIAL = BUILDER
            .comment("Material for roads")
            .define("road_material", "minecraft:gray_concrete_powder", Config::validateBlockName);

    private static final ModConfigSpec.ConfigValue<String> PATH_MATERIAL = BUILDER
            .comment("Material for paths")
            .define("path_material", "minecraft:moss_block", Config::validateBlockName);

    static {
        BUILDER.pop();
    }

    private static final ModConfigSpec.DoubleValue CONFIG_VERSION = BUILDER
            .comment("Configuration version - do not change")
            .defineInRange("config_version", 1.4, 1.4, 1.4);

    static final ModConfigSpec SPEC = BUILDER.build();

    // Enum for linked world methods
    public enum LinkedWorldMethod {
        SERVER, MULTIVERSE
    }

    // Static config values accessible throughout the mod
    public static String prefix;
    public static boolean reducedConsoleMessages;
    public static boolean heightDatapack;
    public static boolean heightInActionbar;
    public static double minLatitude;
    public static double maxLatitude;
    public static double minLongitude;
    public static double maxLongitude;
    public static String passthroughTpll;
    public static int terrainOffsetX;
    public static int terrainOffsetY;
    public static int terrainOffsetZ;
    public static boolean linkedWorldsEnabled;
    public static LinkedWorldMethod linkedWorldsMethod;
    public static List<LinkedWorld> linkedWorlds;
    public static boolean generateTrees;
    public static boolean differentBiomes;
    public static Block surfaceMaterial;
    public static Block buildingOutlinesMaterial;
    public static Block roadMaterial;
    public static Block pathMaterial;
    public static double configVersion;

    // Validation methods
    private static boolean validateBlockName(final Object obj) {
        if (!(obj instanceof String blockName)) return false;
        try {
            ResourceLocation resourceLocation = ResourceLocation.parse(blockName);
            return BuiltInRegistries.BLOCK.containsKey(resourceLocation);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean validateLinkedWorld(final Object obj) {
        if (!(obj instanceof String worldString)) return false;
        try {
            String[] parts = worldString.split(":");
            if (parts.length != 2) return false;
            Integer.parseInt(parts[1]); // Validate offset is a number
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static Block getBlockFromString(String blockName, Block defaultBlock) {
        try {
            ResourceLocation resourceLocation = ResourceLocation.parse(blockName);
            Block block = BuiltInRegistries.BLOCK.get(resourceLocation);
            return block != null ? block : defaultBlock;
        } catch (Exception e) {
            return defaultBlock;
        }
    }

    private static List<LinkedWorld> parseLinkedWorlds(List<? extends String> worldStrings) {
        return worldStrings.stream()
                .map(worldString -> {
                    try {
                        String[] parts = worldString.split(":");
                        if (parts.length == 2) {
                            String worldName = parts[0];
                            int offset = Integer.parseInt(parts[1]);
                            return new LinkedWorld(worldName, offset);
                        }
                    } catch (Exception e) {
                        // Invalid format, skip
                    }
                    return null;
                })
                .filter(world -> world != null)
                .filter(world -> !world.getWorldName().equalsIgnoreCase("another_world/server"))
                .collect(Collectors.toList());
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        // Load all configuration values
        prefix = PREFIX.get();
        reducedConsoleMessages = REDUCED_CONSOLE_MESSAGES.get();
        heightDatapack = HEIGHT_DATAPACK.get();
        heightInActionbar = HEIGHT_IN_ACTIONBAR.get();
        minLatitude = MIN_LATITUDE.get();
        maxLatitude = MAX_LATITUDE.get();
        minLongitude = MIN_LONGITUDE.get();
        maxLongitude = MAX_LONGITUDE.get();
        passthroughTpll = PASSTHROUGH_TPLL.get();
        terrainOffsetX = TERRAIN_OFFSET_X.get();
        terrainOffsetY = TERRAIN_OFFSET_Y.get();
        terrainOffsetZ = TERRAIN_OFFSET_Z.get();
        linkedWorldsEnabled = LINKED_WORLDS_ENABLED.get();
        linkedWorldsMethod = LINKED_WORLDS_METHOD.get();
        linkedWorlds = parseLinkedWorlds(LINKED_WORLDS_LIST.get());
        generateTrees = GENERATE_TREES.get();
        differentBiomes = DIFFERENT_BIOMES.get();
        surfaceMaterial = getBlockFromString(SURFACE_MATERIAL.get(), Blocks.GRASS_BLOCK);
        buildingOutlinesMaterial = getBlockFromString(BUILDING_OUTLINES_MATERIAL.get(), Blocks.BRICKS);
        roadMaterial = getBlockFromString(ROAD_MATERIAL.get(), Blocks.GRAY_CONCRETE_POWDER);
        pathMaterial = getBlockFromString(PATH_MATERIAL.get(), Blocks.MOSS_BLOCK);
        configVersion = CONFIG_VERSION.get();
    }

    // Helper methods for backward compatibility with existing code
    public static String getString(String path) {
        return switch (path) {
            case "prefix" -> prefix;
            case "passthrough_tpll" -> passthroughTpll;
            case "linked_worlds.method" -> linkedWorldsMethod.name();
            default -> "";
        };
    }

    public static boolean getBoolean(String path) {
        return switch (path) {
            case "reduced_console_messages" -> reducedConsoleMessages;
            case "height_datapack" -> heightDatapack;
            case "height_in_actionbar" -> heightInActionbar;
            case "linked_worlds.enabled" -> linkedWorldsEnabled;
            case "generate_trees" -> generateTrees;
            case "different_biomes" -> differentBiomes;
            default -> false;
        };
    }

    public static int getInt(String path) {
        return switch (path) {
            case "terrain_offset.x" -> terrainOffsetX;
            case "terrain_offset.y" -> terrainOffsetY;
            case "terrain_offset.z" -> terrainOffsetZ;
            case "y_offset" -> terrainOffsetY; // Legacy compatibility
            default -> 0;
        };
    }

    public static double getDouble(String path) {
        return switch (path) {
            case "min_latitude" -> minLatitude;
            case "max_latitude" -> maxLatitude;
            case "min_longitude" -> minLongitude;
            case "max_longitude" -> maxLongitude;
            case "config_version" -> configVersion;
            default -> 0.0;
        };
    }

    public static List<Map<?, ?>> getMapList(String path) {
        if ("linked_worlds.worlds".equals(path)) {
            return linkedWorlds.stream()
                    .map(world -> Map.of("name", world.getWorldName(), "offset", world.getOffset()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
