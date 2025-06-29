package de.btegermany.terraplusminus.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import de.btegermany.terraplusminus.Config;
import de.btegermany.terraplusminus.Terraplusminus;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class OffsetCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
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
            player.sendSystemMessage(Component.literal(Config.prefix + "§7 | Y: §8" + Config.terrainOffsetY));
            player.sendSystemMessage(Component.literal(Config.prefix + "§7 | Z: §8" + Config.terrainOffsetZ));
            return 1; // Command success
        } else {
            source.sendSystemMessage(Component.literal("This command can only be used by players!"));
            return 0;
        }
    }
}
