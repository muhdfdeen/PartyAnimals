# Party Animals

Party Animals is a plugin that elevates your server events by introducing interactive, loot-filled Pinatas. The purpose of the plugin is to add excitement to your events and provide a fun, configurable way to distribute rewards to your players.
<!--
## Documentation

Any and all information you need for the plugin should already be included in the [Wiki](link-here)!

## Support

If there is anything missing, you have a question, you want to report a bug, or anything else, please join the [Discord server](link-here). Support is available in the server.
-->
## Server Compatibility

Party Animals is compatible with **Paper** and its forks. It is recommended to use Paper to run your server.

This plugin supports Minecraft versions **1.21** and newer running **Java 21**.

## Installation

1. Download the latest JAR file from the **Releases** page.
2. Place the file into your server's `plugins` directory.
3. Restart the server.

*Note: [PlaceholderAPI](https://placeholderapi.com/) is optional but recommended for placeholder support.*

## Commands and Permissions

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/partyanimals` (or `/pa`) | None | Displays plugin version information. |
| `/pa reload` | `partyanimals.reload` | Reloads all configuration files and pinata templates. |
| `/pa start [template] [location]` | `partyanimals.start` | Starts the countdown for a specific pinata template at a saved location. |
| `/pa spawn [template] [location]` | `partyanimals.spawn` | Spawns a pinata immediately, skipping the countdown. |
| `/pa killall` | `partyanimals.killall` | Removes all active pinata entities from the world. |
| `/pa addlocation <name>` | `partyanimals.addlocation` | Saves the player's current location as a named spawn point. |
| `/pa removelocation <name>` | `partyanimals.removelocation` | Removes a saved spawn point. |

## Configuration

This plugin uses a modular configuration system located in the `plugins/PartyAnimals/` directory.

* `config.yml`: General settings and module toggles.
* `messages.yml`: All plugin text, supporting [MiniMessage](https://docs.advntr.dev/minimessage/format.html) formatting.
* `pinatas/`: Directory containing individual pinata template files (e.g., `default.yml`).

## Building from Source

To build this project locally, ensure you have **JDK 21** or newer installed.

* On Linux or macOS: `./gradlew build`
* On Windows: `gradlew build`

The compiled artifact will be located in `build/libs/`.
