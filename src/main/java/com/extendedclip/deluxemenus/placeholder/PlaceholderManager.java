package com.extendedclip.deluxemenus.placeholder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class PlaceholderManager {
    private static final Map<UUID, Map<String, Object>> PLAYER_META = new ConcurrentHashMap<>();
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    public static String setPlaceholders(ServerPlayer player, String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        if (player == null) {
            return text;
        }

        MinecraftServer server = player.getServer();

        // Player placeholders
        text = text.replace("%player_name%", player.getScoreboardName());
        text = text.replace("%player_displayname%", player.getDisplayName().getString());
        text = text.replace("%player_uuid%", player.getStringUUID());
        text = text.replace("%player_level%", String.valueOf(player.experienceLevel));
        text = text.replace("%player_exp%", String.valueOf(player.totalExperience));
        text = text.replace("%player_health%", String.format("%.1f", player.getHealth()));
        text = text.replace("%player_max_health%", String.format("%.1f", player.getMaxHealth()));
        text = text.replace("%player_food%", String.valueOf(player.getFoodData().getFoodLevel()));
        text = text.replace("%player_gamemode%", player.gameMode.getGameModeForPlayer().getName());
        text = text.replace("%player_ping%", String.valueOf(player.connection.latency()));
        text = text.replace("%player_world%", player.level().dimension().location().getPath());
        text = text.replace("%player_x%", String.format("%.1f", player.getX()));
        text = text.replace("%player_y%", String.format("%.1f", player.getY()));
        text = text.replace("%player_z%", String.format("%.1f", player.getZ()));

        // Server placeholders
        if (server != null) {
            text = text.replace("%server_online%", String.valueOf(server.getPlayerCount()));
            text = text.replace("%server_max_players%", String.valueOf(server.getMaxPlayers()));
            text = text.replace("%server_name%", server.getServerModName());
        }
        text = text.replace("%server_time%", TIME_FORMAT.format(new Date()));

        // Meta placeholders: %meta_<key>%
        if (text.contains("%meta_")) {
            Map<String, Object> meta = PLAYER_META.get(player.getUUID());
            if (meta != null) {
                for (Map.Entry<String, Object> entry : meta.entrySet()) {
                    text = text.replace("%meta_" + entry.getKey() + "%", String.valueOf(entry.getValue()));
                }
            }
        }

        return text;
    }

    public static void setPlayerMeta(UUID uuid, String key, Object value) {
        PLAYER_META.computeIfAbsent(uuid, u -> new ConcurrentHashMap<>()).put(key, value);
    }

    public static Object getPlayerMeta(UUID uuid, String key) {
        Map<String, Object> meta = PLAYER_META.get(uuid);
        return meta != null ? meta.get(key) : null;
    }

    public static void removePlayerMeta(UUID uuid, String key) {
        Map<String, Object> meta = PLAYER_META.get(uuid);
        if (meta != null) {
            meta.remove(key);
        }
    }
}
