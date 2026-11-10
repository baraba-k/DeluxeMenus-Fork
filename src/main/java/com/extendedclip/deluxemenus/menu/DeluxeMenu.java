package com.extendedclip.deluxemenus.menu;

import com.extendedclip.deluxemenus.action.ClickAction;
import com.extendedclip.deluxemenus.placeholder.PlaceholderManager;
import com.extendedclip.deluxemenus.requirement.Requirement;
import com.extendedclip.deluxemenus.utils.ColorUtils;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public class DeluxeMenu {
    private final String name;
    private final String title;
    private final int size;
    private final int rows;
    private final List<String> openCommands = new ArrayList<>();
    private final List<Requirement> openRequirements = new ArrayList<>();
    private final List<ClickAction> openDenyActions = new ArrayList<>();
    private final Map<Integer, List<MenuItem>> slotItems = new HashMap<>();

    @SuppressWarnings("unchecked")
    public DeluxeMenu(String name, Map<String, Object> map) {
        this.name = name;
        this.title = map.containsKey("menu_title") ? String.valueOf(map.get("menu_title")) : name;

        int rawSize = parseInteger(map.get("size"), 54);
        // Normalize size to multiple of 9 between 9 and 54
        if (rawSize <= 9) this.rows = 1;
        else if (rawSize <= 18) this.rows = 2;
        else if (rawSize <= 27) this.rows = 3;
        else if (rawSize <= 36) this.rows = 4;
        else if (rawSize <= 45) this.rows = 5;
        else this.rows = 6;
        this.size = this.rows * 9;

        // Open Command(s)
        Object openCmdObj = map.get("open_command");
        if (openCmdObj instanceof List<?> list) {
            for (Object cmd : list) {
                if (cmd != null && !String.valueOf(cmd).trim().isEmpty()) {
                    this.openCommands.add(String.valueOf(cmd).trim().toLowerCase());
                }
            }
        } else if (openCmdObj instanceof String str && !str.trim().isEmpty()) {
            this.openCommands.add(str.trim().toLowerCase());
        }

        // Open Requirements
        if (map.get("open_requirement") instanceof Map<?, ?> reqMap) {
            Object reqsObj = reqMap.get("requirements");
            if (reqsObj instanceof Map<?, ?> reqs) {
                for (Map.Entry<?, ?> entry : reqs.entrySet()) {
                    if (entry.getValue() instanceof Map<?, ?> optMap) {
                        Object typeObj = optMap.get("type");
                        String type = typeObj != null ? String.valueOf(typeObj) : "";
                        this.openRequirements.add(new Requirement(type, (Map<String, Object>) optMap));
                    }
                }
            }
            Object denyObj = reqMap.get("deny_commands");
            if (denyObj instanceof List<?> list) {
                for (Object a : list) {
                    if (a != null) this.openDenyActions.add(ClickAction.parse(String.valueOf(a)));
                }
            } else if (denyObj instanceof String str) {
                this.openDenyActions.add(ClickAction.parse(str));
            }
        }

        // Items
        Object itemsObj = map.get("items");
        if (itemsObj instanceof Map<?, ?> itemsMap) {
            for (Map.Entry<?, ?> entry : itemsMap.entrySet()) {
                if (entry.getValue() instanceof Map<?, ?> itemData) {
                    MenuItem item = new MenuItem(String.valueOf(entry.getKey()), (Map<String, Object>) itemData);
                    for (int slot : item.getSlots()) {
                        if (slot >= 0 && slot < this.size) {
                            slotItems.computeIfAbsent(slot, k -> new ArrayList<>()).add(item);
                        }
                    }
                }
            }
        }

        // Sort slot items by priority (lower number = higher priority)
        for (List<MenuItem> list : slotItems.values()) {
            list.sort(Comparator.comparingInt(MenuItem::getPriority));
        }
    }

    public void open(ServerPlayer player) {
        if (player == null) return;

        // Check open requirements
        for (Requirement req : openRequirements) {
            if (!req.evaluate(player)) {
                for (ClickAction deny : req.getDenyActions()) {
                    deny.execute(player);
                }
                for (ClickAction deny : openDenyActions) {
                    deny.execute(player);
                }
                return;
            }
        }

        SimpleContainer container = new SimpleContainer(this.size);
        Map<Integer, MenuItem> activeSlotItems = new ConcurrentHashMap<>();
        populateContainer(container, player, activeSlotItems);

        MenuType<?> menuType = switch (this.rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            default -> MenuType.GENERIC_9x6;
        };

        String parsedTitle = PlaceholderManager.setPlaceholders(player, this.title);
        Component titleComponent = ColorUtils.format(parsedTitle);

        player.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, p) -> new DeluxeMenuContainer(menuType, containerId, playerInventory, container, this.rows, this, player, activeSlotItems),
                titleComponent
        ));
    }

    public void populateContainer(SimpleContainer container, ServerPlayer player, Map<Integer, MenuItem> activeSlotItems) {
        container.clearContent();
        activeSlotItems.clear();

        for (int slot = 0; slot < this.size; slot++) {
            List<MenuItem> candidates = slotItems.get(slot);
            if (candidates != null) {
                for (MenuItem item : candidates) {
                    if (item.canView(player)) {
                        ItemStack stack = item.createItemStack(player);
                        container.setItem(slot, stack);
                        activeSlotItems.put(slot, item);
                        break;
                    }
                }
            }
        }
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public int getSize() {
        return size;
    }

    public List<String> getOpenCommands() {
        return Collections.unmodifiableList(openCommands);
    }

    public Map<Integer, List<MenuItem>> getSlotItems() {
        return Collections.unmodifiableMap(slotItems);
    }

    private int parseInteger(Object obj, int def) {
        if (obj == null) return def;
        try {
            return Integer.parseInt(String.valueOf(obj).trim());
        } catch (Exception e) {
            return def;
        }
    }
}
