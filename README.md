# Party Animals

> [!IMPORTANT]
> This project is currently in alpha development. Breaking changes can be expected, and some features may not work as intended. Use at your own risk.

A Minecraft plugin for Paper servers that spawns interactive pinata entities. Players work together to break pinatas and earn configurable rewards through a chance-based system with permission support.

## Features

- Configure pinata entity types, health, scale and spawn locations
- **Flexible Reward System**:
  - Chance-based reward distribution
  - Permission-restricted rewards (optional)
  - Server-wide or player-specific rewards
  - Randomizable command execution
  - PlaceholderAPI support for reward commands
- **Interactive Mechanics**:
  - Countdown timer before pinata spawns
  - Boss bar showing health and status
  - Hit cooldown (per-player or global)
  - Timeout system for inactive pinatas
  - Last-hit tracking
- MiniMessage support

## Installation

1. Ensure your server is running **Java 21**+ and **Paper 1.21**+ (or a fork).
2. Download the latest release from [GitHub Releases](https://github.com/muhdfdeen/partyanimals/releases).
3. Place the JAR file in your `plugins` folder.
4. Optional: Install [PlaceholderAPI](https://placeholderapi.com/) for advanced placeholder support in reward commands.
5. Restart your server.

## Commands & Permissions

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/partyanimals` | None | Displays plugin version information. |
| `/partyanimals reload` | `partyanimals.reload` | Reloads all configuration files. |
| `/partyanimals start <location>` | `partyanimals.start` | Starts a countdown to spawn a pinata at the specified location. |
| `/partyanimals summon <location>` | `partyanimals.summon` | Instantly spawns a pinata at the specified location. |
| `/partyanimals addspawnlocation <name>` | `partyanimals.addspawnlocation` | Saves your current location as a named spawn point. |
| N/A | `partyanimals.admin` | Receive update notifications on join. |


## Building

```bash
./gradlew build
```

The compiled JAR will be in `build/libs/`.

## Support

For issues or questions, open an issue on [GitHub](https://github.com/muhdfdeen/PartyAnimals/issues).

## Statistics

This plugin uses [bStats](https://bstats.org/) to collect anonymous usage statistics. This can be disabled in the bStats config.
