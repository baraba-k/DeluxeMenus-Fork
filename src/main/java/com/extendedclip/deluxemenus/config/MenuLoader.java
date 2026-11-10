package com.extendedclip.deluxemenus.config;

import com.extendedclip.deluxemenus.menu.DeluxeMenu;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.yaml.snakeyaml.Yaml;

public class MenuLoader {
    private static final Yaml YAML = new Yaml();

    public static Map<String, DeluxeMenu> loadMenus(File configDir) {
        Map<String, DeluxeMenu> menus = new HashMap<>();

        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        File menusDir = new File(configDir, "menus");
        if (!menusDir.exists()) {
            menusDir.mkdirs();
        }

        // Extract default menus if empty
        extractDefaultsIfEmpty(menusDir);

        // Load config.yml if it exists
        File configFile = new File(configDir, "config.yml");
        if (configFile.exists()) {
            loadFromConfigFile(configFile, configDir, menus);
        }

        // Also load all .yml files in menus/ directory directly
        File[] menuFiles = menusDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (menuFiles != null) {
            for (File file : menuFiles) {
                try {
                    String menuName = file.getName().substring(0, file.getName().length() - 4).toLowerCase();
                    if (!menus.containsKey(menuName)) {
                        DeluxeMenu menu = loadMenuFile(file, menuName);
                        if (menu != null) {
                            menus.put(menuName, menu);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[DeluxeMenus] Failed to load menu file: " + file.getName() + " - " + e.getMessage());
                }
            }
        }

        return menus;
    }

    @SuppressWarnings("unchecked")
    private static void loadFromConfigFile(File configFile, File baseDir, Map<String, DeluxeMenu> menus) {
        try (InputStream in = new FileInputStream(configFile)) {
            Map<String, Object> data = YAML.load(in);
            if (data != null && data.get("gui_menus") instanceof Map<?, ?> guiMenus) {
                for (Map.Entry<?, ?> entry : guiMenus.entrySet()) {
                    String menuName = String.valueOf(entry.getKey()).toLowerCase();
                    if (entry.getValue() instanceof Map<?, ?> menuDef) {
                        if (menuDef.containsKey("file")) {
                            String fileName = String.valueOf(menuDef.get("file"));
                            File menuFile = new File(baseDir, "menus/" + fileName);
                            if (!menuFile.exists()) {
                                menuFile = new File(baseDir, fileName);
                            }
                            if (menuFile.exists()) {
                                DeluxeMenu menu = loadMenuFile(menuFile, menuName);
                                if (menu != null) {
                                    menus.put(menuName, menu);
                                }
                            }
                        } else {
                            // Inline menu definition
                            menus.put(menuName, new DeluxeMenu(menuName, (Map<String, Object>) menuDef));
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[DeluxeMenus] Failed to load config.yml: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static DeluxeMenu loadMenuFile(File file, String fallbackName) {
        try (InputStream in = new FileInputStream(file)) {
            Map<String, Object> data = YAML.load(in);
            if (data == null) return null;
            return new DeluxeMenu(fallbackName, data);
        } catch (Exception e) {
            System.err.println("[DeluxeMenus] Error parsing menu file " + file.getName() + ": " + e.getMessage());
            return null;
        }
    }

    private static void extractDefaultsIfEmpty(File menusDir) {
        File[] existing = menusDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (existing == null || existing.length == 0) {
            String[] defaultNames = {
                "basics_menu.yml",
                "advanced_menu.yml",
                "default_menu.yml",
                "requirements_menu.yml",
                "actions_menu.yml",
                "items_menu.yml"
            };

            for (String name : defaultNames) {
                try (InputStream in = MenuLoader.class.getResourceAsStream("/default_menus/" + name)) {
                    if (in != null) {
                        File target = new File(menusDir, name);
                        try (OutputStream out = new FileOutputStream(target)) {
                            in.transferTo(out);
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
    }
}
