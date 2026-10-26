# Imp travel for official v4.0.2

Base: upstream v4.0.2, commit 57a4e06a4caf162446d1c28caa7983f0493fecf0.
The user selected ordinary generation followed by teleportation, rather than skipping directly to a freshly generated city floor.

Generate main floors in their normal order, stopping at the actual Imp floor (17–19), and save them. Keep floor 1 as the starting floor. A reusable starting item travels to an unoccupied passable tile beside that Imp and returns to floor 1. Freeze the generated prefix: later trinket changes cannot retroactively modify it. Leave later floors and quest subfloors to the original game.

Give every new hero 15 upgrade scrolls. Retain all original upgrade decisions, counters, placement and RNG calls; remove upgrade scrolls from floor heaps only after generation completes. If this changes other generation results, retain natural scrolls as the user's fallback rather than silently accepting different loot.

Keep quest mechanics intact, including vault inventory storage. Do not bypass a sealed floor or leave a quest branch using the travel item; use the original exit crystal first. Preserve the travel item through ordinary save/load. Build as a separate Android package so it can coexist with the official app.

Validate against independently compiled, unmodified upstream snapshots: map, floor loot, enemies, Imp position and rewards, and generator state. Exercise real generation, save/load and the new item. APK compilation is required; device playtesting is reported separately.
