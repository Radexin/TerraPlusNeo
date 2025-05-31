package de.btegermany.terraplusminus.utils;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;

public class PlayerHashMapManagement {

    HashMap<ServerPlayer, String> players;

    public PlayerHashMapManagement() {
        players = new HashMap<>();
    }

    public void addPlayer(ServerPlayer player, String coordinates) {
        players.put(player, coordinates);
    }

    public void removePlayer(ServerPlayer player) {
        players.remove(player);
    }

    public boolean containsPlayer(ServerPlayer player) {
        return players.containsKey(player);
    }

    public String getCoordinates(ServerPlayer player) {
        return players.get(player);
    }

}
