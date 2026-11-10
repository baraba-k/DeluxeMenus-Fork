package com.extendedclip.deluxemenus.menu;

import com.extendedclip.deluxemenus.action.ClickAction;
import com.extendedclip.deluxemenus.requirement.Requirement;
import com.extendedclip.deluxemenus.utils.ItemBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

public class MenuItem {
    private final String configKey;
    private final List<Integer> slots = new ArrayList<>();
    private final int priority;
    private final String material;
    private final int amount;
    private final String displayName;
    private final List<String> lore = new ArrayList<>();
    private final Integer customModelData;
    private final boolean glow;
    private final boolean unbreakable;
    private final boolean hideAttributes;

    private final List<Requirement> viewRequirements = new ArrayList<>();
    private final List<ClickAction> clickCommands = new ArrayList<>();
    private final List<ClickAction> leftClickCommands = new ArrayList<>();
    private final List<ClickAction> rightClickCommands = new ArrayList<>();
    private final List<ClickAction> shiftLeftClickCommands = new ArrayList<>();
    private final List<ClickAction> shiftRightClickCommands = new ArrayList<>();
    private final List<ClickAction> middleClickCommands = new ArrayList<>();

    private final List<Requirement> clickRequirements = new ArrayList<>();
    private final List<Requirement> leftClickRequirements = new ArrayList<>();
    private final List<Requirement> rightClickRequirements = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public MenuItem(String configKey, Map<String, Object> map) {
        this.configKey = configKey;
        this.priority = parseInteger(map.get("priority"), 1);
        this.material = String.valueOf(map.getOrDefault("material", "STONE"));
        this.amount = parseInteger(map.get("amount"), 1);
        this.displayName = map.containsKey("display_name") ? String.valueOf(map.get("display_name")) : null;

        Object loreObj = map.get("lore");
        if (loreObj instanceof List<?> list) {
            for (Object line : list) {
                if (line != null) this.lore.add(String.valueOf(line));
            }
        }

        this.customModelData = map.containsKey("custom_model_data") ? parseInteger(map.get("custom_model_data"), null) : null;
        this.glow = parseBoolean(map.get("hide_enchantments"), false) || map.containsKey("enchantments");
        this.unbreakable = parseBoolean(map.get("hide_unbreakable"), false);
        this.hideAttributes = parseBoolean(map.get("hide_attributes"), false);

        // Slots parsing (slot: 0, slots: [0, 1, 2], slots: "0-8")
        if (map.containsKey("slot")) {
            this.slots.add(parseInteger(map.get("slot"), 0));
        }
        if (map.containsKey("slots")) {
            Object slotsObj = map.get("slots");
            if (slotsObj instanceof List<?> list) {
                for (Object item : list) {
                    parseSlotItem(String.valueOf(item));
                }
            } else if (slotsObj instanceof String str) {
                parseSlotItem(str);
            }
        }
        if (this.slots.isEmpty()) {
            this.slots.add(0);
        }

        // View Requirements
        if (map.get("view_requirement") instanceof Map<?, ?> viewReqMap) {
            parseRequirements((Map<String, Object>) viewReqMap, this.viewRequirements);
        }

        // Click Commands
        parseActions(map.get("click_commands"), this.clickCommands);
        parseActions(map.get("left_click_commands"), this.leftClickCommands);
        parseActions(map.get("right_click_commands"), this.rightClickCommands);
        parseActions(map.get("shift_left_click_commands"), this.shiftLeftClickCommands);
        parseActions(map.get("shift_right_click_commands"), this.shiftRightClickCommands);
        parseActions(map.get("middle_click_commands"), this.middleClickCommands);

        // Click Requirements
        if (map.get("click_requirement") instanceof Map<?, ?> reqMap) {
            parseRequirements((Map<String, Object>) reqMap, this.clickRequirements);
        }
        if (map.get("left_click_requirement") instanceof Map<?, ?> reqMap) {
            parseRequirements((Map<String, Object>) reqMap, this.leftClickRequirements);
        }
        if (map.get("right_click_requirement") instanceof Map<?, ?> reqMap) {
            parseRequirements((Map<String, Object>) reqMap, this.rightClickRequirements);
        }
    }

