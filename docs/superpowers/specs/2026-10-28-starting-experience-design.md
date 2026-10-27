# Starting experience potions

Extend the existing official-v4.0.2 Imp travel/starting-strength variant with ten
identified experience potions at new-game start. Ten is the implementation
default stated to the user after the quantity question returned no answer;
experience potion generation has no fixed per-run total. Keep the existing
fifteen upgrade scrolls, ten strength potions and reusable beacon.

Grant after the original saved prefix through the Imp, preserving shop selection.
Remove PotionOfExperience from heaps only after original generation/placement;
keep generator decks, counters and all other item types. Enemy drops, alchemy
and other later runtime sources retain their original behavior.

Persist a separate experience-relocation flag in the beacon. Missing flags from
older saves mean false, so an old run that received no grant keeps natural XP
potions. The saved-prefix extent and accepted generation-time limitations stay
unchanged. Deliver a separate experience APK to preserve previous installations.

Secret-laboratory and vault consumable subtypes can be runtime-dependent. Verify
a control without XP relocation against the existing official/analyzer fixtures,
then require the variant to match that same-process control with only heap XP
removed. Do not permit arbitrary missing unknown-potion slots in the comparator.
