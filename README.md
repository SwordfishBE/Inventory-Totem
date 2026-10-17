# Inventory Totem

A totem of undying anywhere in the player's inventory can save them from death.

## Requirements

Install `inventory_totem-<version>.jar` in the server's `mods` folder, or in the local instance for singleplayer.
Clients connecting to a modded server do not need this mod.

## Behavior

- Searches inventory slots in order, including unselected hotbar slots.
- Consumes exactly one totem per rescue, restores one health point, and clears existing status effects.
- Awards the totem usage statistic, triggers the used-totem advancement criterion, and broadcasts the normal totem animation/sound event.
- Leaves held totems to Minecraft's normal death protection.
- No settings, commands, polling, mixins, or extra libraries.

The inventory fallback intentionally matches the reference mod's death-event behavior, including damage sources for which vanilla held totems cannot activate.
If a totem is held, the fallback does not consume another inventory totem.

## Build

Run `./gradlew build` (Windows: `.\gradlew.bat build`) with Java 25.
The installable jar is in `build/libs`; do not install the `-sources.jar`.
`build` includes configuration and text-rendering regression verification.

## License

AGPL-3.0; see [LICENSE](LICENSE). Copyright (c) 2026 SwordfishBE.