package com.extendedclip.deluxemenus;

import com.extendedclip.deluxemenus.command.DeluxeMenusCommand;
import com.extendedclip.deluxemenus.config.MenuLoader;
import com.extendedclip.deluxemenus.menu.DeluxeMenu;
import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(DeluxeMenusMod.MOD_ID)
public class DeluxeMenusMod {
    public static final String MOD_ID = "deluxemenus";
    public static final Logger LOGGER = LogManager.getLogger("DeluxeMenus");

    private static final Map<String, DeluxeMenu> LOADED_MENUS = new ConcurrentHashMap<>();
    private static File configDirectory;
    private static MinecraftServer currentServer;

    public DeluxeMenusMod(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
        NeoForge.EVENT_BUS.addListener(this::serverStarting);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        configDirectory = FMLPaths.CONFIGDIR.get().resolve("deluxemenus").toFile();
        reload();
    }

    private void registerCommands(final RegisterCommandsEvent event) {
        DeluxeMenusCommand.register(event.getDispatcher());
    }

    private void serverStarting(final ServerStartingEvent event) {
        currentServer = event.getServer();
        LOGGER.info("[DeluxeMenus] Successfully loaded {} menus for Minecraft 1.21.1 NeoForge!", LOADED_MENUS.size());
    }

    public static void reload() {
        if (configDirectory == null) {
            configDirectory = FMLPaths.CONFIGDIR.get().resolve("deluxemenus").toFile();
        }

        LOADED_MENUS.clear();
        Map<String, DeluxeMenu> menus = MenuLoader.loadMenus(configDirectory);
        LOADED_MENUS.putAll(menus);
        LOGGER.info("[DeluxeMenus] Reloaded {} menus.", LOADED_MENUS.size());

        if (currentServer != null) {
            DeluxeMenusCommand.registerCustomMenuCommands(currentServer.getCommands().getDispatcher());
        }
    }

    public static Map<String, DeluxeMenu> getLoadedMenus() {
        return Collections.unmodifiableMap(LOADED_MENUS);
    }

    public static DeluxeMenu getMenu(String name) {
        if (name == null) return null;
        return LOADED_MENUS.get(name.toLowerCase());
    }

    public static File getConfigDirectory() {
        return configDirectory;
    }

    public static MinecraftServer getServer() {
        return currentServer;
    }
}
