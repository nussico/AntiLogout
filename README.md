# AntiLogout

Server-side Fabric mod that prevents combat logging. When a player disconnects during combat, the player remains in the world as a temporary dummy until the combat timer expires. The mod also provides an AFK command for intentional disconnects.

## Current Version

- Minecraft: `26.3`
- Fabric Loader: `0.19.5` or newer
- Fabric API: `0.161.0+26.3` or newer
- Java: `25` or newer

AntiLogout is server-side only. Players do not need to install the mod on their client.

## Features

- Prevents combat logging with configurable combat timers.
- Keeps disconnected combat players in the world as dummies.
- Restores death messages when a disconnected dummy dies.
- Provides `/afk` for intentional AFK disconnects.
- Supports configurable messages, timeouts, permissions, and debug logging.
- Supports configuration reload without restarting the server.

## Installation

1. Install a Fabric `26.3` server with Java `25`.
2. Install Fabric API `0.161.0+26.3` or newer.
3. Put `antilogout-2.1.0.jar` in the server's `mods` directory.
4. Start the server once.
5. Edit `config/antilogout.toml` if needed.
6. Use `/antilogout reload` after changing the configuration in-game.

The mod creates its configuration file automatically at:

```text
config/antilogout.toml
```

## Configuration

The configuration is divided into three sections:

```toml
[general]
disableAllLogouts = false
debug = false

[afk]
afkMessage = "You are now AFK!"
permissionLevel = 0
maxAfkTime = 300.0
afkCombatMessage = "You disconnected while in combat!"
afkBroadcastMessage = "{player} is now AFK!"

[combatLog]
notifyOnCombat = true
combatEnterMessage = "You are in combat!"
combatEndMessage = "You are no longer in combat!"
combatTimeout = 30
playerHurtOnly = true
bypassPermissionLevel = 4
combatDisconnectMessage = "disconnected while in combat!"
```

Important options:

- `general.disableAllLogouts`: disables combat logout protection.
- `general.debug`: enables diagnostic logging.
- `afk.maxAfkTime`: maximum AFK duration in seconds. Use `-1` for unlimited AFK time.
- `combatLog.combatTimeout`: combat duration in seconds.
- `combatLog.playerHurtOnly`: when enabled, only player damage and player-owned projectiles trigger combat.
- `combatLog.bypassPermissionLevel`: minimum level for bypassing combat tagging.
- `{player}` in `afkBroadcastMessage` is replaced with the player's name.

## Commands

```text
/afk
/afk time <seconds>
/afk players <targets>
/afk players <targets> time <seconds>
/antilogout reload
/antilogout status
/antilogout get <option>
/antilogout set <option> <value>
/al ...
```

`/afk time -1` and `/afk players <targets> time -1` create unlimited AFK sessions. AFK is blocked while the player is in combat.

The `/antilogout` commands require administrator permissions. Configuration values changed with `/antilogout set` are validated and saved immediately.

## Permissions

The following permission nodes are supported through Fabric's permission API:

- `antilogout.bypass.combat`: bypass combat tagging.
- `antilogout.command.afk`: use `/afk`.
- `antilogout.command.afk.time`: set your own AFK duration.
- `antilogout.command.afk.players`: set other players AFK.
- `antilogout.command.afk.players.time`: set a custom AFK duration for other players.
- `antilogout.command.antilogout`: use `/antilogout`.
- `antilogout.command.antilogout.reload`: reload the configuration.
- `antilogout.command.antilogout.edit`: edit configuration in-game.

Permission levels are used as fallbacks when no external permission provider overrides a node.

## Building From Source

The project requires Java `25` and uses the Gradle wrapper:

```powershell
./gradlew clean build
```

The release JAR is written to:

```text
build/libs/antilogout-2.1.0.jar
```

The `26.3` branch contains the Minecraft 26.3 port. The repository uses `upstream` for the original project and `origin` for the maintained fork.

## License and Credits

AntiLogout was originally created by samo_lego and is maintained in this fork by nussico.

Licensed under `LGPL-3.0`.

## Media

- [Combat log prevention demo](https://user-images.githubusercontent.com/34912839/213432960-15d54218-8313-4470-868b-10eb78357764.mp4)
- [AFK farming demo](https://user-images.githubusercontent.com/34912839/213676495-f3125d24-d42d-4ee8-80d2-55f33d313aae.mp4)
