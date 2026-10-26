# Starting Strength Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Start new games with ten strength potions and remove their natural heaps without changing the remaining seed contents.

**Architecture:** Extend the existing post-generation cleanup and delayed starting grant in ImpTravel. Keep the original generation and counters, and compare against the independent official and analyzer fixtures with only the two relocated item types excluded.

**Tech Stack:** Java 11, libGDX headless regression, Node analyzer comparison, Android Gradle/GitHub Actions.

**Spec:** docs/superpowers/specs/2026-10-27-starting-strength-design.md

## Global Constraints

- Official v4.0.2 base remains unchanged outside this feature.
- New games receive ten identified strength potions, fifteen upgrade scrolls and the existing beacon.
- Grants occur after prefix generation; normal placement and drop counters remain intact.
- Remove only natural strength potions and upgrade scrolls from generated heaps.
- Preserve other seed contents and existing travel behavior.

## Review Focus

- Starting grants must not influence prefix shop selection.
- Saved prefix floors and future floors both need cleanup.
- All six heroes and challenge combinations must receive the same ten potions.
- Save/reload must retain the grant without duplicating it.
- APK delivery uses a separate package to preserve the previous APK/saves across debug signing changes.

### Task 1: Relocate strength potions and deliver the verified APK

**Files:**
- Modify: core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/ImpTravel.java
- Modify: core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/levels/Level.java
- Test: tools/imp-travel/ImpTravelRegression.java
- Modify: tools/imp-travel/compare-analyzer.mjs, tools/imp-travel/README.md
- Modify: build.gradle, .github/workflows/build-imp-travel.yml

**Interfaces:**
- Consumes: ImpTravel.prepare(Level), original Level.create() post-generation hook.
- Produces: renamed removeNaturalStartingItems(Level), ten identified potions in starting inventory.

- [x] Extend the real starting regression: no early potions, ten after preparation, identified, retained after save/load. Observe the missing grant fail.
- [x] Assert no natural strength potions on saved and later floors; assert the original strength-drop counter reaches ten through floor24 and matches upstream on later floors.
- [x] Add a legacy-save RED→GREEN regression and persisted relocation flag so older runs retain natural strength potions.
- [x] Extend cleanup and delayed grant; update messages/docs and bump custom version code.
- [x] Run the full official-engine suite and all 26 existing direct analyzer scenarios, excluding only relocated upgrade scrolls and strength potions.
- [x] Request one read-only code review and resolve important findings.
- [ ] Build Android APK through the feature branch workflow, inspect successful checks/signature/package, and deliver the actual APK.
