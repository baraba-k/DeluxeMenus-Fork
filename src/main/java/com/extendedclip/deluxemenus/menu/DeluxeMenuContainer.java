package com.extendedclip.deluxemenus.menu;

import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public class DeluxeMenuContainer extends ChestMenu {
    private final DeluxeMenu deluxeMenu;
    private final ServerPlayer serverPlayer;
    private final int totalMenuSlots;
    private final Map<Integer, MenuItem> activeSlotItems;

    public DeluxeMenuContainer(MenuType<?> menuType, int containerId, Inventory playerInventory, SimpleContainer container, int rows, DeluxeMenu deluxeMenu, ServerPlayer player, Map<Integer, MenuItem> activeSlotItems) {
        super(menuType, containerId, playerInventory, container, rows);
        this.deluxeMenu = deluxeMenu;
        this.serverPlayer = player;
        this.totalMenuSlots = rows * 9;
        this.activeSlotItems = activeSlotItems;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // Prevent all interaction with menu slots
        if (slotId >= 0 && slotId < totalMenuSlots) {
            MenuItem item = activeSlotItems.get(slotId);
            if (item != null && player instanceof ServerPlayer sp) {
                item.handleClick(sp, clickType, button);
            }
            this.sendAllDataToRemote();
            player.inventoryMenu.broadcastChanges();
            return;
        }

        // Prevent shift-clicking items from player inventory into the menu
        if (clickType == ClickType.QUICK_MOVE) {
            this.sendAllDataToRemote();
            player.inventoryMenu.broadcastChanges();
            return;
        }

        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public void refreshMenu() {
        if (serverPlayer != null) {
            deluxeMenu.populateContainer((SimpleContainer) this.getContainer(), serverPlayer, activeSlotItems);
            this.broadcastChanges();
        }
    }
}
