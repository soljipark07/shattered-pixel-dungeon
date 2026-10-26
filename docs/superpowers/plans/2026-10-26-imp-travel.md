# Imp travel implementation plan

> Execution: inline, with a final code review.

**Goal:** Official 4.0.2 plus a reusable Imp/floor-1 travel item and 15 starting upgrade scrolls.
**Architecture:** Keep level generation intact; postprocess floor heaps and generate/save the prefix through the Imp in original order at new-game initialization.
**Tech stack:** Java 11, libGDX 1.14, original Android Gradle build.
**Spec:** ../specs/2026-10-26-imp-travel-design.md

## Global constraints
- Original tag and RNG calls remain unchanged.
- Only post-generation upgrade removal may change generated loot.
- Future trinkets do not modify floors already generated.
- No edits to existing shared branches or original game saves.

## Review focus
- Starting item and scrolls survive save/load without duplicate grants.
- All seeds reach a real Imp and a safe neighboring cell.
- Generating the prefix preserves other loot, maps and rewards.
- Sealed floors and inventory-stripping quest branches cannot be escaped incorrectly.
- Saved floor 1 and the Imp floor are loaded, never regenerated during travel.

## Tasks
1. Add a headless regression runner. Compile upstream and capture seeded fixtures before edits; run a starting-inventory assertion and observe its failure.
2. Add `items/ImpBeacon.java` and `ImpTravel.java`, hook initialization and post-generation cleanup, and add English/Korean strings. Compare generated floors and rewards against upstream; exercise prefix save/load, inventory, and removal.
3. Give the variant its own Android package/name; add an Actions build with regression verification and APK output. Review the diff, build, and deliver the APK with the verified limitations.
