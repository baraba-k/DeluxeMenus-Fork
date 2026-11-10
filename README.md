# DeluxeMenus (NeoForge 1.21.1 Fork)

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg)](https://www.minecraft.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.121-orange.svg)](https://neoforged.net/)
[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://adoptium.net/)
[![Version](https://img.shields.io/badge/Version-1.0.0-blueviolet.svg)](https://github.com/baraba-k/DeluxeMenus-Fork/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A modern, high-performance port and fork of the popular Spigot/Paper menu plugin **DeluxeMenus** to **Minecraft 1.21.1** powered by **NeoForge**.

Create completely customizable, interactive GUI menus, server selectors, item shops, kits, and reward systems using standard YAML configuration files.

---

## Features

- **Universal Vanilla Chest GUIs:**
  - Operates seamlessly both on dedicated servers and in singleplayer.
  - Utilizes standard Minecraft chest containers (`ChestMenu`, sizes 9 to 54 slots).
  - Players on dedicated servers do not need any client-side mods to view and interact with menus.
- **Native DeluxeMenus YAML Configuration:**
  - Matches the Spigot/Paper DeluxeMenus YAML schema (`config.yml`, `menu_title`, `open_command`, `size`, `items`, `priority`, etc.).
  - Default template menus automatically generate on first startup.
- **Extensive Click Actions:**
  - `[console] <command>`: Execute console commands.
  - `[player] <command>`: Execute commands as the clicking player.
  - `[message] <text>`: Send private formatted chat messages (supports `&` codes and `#RRGGBB` hex colors).
  - `[actionbar] <text>`: Display action bar notifications.
  - `[broadcast] <text>`: Broadcast announcements across the server.
  - `[sound] <sound_id> [volume] [pitch]`: Play sounds at player position.
  - `[openguimenu] <menu_name>`: Transition directly to another menu.
  - `[close]`: Close active GUI container.
  - `[refresh]`: Dynamically re-evaluate slots, conditions, and items.
  - `[giveexp] <amount>` / `[takeexp] <amount>`: Modify player experience points or levels.
  - `[ephemeralcooldown] <tag> <seconds>`: In-memory cooldowns without database bloat.
  - `[meta] set <key> <val>` / `[meta] remove <key>`: Per-player persistent metadata storage.
- **View and Click Requirements:**
  - Permission checks: `has permission <permission_level>` (0 to 4).
  - Inventory checks: `has item <item_id> [amount]`.
  - Experience checks: `has exp <amount> [level: true/false]`.
  - Value comparisons: `==`, `!=`, `>`, `>=`, `<`, `<=`.
  - String evaluation: `string contains`, `regex matches`.
  - Cooldown checks: `ephemeral cooldown`.
  - Scripting: `javascript <expression>` through embedded expression parser.
  - `deny_commands`: Triggers custom actions when requirements are unmet.
- **Dynamic Placeholders:**
  - Player info: `%player_name%`, `%player_uuid%`, `%player_displayname%`.
  - Position & world: `%player_x%`, `%player_y%`, `%player_z%`, `%player_dimension%`.
  - Vitals: `%player_health%`, `%player_max_health%`, `%player_food_level%`, `%player_gamemode%`.
  - Stats: `%player_exp%`, `%player_level%`, `%player_total_exp%`, `%player_ping%`.
  - Server metrics: `%server_online%`, `%server_max_players%`, `%server_tps%`.
  - Custom metadata: `%meta_<key>%`.
- **Modern Item Component Engine (Minecraft 1.21.1):**
  - Native Data Components architecture.
  - RGB hex color codes (`&#RRGGBB`) and standard Minecraft formatting codes (`&a`, `&l`, etc.).
  - Player heads (`PLAYER_HEAD` with `skull_owner`).
  - Custom Model Data (`custom_model_data`).
  - Enchantment glint override (`enchanted: true`).
  - Unbreakable items (`unbreakable: true`).
  - Leather and potion custom RGB tints (`rgb: R, G, B`).

---

## Commands and Permissions

| Command | Permission Level | Description |
|:---|:---:|:---|
| `/dm open <menu> [player]` | Level 2 (or OP) | Open a designated menu for yourself or target player |
| `/dm list` | Level 0 (All) | List all registered menus |
| `/dm reload` | Level 2 (or OP) | Reload `config.yml` and all menu YAML files |
| `/dm help` | Level 0 (All) | View help and command usage |
| `/<open_command>` | Menu Defined | Custom direct menu aliases configured via `open_command` (e.g. `/basicsmenu`) |

Aliases: `/deluxemenus`, `/dm`

---

## Configuration

Configurations are stored in `config/deluxemenus/`:

```
config/deluxemenus/
├── config.yml
└── menus/
    ├── basics_menu.yml
    ├── advanced_menu.yml
    ├── requirements_menu.yml
    ├── actions_menu.yml
    ├── items_menu.yml
    └── default_menu.yml
```

### Example Menu Configuration

```yaml
menu_title: '&8[&6DeluxeMenus&8] &aShop'
open_command: shop
size: 27

items:
  'diamond_sword':
    material: DIAMOND_SWORD
    slot: 13
    priority: 1
    display_name: '&#FFAA00&lLegendary Sword'
    lore:
      - '&7Cost: &a100 EXP'
      - ''
      - '&eClick to purchase!'
    enchanted: true
    view_requirement:
      requirements:
        has_level:
          type: has exp
          amount: 100
          level: false
    click_requirement:
      requirements:
        has_level:
          type: has exp
          amount: 100
          level: false
          deny_commands:
            - '[message] &cYou do not have enough experience!'
            - '[sound] minecraft:entity.villager.no 1.0 1.0'
    left_click_commands:
      - '[takeexp] 100'
      - '[console] give %player_name% diamond_sword[custom_name=''{"text":"Legendary Sword","color":"gold"}'' run_command 1'
      - '[message] &aYou purchased the Legendary Sword!'
      - '[sound] minecraft:entity.player.levelup 1.0 1.0'
      - '[refresh]'
```

---

## Building from Source

Requires Java 21 JDK:

```bash
git clone https://github.com/baraba-k/DeluxeMenus-Fork.git
cd DeluxeMenus-Fork
./gradlew build
```

Compiled jar will be located at:
`build/libs/DeluxeMenus-1.21.1-neoforge.jar`

---

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
