package de.btegermany.terraplusminus.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import de.btegermany.terraplusminus.Config;
import de.btegermany.terraplusminus.terraplusminus;
import de.btegermany.terraplusminus.utils.ConfigurationHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = terraplusminus.MODID, bus = EventBusSubscriber.Bus.GAME)
public class OffsetCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("offset")
            .requires(source -> source.hasPermission(0)) // Allow all players
            .executes(OffsetCommand::execute));
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (source.getEntity() instanceof ServerPlayer player) {
            // Display offsets
            player.sendSystemMessage(Component.literal(Config.prefix + "§7Offsets:"));
            player.sendSystemMessage(Component.literal(Config.prefix + "§7 | X: §8" + Config.terrainOffsetX));

            // Handle linked worlds logic
            if (!Config.linkedWorldsMethod.equals(Config.LinkedWorldMethod.MULTIVERSE) || !Config.linkedWorldsEnabled) {
                player.sendSystemMessage(Component.literal(Config.prefix + "§7 | Y: §8" + Config.terrainOffsetY));
            } else {
                if (Config.linkedWorldsEnabled && Config.linkedWorldsMethod.equals(Config.LinkedWorldMethod.MULTIVERSE)) {
                    ConfigurationHelper.getWorlds().forEach(world ->
                        player.sendSystemMessage(Component.literal(Config.prefix + "§9 " + world.getWorldName() + "§7 | Y: §8" + world.getOffset())));
                }
            }

            player.sendSystemMessage(Component.literal(Config.prefix + "§7 | Z: §8" + Config.terrainOffsetZ));
            return 1; // Command success
        } else {
            source.sendSystemMessage(Component.literal("This command can only be used by players!"));
            return 0;
        }
    }
}
