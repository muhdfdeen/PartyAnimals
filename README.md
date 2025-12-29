# Party Animals

A feature-rich Minecraft plugin for Paper servers that spawns interactive pinata entities. Players collaborate to break pinatas and earn configurable, chance-based rewards and PlaceholderAPI support. Includes modular configuration, live reload, and a developer API for custom event handling.

> [!IMPORTANT]
> This project is currently in Alpha. While stable for testing, breaking changes to configuration structures may occur.

## Building

```bash
./gradlew build
```

The compiled JAR will be in `build/libs/`.

## Developer API

Developers can listen to custom events to extend functionality:
* [`PinataSpawnEvent`](src/main/java/com/muhdfdeen/partyanimals/api/event/PinataSpawnEvent.java)
* [`PinataHitEvent`](src/main/java/com/muhdfdeen/partyanimals/api/event/PinataHitEvent.java)
* [`PinataDeathEvent`](src/main/java/com/muhdfdeen/partyanimals/api/event/PinataDeathEvent.java)

## Support

For issues or questions, open an issue on [GitHub](https://github.com/muhdfdeen/PartyAnimals/issues).

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.
