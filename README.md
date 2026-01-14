<div align="center">
  <h3>PartyAnimals</h3>
  <p>The modular player engagement and voting solution for Paper servers</p>
</div>

***

**PartyAnimals** is a modular **player engagement plugin** for **Paper** servers designed to reward your community through **voting** and interactive, **loot-filled Pinatas**. The purpose of the plugin is to boost engagement, reward voters, and automate excitement with community-driven events.

### Features

PartyAnimals handles player retention with a focus on these core functions:

* It spawns interactive **pinata** entities (can be any mob!) that players can strike to receive configurable rewards.
* You can configure their behaviors, allowing pinatas to roam, flee from players, or defend themselves with **reflexes** like *Shockwave* (knockback), *Blink* (teleportation), and more!
* It features a complete **voting module** that integrates with **NuVotifier** to track votes, handle offline queuing, and manage **community goals**.
* It includes extensive customization for rewards, messages, and interaction rules, with optional **PlaceholderAPI** support.

### Prerequisites

To use this plugin, your server must be running **Paper**, **Purpur**, or **Folia** on `1.21` or higher. It requires **Java 21** and the [NuVotifier](https://github.com/NuVotifier/NuVotifier) plugin to properly handle voting features.

#### Dependencies

Both dependencies are optional, but highly recommended:

* [NuVotifier](https://github.com/NuVotifier/NuVotifier)
* [PlaceholderAPI](https://placeholderapi.com/)

### Documentation & Support

For a complete guide on features, commands, and configuration, please visit our [Wiki](https://docs.maboroshi.org/). If you have questions or need to report a bug, join our [Discord server](https://discord.maboroshi.org).

### Statistics

This plugin utilizes [bStats](https://bstats.org/plugin/bukkit/PartyAnimals/28389) to collect anonymous usage metrics.

![bStats Metrics](https://bstats.org/signatures/bukkit/PartyAnimals.svg)

## Building

If you wish to build the project from source, ensure you have a Java 21 environment configured.

```bash
./gradlew build
```
