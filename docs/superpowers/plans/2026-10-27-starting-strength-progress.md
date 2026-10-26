# Progress for 2026-10-27-starting-strength.md

- Baseline: existing starting-inventory regression passed before changes.
- Starting-potion regression observed RED at 0 vs required10; after delayed grant it passed, including identification and actual save/reload.
- Read-only review found legacy-save loss of future natural potions. Regression observed RED (0 natural vs10) and GREEN after a persisted beacon relocation mode; absent flags restore false.
- Full official-source fixtures and suite passed: 24 runs, 624 floors, all six heroes, normal/Forbidden Runes, saved-prefix and future floors, original drop counters, remaining loot/maps/mobs/rewards/decks, visits/badges/landing/save state.
- Direct existing-analyzer comparison passed: 26 scenarios, 598 maps, 2423 equipment/reward candidates, 10960 deterministic heap items, 389 declared unknown subtype slots. Only natural upgrade scrolls and strength potions are relocated. Existing precisely recorded Barren Land terrain exception remains unchanged.
- Counter check correction: ten strength potions through floor24; floor26 inherits base scheduling but LastLevel.createItems only places the Amulet. Compare exact upstream counter at Imp and every future floor instead of asserting a constant at26.
- Delivery decision: use separate package com.solji.imptravel402.strength.indev, label Solji Imp 힘물약 4.0.2, version923 /4.0.2-STR-INDEV. This preserves the previous app/saves even if ephemeral debug keys differ; saves are separate.
- Android build/signature and artifact delivery pending.
