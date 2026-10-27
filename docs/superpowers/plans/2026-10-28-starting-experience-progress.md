# Progress for 2026-10-28-starting-experience.md

- Default quantity10 stated to user; quantity question returned no answer. Experience potions have variable natural counts, unlike the ten guaranteed strength potions.
- Starting-XP test observed RED (0 vs10); GREEN includes identification, real save/reload and no duplicate grant.
- Cleanup test observed RED, then GREEN: remove regular XP from mixed heaps and empty XP-only heaps, preserve healing/exotic potions and terrain.
- Separate experience-relocated beacon flag defaults false on old saves. New-run grant remains after the Imp prefix, keeping existing generation extent and RNG order.
- Full official-control/variant suite passed: 24 runs through26 (624 control and624 variant floors), unchanged maps/other loot/mobs/rewards/decks and strength counters. All seven behavior modes passed.
- Direct analyzer audit passed all26 scenarios/598 maps/2423 equipment-reward candidates; 10882 remaining deterministic heap items,106 regular-XP potions removed.
- Audit controls retain XP and match original analyzer uncertainty rules. Paired deltas normalize only existing shop-bag subtype/image ties; vanilla unseeded Collections.shuffle is reset to Random0 and mutated vault solve arrays reset before each pass. Production RNG remains untouched; every other heap item/count/cell/type and all terrain/mobs/decks/rewards must match. No arbitrary missing unknown-potion slots are allowed.
- Independent read-only review found no production issue. Added its two coverage suggestions: previous strength=true / missing-XP save format, and variant/control potion-scroll-ring appearance mapping equality.
- Android build/signature and file delivery pending.
- Delivery package com.solji.imptravel402.experience.indev, label Solji Imp 경험물약 4.0.2, version924 /4.0.2-EXP-INDEV. Previous apps/saves remain separate.
