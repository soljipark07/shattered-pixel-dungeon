# Progress for 2026-10-26-imp-travel.md

- Task 1 complete: unmodified v4.0.2 compiled; 624 floor fixtures captured. Starting inventory assertion failed at 0 vs 15 before implementation.
- Task 2: generation comparison passed for 624 floors / 24 runs after documented normalization of original nondeterminism.
- Ruling: generate only through the actual Imp, preserving later floor generation at normal encounter time; prefix generation freezes all generation-time trinket/artifact/talent effects, including petals, early shop sandbags and cached rations. This inherent upfront-generation limitation is disclosed in the item, README and delivery; it does not reproduce every possible future playthrough's added drops.
- Gift grant moved after preparation because the real ShopRoom bag choice inspects inventory; still occurs before the first player action.
- Review findings fixed with RED to GREEN tests: real saved-floor visit progression, leaked transient no-killing flag, and occupied arrival revalidation. Preserve per-floor boss-remains qualification as well.
- Final review: independent read-only reviewer inspected gameplay integration, save/load, quest inventory, resurrection, and package isolation. No other concrete functional issues. Android build and device UI behavior were not reviewed.
- CI JDK 21 exposed original SecretLaboratoryRoom/SecretLibraryRoom Class-keyed HashMap ordering: subtype choices differed between JVMs while every other recorded cell/item matched. Compare their quantities/positions/state, not the original non-seed-defined subtypes; leave production generation unchanged. Initialization-order matching was insufficient and was removed.
- Task 3 pending: final suite, GitHub branch and Android build/artifact.
