package de.btegermany.terraplusminus.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import de.btegermany.terraplusminus.Config;
import de.btegermany.terraplusminus.data.TerraConnector;
import de.btegermany.terraplusminus.gen.RealWorldGenerator;
import net.buildtheearth.terraminusminus.generator.EarthGeneratorSettings;
import net.buildtheearth.terraminusminus.projection.GeographicProjection;
import net.buildtheearth.terraminusminus.projection.OutOfProjectionBoundsException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.Collections;

public class TpllCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> tpllCommand = Commands.literal("tpll")
                .requires(source -> source.hasPermission(2) || source.getServer().isSingleplayer())
                .then(Commands.argument("latitude", DoubleArgumentType.doubleArg(-90, 90))
                        .then(Commands.argument("longitude", DoubleArgumentType.doubleArg(-180, 180))
                                .executes(context -> execute(context, Collections.singleton(context.getSource().getPlayerOrException()), 
                                        DoubleArgumentType.getDouble(context, "latitude"),
                                        DoubleArgumentType.getDouble(context, "longitude"), 
                                        null))
                                .then(Commands.argument("height", DoubleArgumentType.doubleArg())
                                        .executes(context -> execute(context, Collections.singleton(context.getSource().getPlayerOrException()),
                                                DoubleArgumentType.getDouble(context, "latitude"),
                                                DoubleArgumentType.getDouble(context, "longitude"),
                                                DoubleArgumentType.getDouble(context, "height"))))))
                .then(Commands.argument("targets", EntityArgument.players())
                        .requires(source -> source.hasPermission(3))
                        .then(Commands.argument("latitude", DoubleArgumentType.doubleArg(-90, 90))
                                .then(Commands.argument("longitude", DoubleArgumentType.doubleArg(-180, 180))
                                        .executes(context -> execute(context, EntityArgument.getPlayers(context, "targets"),
                                                DoubleArgumentType.getDouble(context, "latitude"),
                                                DoubleArgumentType.getDouble(context, "longitude"),
                                                null))
                                        .then(Commands.argument("height", DoubleArgumentType.doubleArg())
                                                .executes(context -> execute(context, EntityArgument.getPlayers(context, "targets"),
                                                        DoubleArgumentType.getDouble(context, "latitude"),
                                                        DoubleArgumentType.getDouble(context, "longitude"),
                                                        DoubleArgumentType.getDouble(context, "height")))))));

        dispatcher.register(tpllCommand);
        
        // Register alias
        dispatcher.register(Commands.literal("tpc").redirect(dispatcher.getRoot().getChild("tpll")));
    }

    private static int execute(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets, double latitude, double longitude, Double height) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        
        // Check bounds if configured
        double minLat = Config.minLatitude;
        double maxLat = Config.maxLatitude;
        double minLon = Config.minLongitude;
        double maxLon = Config.maxLongitude;
        
        if (minLat != 0 && maxLat != 0 && minLon != 0 && maxLon != 0 && !source.hasPermission(4)) {
            if (latitude < minLat || latitude > maxLat || longitude < minLon || longitude > maxLon) {
                source.sendFailure(Component.literal(Config.prefix + "§cYou cannot tpll to these coordinates, because this area is being worked on by another build team."));
                return 0;
            }
        }
        
        // Get world generator
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        if (!(generator instanceof RealWorldGenerator realWorldGen)) {
            source.sendFailure(Component.literal(Config.prefix + "§cThe world generator must be set to Terraplusminus"));
            return 0;
        }
        
        EarthGeneratorSettings generatorSettings = realWorldGen.settings;
        GeographicProjection projection = generatorSettings.projection();
        int yOffset = realWorldGen.yOffset;
        
        // Convert coordinates
        double[] mcCoordinates;
        try {
            mcCoordinates = projection.fromGeo(longitude, latitude);
        } catch (OutOfProjectionBoundsException e) {
            source.sendFailure(Component.literal("§cLocation is not within projection bounds"));
            return 0;
        }
        
        // Apply terrain offset
        double x = mcCoordinates[0] + Config.terrainOffsetX;
        double z = mcCoordinates[1] + Config.terrainOffsetZ;
        
        // Determine height
        double finalHeight;
        if (height != null) {
            finalHeight = height + yOffset;
        } else {
            TerraConnector terraConnector = new TerraConnector();
            finalHeight = terraConnector.getHeight((int) mcCoordinates[0], (int) mcCoordinates[1]).join() + yOffset;
        }
        
        // Check height bounds
        if (finalHeight > level.getMaxBuildHeight()) {
            source.sendFailure(Component.literal(Config.prefix + "§cYou cannot tpll to these coordinates, because the world is not high enough."));
            return 0;
        } else if (finalHeight < level.getMinBuildHeight()) {
            source.sendFailure(Component.literal(Config.prefix + "§cYou cannot tpll to these coordinates, because the world is not low enough."));
            return 0;
        }
        
        // Teleport all targets
        for (ServerPlayer player : targets) {
            // Check if chunk is loaded, if not, generate it
            BlockPos blockPos = BlockPos.containing(x, finalHeight, z);
            level.getChunk(blockPos);
            
            // If no specific height was given, find the highest block
            if (height == null) {
                int highestY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, (int) x, (int) z);
                finalHeight = highestY + 1;
            }
            
            player.teleportTo(level, x, finalHeight, z, player.getYRot(), player.getXRot());
            
            if (targets.size() == 1 && player == source.getPlayer()) {
                final double displayHeight = finalHeight;
                source.sendSuccess(() -> Component.literal(Config.prefix + "§7Teleported to " + latitude + ", " + longitude + 
                        (height != null ? ", " + displayHeight : "") + "."), false);
            }
        }
        
        if (targets.size() > 1 || (targets.size() == 1 && !targets.contains(source.getPlayer()))) {
            String playerNames = targets.stream()
                    .map(p -> p.getName().getString())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            source.sendSuccess(() -> Component.literal(Config.prefix + "§7Teleported §9" + playerNames + " §7to " + 
                    latitude + ", " + longitude + "."), true);
        }
        
        return targets.size();
    }
}
