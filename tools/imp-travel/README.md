# Solji Imp Travel 4.0.2

Based exactly on official v4.0.2 (57a4e06a4caf162446d1c28caa7983f0493fecf0).
Start a new game to receive an identified, reusable Imp travel beacon and 15 upgrade scrolls.
Its two actions visit the actual generated Imp room and return to floor 1. Complete the vault
and use its normal escape crystal before using the beacon. Locked floors retain their normal restriction.

The main floors from 1 through the Imp (17–19) are generated in original order at new-game start.
All original upgrade spawn/placement RNG runs; only afterwards are natural upgrade scrolls
removed. The gift is added after that prefix is prepared, so it cannot influence those shops.
The prefix is saved and loaded for travel, with destination occupancy rechecked on every use.

## Generation-time effects

Pre-generating floors fixes their generation-time hero state at the start of the run.
This includes trinkets, Dried Rose petals, Timekeeper's Hourglass sandbags in the early shops,
and Cached Rations. Acquiring these effects later cannot add their normal generation-time
extras to this already generated prefix. Later ungenerated floors use the normal current state.
This variant preserves the initial seeded layout and loot, rather than simulating every
possible later playthrough's inventory-dependent generation.

Shop bag selection is also inventory-dependent in the original game, with Java HashMap
identity order breaking equal-saving ties. The engine comparison therefore normalizes bag
subtypes, throwing-weapon SecureRandom set IDs, and actor runtime IDs. The original secret
laboratory and secret library also draw potion/scroll subtypes through Class-keyed HashMaps;
their JVM-dependent iteration order is not fixed by the seed. Those two rooms' subtype
choices are normalized while their counts, positions and item state remain checked. Tutorial/journal
progress is equalized in both builds to avoid upstream unseeded guide-page placement.

## Verification

`python3 tools/imp-travel/verify.py reference --source-root PATH_TO_UNMODIFIED_V402`
captures independent upstream fixtures. `python3 tools/imp-travel/verify.py suite` compares
24 runs (12 seeds, normal and Forbidden Runes, all six classes) through floor 26, including
map cells, loot types/levels/curses/quantities/positions, enemies, Imp rewards and item-deck
state. It also checks starting inventory, actual saved-floor visits, no free exploration
badge from pre-generation, occupied-destination handling, and travel-item serialization.

The APK uses its own package, `com.solji.imptravel402.indev`, and does not replace official saves.
Android compilation does not substitute for device playtesting.
