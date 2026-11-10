package com.extendedclip.deluxemenus.cooldown;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EphemeralCooldownManager {
    private static final Map<UUID, Map<String, Long>> COOLDOWNS = new ConcurrentHashMap<>();

    public static void setCooldown(UUID uuid, String id, long durationSeconds) {
        if (durationSeconds <= 0) {
            Map<String, Long> playerCooldowns = COOLDOWNS.get(uuid);
            if (playerCooldowns != null) {
                playerCooldowns.remove(id.toLowerCase());
            }
            return;
        }

        long expiry = System.currentTimeMillis() + (durationSeconds * 1000L);
        COOLDOWNS.computeIfAbsent(uuid, u -> new ConcurrentHashMap<>()).put(id.toLowerCase(), expiry);
    }

    public static boolean isOnCooldown(UUID uuid, String id) {
        Map<String, Long> playerCooldowns = COOLDOWNS.get(uuid);
        if (playerCooldowns == null) return false;
        Long expiry = playerCooldowns.get(id.toLowerCase());
        if (expiry == null) return false;
        if (System.currentTimeMillis() >= expiry) {
            playerCooldowns.remove(id.toLowerCase());
            return false;
        }
        return true;
    }

    public static long getRemainingSeconds(UUID uuid, String id) {
        Map<String, Long> playerCooldowns = COOLDOWNS.get(uuid);
        if (playerCooldowns == null) return 0;
        Long expiry = playerCooldowns.get(id.toLowerCase());
        if (expiry == null) return 0;
        long remaining = (expiry - System.currentTimeMillis()) / 1000L;
        return Math.max(0, remaining);
    }
}
