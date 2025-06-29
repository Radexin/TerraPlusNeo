package de.btegermany.terraplusminus.utils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

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
}
