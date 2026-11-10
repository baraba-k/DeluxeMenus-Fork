package com.extendedclip.deluxemenus.requirement;

import com.extendedclip.deluxemenus.action.ClickAction;
import com.extendedclip.deluxemenus.cooldown.EphemeralCooldownManager;
import com.extendedclip.deluxemenus.placeholder.PlaceholderManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class Requirement {
    private final String type;
    private final Map<String, Object> options;
    private final List<ClickAction> denyActions = new ArrayList<>();

    public Requirement(String type, Map<String, Object> options) {
        this.type = type != null ? type.toLowerCase() : "";
        this.options = options != null ? options : Map.of();

        Object denyCommands = this.options.get("deny_commands");
        if (denyCommands instanceof List<?> list) {
            for (Object obj : list) {
                if (obj != null) {
                    denyActions.add(ClickAction.parse(String.valueOf(obj)));
                }
            }
        } else if (denyCommands instanceof String str) {
            denyActions.add(ClickAction.parse(str));
        }
    }

    public List<ClickAction> getDenyActions() {
        return denyActions;
    }

    public boolean evaluate(ServerPlayer player) {
        if (player == null) return false;

        String input = parseString("input", player);
        String output = parseString("output", player);

        switch (type) {
            case "has permission":
            case "has perm":
            case "perm":
            case "haspermission":
                String perm = parseString("permission", player);
                if (perm.isEmpty()) return true;
                // In vanilla/NeoForge: check if player is OP or permission level
                return player.hasPermissions(2) || player.getServer().isSingleplayer();

            case "!has permission":
            case "!has perm":
            case "!perm":
            case "!haspermission":
                return !player.hasPermissions(2) && !player.getServer().isSingleplayer();

            case "has exp":
            case "hasexp":
            case "exp":
                int reqExp = parseInt("amount", 0);
                boolean levelBased = parseBoolean("level", false);
                return levelBased ? player.experienceLevel >= reqExp : player.totalExperience >= reqExp;

            case "!has exp":
            case "!hasexp":
            case "!exp":
                int noExp = parseInt("amount", 0);
                boolean noExpLevel = parseBoolean("level", false);
                return noExpLevel ? player.experienceLevel < noExp : player.totalExperience < noExp;

            case "has item":
            case "hasitem":
            case "item":
                return checkHasItem(player, true);

            case "!has item":
            case "!hasitem":
            case "does not have item":
                return !checkHasItem(player, true);

            case "has ephemeral cooldown":
            case "hasephemeralcooldown":
            case "ephemeral cooldown":
            case "ephemeralcooldown":
                String cdId = parseString("id", player);
                return EphemeralCooldownManager.isOnCooldown(player.getUUID(), cdId);

            case "!has ephemeral cooldown":
            case "!hasephemeralcooldown":
            case "!ephemeral cooldown":
            case "!ephemeralcooldown":
            case "does not have ephemeral cooldown":
                String noCdId = parseString("id", player);
                return !EphemeralCooldownManager.isOnCooldown(player.getUUID(), noCdId);

            case "string equals":
            case "stringequals":
            case "equals":
                return input.equals(output);

            case "!string equals":
            case "!stringequals":
            case "!equals":
                return !input.equals(output);

            case "string equals ignorecase":
            case "stringequalsignorecase":
            case "equalsignorecase":
                return input.equalsIgnoreCase(output);

            case "!string equals ignorecase":
            case "!stringequalsignorecase":
            case "!equalsignorecase":
                return !input.equalsIgnoreCase(output);

            case "string contains":
            case "stringcontains":
            case "contains":
                return input.contains(output);

            case "!string contains":
            case "!stringcontains":
            case "!contains":
                return !input.contains(output);

            case "==":
            case "equal to":
            case "equalto":
                return compareNumbersOrStrings(input, output) == 0;

            case "!=":
            case "not equal to":
            case "notequalto":
                return compareNumbersOrStrings(input, output) != 0;

            case ">":
            case "greater than":
            case "greaterthan":
                return compareNumbers(input, output) > 0;

            case ">=":
            case "greater than or equal to":
            case "greaterthanorequalto":
                return compareNumbers(input, output) >= 0;

            case "<":
            case "less than":
            case "lessthan":
                return compareNumbers(input, output) < 0;

            case "<=":
            case "less than or equal to":
            case "lessthanorequalto":
                return compareNumbers(input, output) <= 0;

            case "regex matches":
            case "regex":
                String regex = parseString("regex", player);
                try {
                    return Pattern.compile(regex).matcher(input).find();
                } catch (Exception e) {
                    return false;
                }

            case "!regex matches":
            case "!regex":
                String notRegex = parseString("regex", player);
                try {
                    return !Pattern.compile(notRegex).matcher(input).find();
                } catch (Exception e) {
                    return false;
                }

            case "string length":
                int min = parseInt("min", 0);
                int max = parseInt("max", Integer.MAX_VALUE);
                int len = input.length();
                return len >= min && len <= max;

            case "javascript":
            case "js":
                String expr = parseString("expression", player);
                return evaluateSimpleExpression(expr);

            default:
                return true;
        }
    }

    private boolean checkHasItem(ServerPlayer player, boolean exactAmount) {
        String materialName = parseString("material", player).toLowerCase();
        int requiredAmount = parseInt("amount", 1);
        String requiredName = parseString("name", player);

        Item targetItem = Items.AIR;
        if (materialName.contains(":")) {
            targetItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(materialName));
        } else {
            targetItem = BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(materialName));
        }

        if (targetItem == Items.AIR && !materialName.equals("air")) {
            return false;
        }

        int foundAmount = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(targetItem)) {
                if (!requiredName.isEmpty() && !stack.getHoverName().getString().equals(requiredName)) {
                    continue;
                }
                foundAmount += stack.getCount();
            }
        }
        return foundAmount >= requiredAmount;
    }

    private int compareNumbers(String a, String b) {
        try {
            double da = Double.parseDouble(a.trim());
            double db = Double.parseDouble(b.trim());
            return Double.compare(da, db);
        } catch (Exception e) {
            return a.compareTo(b);
        }
    }

    private int compareNumbersOrStrings(String a, String b) {
        try {
            double da = Double.parseDouble(a.trim());
            double db = Double.parseDouble(b.trim());
            return Double.compare(da, db);
        } catch (Exception e) {
            return a.equals(b) ? 0 : 1;
        }
    }

    private boolean evaluateSimpleExpression(String expr) {
        if (expr == null || expr.isEmpty()) return false;
        expr = expr.trim();
        if (expr.equalsIgnoreCase("true")) return true;
        if (expr.equalsIgnoreCase("false")) return false;

        if (expr.contains("==")) {
            String[] parts = expr.split("==", 2);
            return compareNumbersOrStrings(parts[0].trim(), parts[1].trim()) == 0;
        } else if (expr.contains("!=")) {
            String[] parts = expr.split("!=", 2);
            return compareNumbersOrStrings(parts[0].trim(), parts[1].trim()) != 0;
        } else if (expr.contains(">=")) {
            String[] parts = expr.split(">=", 2);
            return compareNumbers(parts[0].trim(), parts[1].trim()) >= 0;
        } else if (expr.contains("<=")) {
            String[] parts = expr.split("<=", 2);
            return compareNumbers(parts[0].trim(), parts[1].trim()) <= 0;
        } else if (expr.contains(">")) {
            String[] parts = expr.split(">", 2);
            return compareNumbers(parts[0].trim(), parts[1].trim()) > 0;
        } else if (expr.contains("<")) {
            String[] parts = expr.split("<", 2);
            return compareNumbers(parts[0].trim(), parts[1].trim()) < 0;
        }
        return false;
    }

    private String parseString(String key, ServerPlayer player) {
        Object val = options.get(key);
        if (val == null) return "";
        return PlaceholderManager.setPlaceholders(player, String.valueOf(val));
    }

    private int parseInt(String key, int defaultValue) {
        Object val = options.get(key);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(String.valueOf(val).trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private boolean parseBoolean(String key, boolean defaultValue) {
        Object val = options.get(key);
        if (val == null) return defaultValue;
        return Boolean.parseBoolean(String.valueOf(val).trim());
    }
}
