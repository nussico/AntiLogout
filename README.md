# AntiLogout

**Originally by samo_lego, maintained/modified by pafer29555.**

*If samo_lego does not want this fork to be public, I (pafer29555) will take it down upon request.*

---

## Overview

**AntiLogout** is a server-side mod that prevents players from escaping combat by logging out. When a player logs out during combat, their "body" remains online for a configurable amount of time, making combat logging impossible. The mod also provides a flexible `/afk` command for safe AFK handling.

---

## Features

- Prevents combat logging: players who log out during combat remain in the world.
- `/afk` command: lets players safely go AFK, with optional time limits.
- Configurable messages and timeouts.
- Permission-based command and feature access.
- Simple, clean configuration file (`antilogout.toml`).

---

## Installation

1. Download the mod JAR and place it in your server's `mods` folder.
2. Start the server to generate the config file.
3. Edit `config/antilogout.toml` to customize settings.

---

## Configuration

All options are in `config/antilogout.toml`.

**Key options:**
- `disableAllLogouts`: Disable all logout protection features.
- `debug`: Enable debug logging.
- `afkMessage`: Message shown when a player is AFK.
- `afkCombatMessage`: Message shown if a player tries to go AFK while in combat.
- `afkBroadcastMessage`: Broadcast when a player goes AFK (`{player}` = name).
- `combatEnterMessage`: Message when entering combat.
- `combatEndMessage`: Message when leaving combat.
- `combatTimeout`: How long a player is considered in combat (seconds).
- `combatDisconnectMessage`: Message when a player disconnects during combat.

---

## Commands

- `/afk`  
  Set yourself AFK for the max time. (Permission level 0)
- `/afk time <seconds>`  
  Set yourself AFK for a specific time (`-1` for unlimited). (Permission level 0)
- `/afk players <targets> [time <seconds>]`  
  Set other players AFK. (Admin only, permission level 4)
- `/antilogout reload`  
  Reload the config file. (Admin only, permission level 4)
- `/antilogout status`  
  Show current config summary. (Admin only, permission level 4)

---

## Permissions

- `antilogout.bypass.combat` — Bypass combat tagging.
- `antilogout.command.afk` — Use `/afk` (level 1).
- `antilogout.command.afk.time` — Set AFK time for yourself (level 0).
- `antilogout.command.afk.players` — Set other players AFK (admin only, level 4).
- `antilogout.command.antilogout` — Use `/antilogout` admin commands (level 4).
- `antilogout.command.antilogout.reload` — Reload config (level 4).
- `antilogout.command.antilogout.edit` — Edit config in-game (if enabled, level 4).

---

## Examples

**Combat log prevention:**  
Players who log out during combat remain in the world for the configured timeout.

**AFK farming:**  
Use `/afk` to safely go AFK for farming or other purposes.

---

## Video Showcase

- [Combat log prevention demo](https://user-images.githubusercontent.com/34912839/213432960-15d54218-8313-4470-868b-10eb78357764.mp4)
- [AFK farming demo](https://user-images.githubusercontent.com/34912839/213676495-f3125d24-d42d-4ee9-80d2-55f33d313aae.mp4)

---

For questions, suggestions, or issues, please open an issue on GitHub or contact the maintainer.
