# 녹픽던 장신구 시드 검사기 — Android

This is a standalone seed/trinket checker, **not a Shattered Pixel Dungeon mod**.
Game logic was reverse-engineered from official Shattered Pixel Dungeon v4.0.2.

- Paste any supported dungeon seed, see four trinket options
- Mimic Tooth is highlighted
- Native Android WebView wrapper for self-contained, offline calculator
- Native Shattered Pixel Dungeon item sheet downloaded at CI build time

Build: run from this repository root:
```
./gradlew -p trinket-checker :app:assembleDebug
```
First, download `items.png` from the official v4.0.2 release to
`trinket-checker/app/src/main/assets/items.png`.
The standalone HTML includes remote image fallback.

Game code and concepts: GPL-3.0, (c) Evan Debenham
Sprites: CC BY-SA 4.0, Shattered Pixel Dungeon contributors.
Source: https://github.com/00-Evan/shattered-pixel-dungeon/tree/v4.0.2
