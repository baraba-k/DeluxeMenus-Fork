package com.extendedclip.deluxemenus.utils;

import com.extendedclip.deluxemenus.placeholder.PlaceholderManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.component.Unbreakable;

public class ItemBuilder {

    public static ItemStack build(
            ServerPlayer player,
            String rawMaterial,
            int amount,
            String displayName,
            List<String> loreLines,
            Integer customModelData,
            boolean glow,
            boolean unbreakable,
            boolean hideAttributes
    ) {
        if (rawMaterial == null || rawMaterial.isEmpty()) {
            return ItemStack.EMPTY;
        }

        String materialStr = PlaceholderManager.setPlaceholders(player, rawMaterial).toLowerCase();
        Item item = Items.STONE;
        String skullOwner = null;

        if (materialStr.startsWith("head-")) {
            item = Items.PLAYER_HEAD;
            skullOwner = materialStr.substring(5).trim();
        } else {
            ResourceLocation loc;
            if (materialStr.contains(":")) {
                loc = ResourceLocation.parse(materialStr);
            } else {
                loc = ResourceLocation.withDefaultNamespace(materialStr);
            }

            if (BuiltInRegistries.ITEM.containsKey(loc)) {
                item = BuiltInRegistries.ITEM.get(loc);
            } else {
                // Try fuzzy lookup (e.g. "player_head", "wool", etc.)
                for (Item regItem : BuiltInRegistries.ITEM) {
                    ResourceLocation key = BuiltInRegistries.ITEM.getKey(regItem);
                    if (key.getPath().equalsIgnoreCase(materialStr)) {
                        item = regItem;
                        break;
                    }
                }
            }
        }

        if (item == Items.AIR) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = new ItemStack(item, Math.max(1, Math.min(64, amount)));

        // Display Name
        if (displayName != null && !displayName.isEmpty()) {
            String parsedName = PlaceholderManager.setPlaceholders(player, displayName);
            stack.set(DataComponents.CUSTOM_NAME, ColorUtils.format(parsedName));
        }

        // Lore
        if (loreLines != null && !loreLines.isEmpty()) {
            List<Component> components = new ArrayList<>();
            for (String line : loreLines) {
                String parsedLine = PlaceholderManager.setPlaceholders(player, line);
                components.add(ColorUtils.format(parsedLine));
            }
            stack.set(DataComponents.LORE, new ItemLore(components));
        }

        // Custom Model Data
        if (customModelData != null) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(customModelData));
        }

        // Player Head
        if (skullOwner != null && !skullOwner.isEmpty()) {
            String parsedOwner = PlaceholderManager.setPlaceholders(player, skullOwner);
            stack.set(DataComponents.PROFILE, new ResolvableProfile(new com.mojang.authlib.GameProfile(net.minecraft.core.UUIDUtil.createOfflinePlayerUUID(parsedOwner), parsedOwner)));
        }

        // Glow (Enchantment Glint Override)
        if (glow) {
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        }

        // Unbreakable
        if (unbreakable) {
            stack.set(DataComponents.UNBREAKABLE, new Unbreakable(true));
        }

        // Hide Attributes
        if (hideAttributes) {
            stack.set(DataComponents.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
        }

        return stack;
    }
}
