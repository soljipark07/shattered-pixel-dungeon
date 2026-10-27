# Progress for 2026-10-28-starting-experience.md

- Default quantity10 stated to user; quantity question returned no answer. Experience potions have variable natural counts, unlike the ten guaranteed strength potions.
- Starting-XP test observed RED (0 vs10); GREEN includes identification, real save/reload and no duplicate grant.
- Cleanup test observed RED, then GREEN: remove regular XP from mixed heaps and empty XP-only heaps, preserve healing/exotic potions and terrain.
- Separate experience-relocated beacon flag defaults false on old saves. New-run grant remains after the Imp prefix, keeping existing generation extent and RNG order.
- Full official-control/variant suite passed: 24 runs through26 (624 control and624 variant floors), unchanged maps/other loot/mobs/rewards/decks and strength counters. All seven behavior modes passed.
- Direct analyzer audit passed all26 scenarios/598 maps/2423 equipment-reward candidates; 10882 remaining deterministic heap items,106 regular-XP potions removed.
- Audit controls retain XP and match original analyzer uncertainty rules. Paired deltas normalize only existing shop-bag subtype/image ties; vanilla unseeded Collections.shuffle is reset to Random0 and mutated vault solve arrays reset before each pass. Production RNG remains untouched; every other heap item/count/cell/type and all terrain/mobs/decks/rewards must match. No arbitrary missing unknown-potion slots are allowed.
- Independent read-only review found no production issue. Added its two coverage suggestions: previous strength=true / missing-XP save format, and variant/control potion-scroll-ring appearance mapping equality.
- Android build/signature and file delivery complete: source 2984e4a02e9fdfa8e3fe25d320b1543a0dcb2f4a, workflow run 40552383017, artifact 12756091314. APK 35137239 bytes; SHA256 c578fb38ed6cf03c89e86602298bdcd341f49e83c5c0db48240961678baf8044. Local APK/ZIP/certificate hashes independently matched CI. Physical-device execution remains untested. CI repeated all26 scenarios with104 XP removed (local106; runtime-dependent subtype draws),10882 deterministic remaining heap items and2423 equipment/reward candidates.
- Delivery package com.solji.imptravel402.experience.indev, label Solji Imp 경험물약 4.0.2, version924 /4.0.2-EXP-INDEV. Previous apps/saves remain separate.