    private void parseSlotItem(String item) {
        if (item.contains("-")) {
            String[] parts = item.split("-");
            try {
                int start = Integer.parseInt(parts[0].trim());
                int end = Integer.parseInt(parts[1].trim());
                for (int i = start; i <= end; i++) {
                    this.slots.add(i);
                }
            } catch (Exception ignored) {}
        } else {
            try {
                this.slots.add(Integer.parseInt(item.trim()));
            } catch (Exception ignored) {}
        }
    }

    @SuppressWarnings("unchecked")
    private void parseRequirements(Map<String, Object> reqMap, List<Requirement> target) {
        Object requirements = reqMap.get("requirements");
        if (requirements instanceof Map<?, ?> reqs) {
            for (Map.Entry<?, ?> entry : reqs.entrySet()) {
                if (entry.getValue() instanceof Map<?, ?> optMap) {
                    Object typeObj = optMap.get("type");
                    String type = typeObj != null ? String.valueOf(typeObj) : "";
                    target.add(new Requirement(type, (Map<String, Object>) optMap));
                }
            }
        }
    }

    private void parseActions(Object obj, List<ClickAction> target) {
        if (obj instanceof List<?> list) {
            for (Object item : list) {
                if (item != null) target.add(ClickAction.parse(String.valueOf(item)));
            }
        } else if (obj instanceof String str) {
            target.add(ClickAction.parse(str));
        }
    }

    public boolean canView(ServerPlayer player) {
        for (Requirement req : viewRequirements) {
            if (!req.evaluate(player)) {
                return false;
            }
        }
        return true;
    }

    public ItemStack createItemStack(ServerPlayer player) {
        return ItemBuilder.build(player, material, amount, displayName, lore, customModelData, glow, unbreakable, hideAttributes);
    }

    public void handleClick(ServerPlayer player, ClickType clickType, int button) {
        List<ClickAction> toExecute = new ArrayList<>();
        List<Requirement> requirementsToCheck = new ArrayList<>(clickRequirements);

        boolean isLeft = (button == 0 && clickType == ClickType.PICKUP);
        boolean isRight = (button == 1 && clickType == ClickType.PICKUP);
        boolean isShiftLeft = (button == 0 && clickType == ClickType.QUICK_MOVE);
        boolean isShiftRight = (button == 1 && clickType == ClickType.QUICK_MOVE);
        boolean isMiddle = (clickType == ClickType.CLONE);

        if (isShiftLeft && !shiftLeftClickCommands.isEmpty()) {
            toExecute.addAll(shiftLeftClickCommands);
        } else if (isShiftRight && !shiftRightClickCommands.isEmpty()) {
            toExecute.addAll(shiftRightClickCommands);
        } else if (isLeft && !leftClickCommands.isEmpty()) {
            toExecute.addAll(leftClickCommands);
            requirementsToCheck.addAll(leftClickRequirements);
        } else if (isRight && !rightClickCommands.isEmpty()) {
            toExecute.addAll(rightClickCommands);
            requirementsToCheck.addAll(rightClickRequirements);
        } else if (isMiddle && !middleClickCommands.isEmpty()) {
            toExecute.addAll(middleClickCommands);
        } else {
            toExecute.addAll(clickCommands);
        }

        // Evaluate requirements
        for (Requirement req : requirementsToCheck) {
            if (!req.evaluate(player)) {
                for (ClickAction deny : req.getDenyActions()) {
                    deny.execute(player);
                }
                return;
            }
        }

        // Execute actions
        for (ClickAction action : toExecute) {
            action.execute(player);
        }
    }

    public List<Integer> getSlots() {
        return Collections.unmodifiableList(slots);
    }

    public int getPriority() {
        return priority;
    }

    public String getConfigKey() {
        return configKey;
    }

    private int parseInteger(Object obj, Integer def) {
        if (obj == null) return def != null ? def : 0;
        try {
            return Integer.parseInt(String.valueOf(obj).trim());
        } catch (Exception e) {
            return def != null ? def : 0;
        }
    }

    private boolean parseBoolean(Object obj, boolean def) {
        if (obj == null) return def;
        return Boolean.parseBoolean(String.valueOf(obj).trim());
    }
}
