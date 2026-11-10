package com.extendedclip.deluxemenus.action;

import com.extendedclip.deluxemenus.DeluxeMenusMod;
import com.extendedclip.deluxemenus.cooldown.EphemeralCooldownManager;
import com.extendedclip.deluxemenus.menu.DeluxeMenu;
import com.extendedclip.deluxemenus.menu.DeluxeMenuContainer;
import com.extendedclip.deluxemenus.placeholder.PlaceholderManager;
import com.extendedclip.deluxemenus.utils.ColorUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public class ClickAction {
    private final String rawAction;

    public ClickAction(String rawAction) {
        this.rawAction = rawAction != null ? rawAction.trim() : "";
    }

    public static ClickAction parse(String raw) {
        return new ClickAction(raw);
    }

    public void execute(ServerPlayer player) {
        if (player == null || rawAction.isEmpty()) return;

        MinecraftServer server = player.getServer();
        if (server == null) return;

        String parsed = PlaceholderManager.setPlaceholders(player, rawAction);
        String lower = parsed.toLowerCase();

        if (lower.startsWith("[console]")) {
            String cmd = parsed.substring(9).trim();
            if (cmd.startsWith("/")) cmd = cmd.substring(1);
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), cmd);
        } else if (lower.startsWith("[player]")) {
            String cmd = parsed.substring(8).trim();
            if (cmd.startsWith("/")) cmd = cmd.substring(1);
            server.getCommands().performPrefixedCommand(player.createCommandSourceStack(), cmd);
        } else if (lower.startsWith("[message]")) {
            String msg = parsed.substring(9).trim();
            player.sendSystemMessage(ColorUtils.format(msg));
        } else if (lower.startsWith("[actionbar]")) {
            String msg = parsed.substring(11).trim();
            player.displayClientMessage(ColorUtils.format(msg), true);
        } else if (lower.startsWith("[broadcast]")) {
            String msg = parsed.substring(11).trim();
            Component component = ColorUtils.format(msg);
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                p.sendSystemMessage(component);
            }
        } else if (lower.startsWith("[close]")) {
            player.closeContainer();
        } else if (lower.startsWith("[refresh]")) {
            if (player.containerMenu instanceof DeluxeMenuContainer container) {
                container.refreshMenu();
            }
        } else if (lower.startsWith("[openguimenu]") || lower.startsWith("[openmenu]")) {
            int prefixLen = lower.startsWith("[openguimenu]") ? 13 : 10;
            String menuName = parsed.substring(prefixLen).trim();
            DeluxeMenu nextMenu = DeluxeMenusMod.getMenu(menuName);
            if (nextMenu != null) {
                server.execute(() -> nextMenu.open(player));
            } else {
                player.sendSystemMessage(Component.literal("§cMenu '" + menuName + "' not found!"));
            }
        } else if (lower.startsWith("[sound]")) {
            String[] parts = parsed.substring(7).trim().split("\\s+");
            if (parts.length > 0) {
                String soundName = parts[0].toLowerCase();
                float volume = parts.length > 1 ? parseFloat(parts[1], 1.0f) : 1.0f;
                float pitch = parts.length > 2 ? parseFloat(parts[2], 1.0f) : 1.0f;

                ResourceLocation soundLoc = soundName.contains(":") ? ResourceLocation.parse(soundName) : ResourceLocation.withDefaultNamespace(soundName);
                if (BuiltInRegistries.SOUND_EVENT.containsKey(soundLoc)) {
                    Holder<SoundEvent> sound = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(BuiltInRegistries.SOUND_EVENT.get(soundLoc));
                    player.playNotifySound(sound.value(), SoundSource.MASTER, volume, pitch);
                }
            }
        } else if (lower.startsWith("[ephemeralcooldown]")) {
            String[] parts = parsed.substring(19).trim().split("\\s+");
            if (parts.length >= 2) {
                String id = parts[0];
                long duration = parseDuration(parts[1]);
                EphemeralCooldownManager.setCooldown(player.getUUID(), id, duration);
            }
        } else if (lower.startsWith("[giveexp]")) {
            String val = parsed.substring(9).trim().replace("L", "").replace("l", "");
            try {
                int lv = Integer.parseInt(val);
                player.giveExperienceLevels(lv);
            } catch (Exception ignored) {}
        } else if (lower.startsWith("[takeexp]")) {
            String val = parsed.substring(9).trim().replace("L", "").replace("l", "");
            try {
                int lv = Integer.parseInt(val);
                player.giveExperienceLevels(-lv);
            } catch (Exception ignored) {}
        } else if (lower.startsWith("[meta]")) {
            String[] parts = parsed.substring(6).trim().split("\\s+", 3);
            if (parts.length >= 2) {
                String action = parts[0].toLowerCase();
                String key = parts[1];
                if (action.equals("set") && parts.length >= 3) {
                    PlaceholderManager.setPlayerMeta(player.getUUID(), key, parts[2]);
                } else if (action.equals("remove")) {
                    PlaceholderManager.removePlayerMeta(player.getUUID(), key);
                }
            }
        } else {
            // Default command fallback
            String cmd = parsed;
            if (cmd.startsWith("/")) cmd = cmd.substring(1);
            server.getCommands().performPrefixedCommand(player.createCommandSourceStack(), cmd);
        }
    }

    private float parseFloat(String s, float def) {
        try {
            return Float.parseFloat(s);
        } catch (Exception e) {
            return def;
        }
    }

    private long parseDuration(String s) {
        s = s.trim().toLowerCase();
        try {
            if (s.endsWith("s")) return Long.parseLong(s.substring(0, s.length() - 1));
            if (s.endsWith("m")) return Long.parseLong(s.substring(0, s.length() - 1)) * 60L;
            if (s.endsWith("h")) return Long.parseLong(s.substring(0, s.length() - 1)) * 3600L;
            if (s.endsWith("d")) return Long.parseLong(s.substring(0, s.length() - 1)) * 86400L;
            return Long.parseLong(s);
        } catch (Exception e) {
            return 0L;
        }
    }
}
