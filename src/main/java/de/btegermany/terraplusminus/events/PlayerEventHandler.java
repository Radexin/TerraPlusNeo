package de.btegermany.terraplusminus.events;

import de.btegermany.terraplusminus.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.minecraft.server.MinecraftServer;

public class PlayerEventHandler {

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (!Config.heightInActionbar) {
            return;
        }
        
        MinecraftServer server = event.getServer();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            // Only update every 20 ticks (1 second) to reduce overhead
            if (player.tickCount % 20 == 0) {
                BlockPos pos = player.blockPosition();
                int height = pos.getY();
                int offsetHeight = height - Config.terrainOffsetY;
                
                String message = Config.prefix + "§7Height: §f" + offsetHeight + "m";
                player.displayClientMessage(Component.literal(message), true);
            }
        }
    }
} 