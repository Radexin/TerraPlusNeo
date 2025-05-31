package de.btegermany.terraplusminus.utils;

import de.btegermany.terraplusminus.Config;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ConfigurationHelper {

    /**
     * Returns a block from the configuration,
     * or a default value if the configuration path is either missing or the value is not a valid block identifier.
     *
     * @param blockName     the block name to parse
     * @param defaultValue  a default value to return if the value is missing from the config or invalid
     * @return a {@link Block} from the configuration, or {@code defaultValue} as a fallback
     */
    public static Block getBlock(@NotNull String blockName, Block defaultValue) {
        try {
            ResourceLocation resourceLocation = ResourceLocation.parse(blockName);
            Block block = BuiltInRegistries.BLOCK.get(resourceLocation);
            return block != null ? block : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private ConfigurationHelper() {
        throw new IllegalStateException();
    }

    public static List<LinkedWorld> convertList(List<Map<?, ?>> originalList) {
        return originalList.stream()
                .map(ConfigurationHelper::convertMapToLinkedWorld)
                .filter(world -> !world.getWorldName().equalsIgnoreCase("another_world/server") || !world.getWorldName().equalsIgnoreCase("current_world/server"))
                .collect(Collectors.toList());
    }

    private static LinkedWorld convertMapToLinkedWorld(Map<?, ?> originalMap) {
        String worldName = originalMap.get("name").toString();
        int offset = (Integer) originalMap.get("offset");
        return new LinkedWorld(worldName, offset);
    }

    public static LinkedWorld getNextServerName(String currentWorldName) {
        List<LinkedWorld> worlds = Config.linkedWorlds;
        int currentIndex = -1;

        for (int i = 0; i < worlds.size(); i++) {
            LinkedWorld world = worlds.get(i);
            if (world.getWorldName().equalsIgnoreCase(currentWorldName)) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex >= 0 && currentIndex < worlds.size() - 1) {
            return worlds.get(currentIndex + 1);
        } else {
            // Entweder wurde die Welt nicht gefunden oder sie ist die letzte Welt in der Liste
            return null;
        }
    }

    public static LinkedWorld getPreviousServerName(String currentWorldName) {
        List<LinkedWorld> worlds = Config.linkedWorlds;
        int currentIndex = -1;

        for (int i = 0; i < worlds.size(); i++) {
            LinkedWorld world = worlds.get(i);
            if (world.getWorldName().equalsIgnoreCase(currentWorldName)) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex > 0) {
            return worlds.get(currentIndex - 1);
        } else {
            // Entweder wurde die Welt nicht gefunden oder sie ist die erste Welt in der Liste
            return null;
        }
    }

    public static List<LinkedWorld> getWorlds() {
        return Config.linkedWorlds;
    }

}
