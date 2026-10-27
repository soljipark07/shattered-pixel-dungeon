# Starting Experience Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Start new games with ten identified experience potions and remove natural heap XP potions without changing the other seeded contents.

**Architecture:** Extend the existing delayed starting grant and post-generation cleanup, guarded by a persisted beacon flag. Compare unrelocated controls with the independent official/analyzer references, then compare the new variant with those controls in the same JVM.

**Tech Stack:** Java11/libGDX, headless Java17 oracle, Node, Android Gradle/GitHub Actions.

**Spec:** docs/superpowers/specs/2026-10-28-starting-experience-design.md

## Global Constraints

- Keep official v4.0.2 algorithms and the saved-prefix endpoint at the actual Imp.
- Starting inventory: fifteen upgrade scrolls, ten strength potions, ten experience potions, reusable beacon.
- Grant after prefix generation; clean heaps only after all normal placement/RNG.
- Older saves without the experience flag retain natural XP and get no extra grant.
- Separate package com.solji.imptravel402.experience.indev; version924 /4.0.2-EXP-INDEV.

## Review Focus

- Gift collection and identification survive real save/load without duplicates.
- Mixed heaps keep all other items; empty XP-only heaps disappear.
- Original potion decks/counters, mobs, terrain and rewards remain unchanged.
- Runtime-unknown laboratory/vault slots must not hide deletion of other consumables.
- Previous APKs/saves remain installed; physical-device execution is not claimed.

### Task 1: Add XP relocation and deliver verified APK

**Files:** ImpTravel.java, items/ImpBeacon.java, tools/imp-travel/{ImpTravelRegression.java,compare-analyzer.mjs,oracle/AnalyzerExportLauncher.java,oracle/AnalyzerOracle.java,README.md,verify.py}, build.gradle, .github/workflows/build-imp-travel.yml, English/Korean beacon descriptions.

**Interfaces:** Preserve ImpTravel.prepare(Level)/removeNaturalStartingItems(Level); add ImpBeacon.relocatesExperience() with bundle persistence. Test oracle provides control/variant output pairs in the same JVM.

- [x] Add starting-ten, save/reload, legacy-save and heap-cleanup regressions; observe missing grant fail.
- [x] Implement guarded cleanup and delayed identified grant; update version, descriptions and artifact name.
- [x] Validate 624 official-reference floors using controls and variants; compare all26 analyzer scenarios with exact same-JVM XP-only deltas.
- [x] Obtain read-only review and resolve important findings with RED/GREEN checks.
- [x] Build/sign/inspect APK in CI, retain source/evidence, save and deliver the actual file.
