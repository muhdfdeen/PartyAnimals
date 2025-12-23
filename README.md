# Party Animals

> [!IMPORTANT]
> This project is currently in alpha development. Breaking changes can be expected, and some features may not work as intended. Use at your own risk.

A Minecraft plugin for Paper servers that spawns interactive pinata entities. Players work together to break pinatas and earn configurable rewards through a chance-based system with permission support.

## Features

### General

- MiniMessage support for all broadcasts and messages
- Extensive PlaceholderAPI support throughout configuration

### Pinata

- Configure entity types (`LLAMA`, `MULE`, etc.), health, scale, and glowing effects
- Configurable wandering behavior, knockback resistance, and pathfinding
- Define custom spawn locations and summon via command
- Mechanics:
  - Configurable countdown timer with visual effects before pinata spawns
  - Configurable boss bars showing health and lifespan status
  - Configurable hit cooldowns (Global or Per-Player)
  - Configurable item whitelist to restrict damage to specific items (e.g., sticks only)
  - Configurable timeout system to automatically despawn pinatas if not killed in time
- Reward System:
  - Attach rewards to specific events: `spawn`, `hit`, `lastHit`, and `death`
  - Chance-based reward distribution using commands
  - Permission-restricted rewards (optional)
  - Server-wide or player-specific rewards
  - Randomizable command execution within a list of commands (optional)
  - PlaceholderAPI support for reward commands

## Planned Features

### Pinata

- Ability to spawn automatically after a certain condition is met
  - PlaceholderAPI conditions
  - Pair with the vote module after it is implemented

### Vote

- Implement Votifier support
- Offline vote support
- Vote reminders
- Milestones (separate from pinata)
  - Repetitive or cumulative can be switched around

## Building

```bash
./gradlew build
```

The compiled JAR will be in `build/libs/`.

## Support

For issues or questions, open an issue on [GitHub](https://github.com/muhdfdeen/PartyAnimals/issues).
