package de.btegermany.terraplusminus.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import de.btegermany.terraplusminus.Config;
import de.btegermany.terraplusminus.Terraplusminus;
import net.buildtheearth.terraminusminus.generator.EarthGeneratorSettings;
import net.buildtheearth.terraminusminus.projection.OutOfProjectionBoundsException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

public class WhereCommand {

    private static final EarthGeneratorSettings bteGeneratorSettings = EarthGeneratorSettings.parse(EarthGeneratorSettings.BTE_DEFAULT_SETTINGS);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("where")
            .requires(source -> source.hasPermission(0)) // Allow all players
            .executes(WhereCommand::execute));
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer)) {
            source.sendSystemMessage(Component.literal("This command can only be used by players!"));
            return 0;
        }

        ServerPlayer player = (ServerPlayer) source.getEntity();
        int xOffset = Config.terrainOffsetX;
        int zOffset = Config.terrainOffsetZ;

        double[] mcCoordinates = new double[2];
        mcCoordinates[0] = player.getX() - xOffset;
        mcCoordinates[1] = player.getZ() - zOffset;

        final double[] coordinates;
        try {
            coordinates = bteGeneratorSettings.projection().toGeo(mcCoordinates[0], mcCoordinates[1]);
        } catch (OutOfProjectionBoundsException e) {
            player.sendSystemMessage(Component.literal(Config.prefix + "§cLocation is not within projection bounds"));
            return 0;
        }

        // Create clickable message component
        MutableComponent message = Component.literal(Config.prefix + "§7Your coordinates are:\n§8" + coordinates[1] + ", " + coordinates[0] + "§7.")
            .withStyle(style -> style
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://maps.google.com/maps?t=k&q=loc:" + coordinates[1] + "+" + coordinates[0]))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("§7Click here to view in Google Maps."))));

        player.sendSystemMessage(message);
        return 1;
    }
}
