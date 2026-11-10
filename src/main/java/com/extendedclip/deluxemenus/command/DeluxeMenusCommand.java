package com.extendedclip.deluxemenus.command;

import com.extendedclip.deluxemenus.DeluxeMenusMod;
import com.extendedclip.deluxemenus.menu.DeluxeMenu;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Collection;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class DeluxeMenusCommand {

    private static final SuggestionProvider<CommandSourceStack> MENU_SUGGESTIONS = (context, builder) -> {
        for (String menuName : DeluxeMenusMod.getLoadedMenus().keySet()) {
            if (menuName.toLowerCase().startsWith(builder.getRemaining().toLowerCase())) {
                builder.suggest(menuName);
            }
        }
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var baseCommand = Commands.literal("deluxemenus")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal("§6[DeluxeMenus] §eUse §f/deluxemenus help §efor a list of commands."), false);
                    return 1;
                })
                .then(Commands.literal("help")
                        .executes(context -> {
                            CommandSourceStack src = context.getSource();
                            src.sendSuccess(() -> Component.literal("§6--- §eDeluxeMenus Commands §6---"), false);
                            src.sendSuccess(() -> Component.literal("§e/deluxemenus reload §7- Reload all menus"), false);
                            src.sendSuccess(() -> Component.literal("§e/deluxemenus list §7- List all loaded menus"), false);
                            src.sendSuccess(() -> Component.literal("§e/deluxemenus open <menu> [player] §7- Open a menu"), false);
                            src.sendSuccess(() -> Component.literal("§e/deluxemenus execute <menu> <slot> [player] §7- Execute menu slot"), false);
                            return 1;
                        }))
                .then(Commands.literal("reload")
                        .executes(context -> {
                            DeluxeMenusMod.reload();
                            int count = DeluxeMenusMod.getLoadedMenus().size();
                            context.getSource().sendSuccess(() -> Component.literal("§a[DeluxeMenus] Successfully reloaded " + count + " menus!"), true);
                            return 1;
                        }))
                .then(Commands.literal("list")
                        .executes(context -> {
                            CommandSourceStack src = context.getSource();
                            Map<String, DeluxeMenu> menus = DeluxeMenusMod.getLoadedMenus();
                            src.sendSuccess(() -> Component.literal("§6[DeluxeMenus] §eLoaded Menus (" + menus.size() + "):"), false);
                            for (Map.Entry<String, DeluxeMenu> entry : menus.entrySet()) {
                                String cmds = String.join(", ", entry.getValue().getOpenCommands());
                                src.sendSuccess(() -> Component.literal(" §7- §a" + entry.getKey() + " §7(size: " + entry.getValue().getSize() + (cmds.isEmpty() ? "" : ", commands: /" + cmds) + ")"), false);
                            }
                            return 1;
                        }))
                .then(Commands.literal("open")
                        .then(Commands.argument("menu", StringArgumentType.string())
                                .suggests(MENU_SUGGESTIONS)
                                .executes(context -> {
                                    String menuName = StringArgumentType.getString(context, "menu").toLowerCase();
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    return openMenu(context.getSource(), menuName, player);
                                })
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> {
                                            String menuName = StringArgumentType.getString(context, "menu").toLowerCase();
                                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                            return openMenu(context.getSource(), menuName, target);
                                        }))));

        dispatcher.register(baseCommand);
        dispatcher.register(Commands.literal("dm").requires(source -> source.hasPermission(2)).redirect(dispatcher.getRoot().getChild("deluxemenus")));

        // Register custom open_commands for each menu
        registerCustomMenuCommands(dispatcher);
    }

    public static void registerCustomMenuCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (DeluxeMenu menu : DeluxeMenusMod.getLoadedMenus().values()) {
            for (String alias : menu.getOpenCommands()) {
                if (alias.isEmpty()) continue;
                String cleanAlias = alias.startsWith("/") ? alias.substring(1) : alias;
                try {
                    dispatcher.register(Commands.literal(cleanAlias)
                            .executes(ctx -> {
                                ServerPlayer player = ctx.getSource().getPlayer();
                                if (player != null) {
                                    menu.open(player);
                                    return 1;
                                }
                                ctx.getSource().sendFailure(Component.literal("§cThis command can only be executed by a player!"));
                                return 0;
                            }));
                } catch (Exception ignored) {
                    // Command might already be registered
                }
            }
        }
    }

    private static int openMenu(CommandSourceStack source, String menuName, ServerPlayer target) {
        DeluxeMenu menu = DeluxeMenusMod.getMenu(menuName);
        if (menu == null) {
            source.sendFailure(Component.literal("§c[DeluxeMenus] Menu '" + menuName + "' not found!"));
            return 0;
        }

        menu.open(target);
        if (source.getEntity() != target) {
            source.sendSuccess(() -> Component.literal("§a[DeluxeMenus] Opened menu '" + menuName + "' for " + target.getScoreboardName()), false);
        }
        return 1;
    }
}
