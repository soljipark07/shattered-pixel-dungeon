# Starting strength potions

The existing official-v4.0.2 Imp travel variant must also give the hero the ten
normal strength potions at new-game start, as identified inventory items. The
existing beacon and fifteen upgrade scrolls stay available. Normal item placement
and RNG run first; remove strength potions from generated heaps only after each
level has finished generation. Keep the original strength-drop counters.

Use the already authorized post-generation approach. Suppressing the original
spawn would change the seed's other items and does not meet the requirement.
Grant the ten potions after the saved prefix through the real Imp is complete,
alongside the upgrade scrolls, to preserve prefix shop selection.

This applies to new games. Existing saves do not receive a second starting grant
and must retain their remaining natural strength potions. Persist the relocation
mode in the beacon, treating missing flags from older saves as false.
Pre-generated floors keep the existing generation-time trinket/artifact/talent
limitations. Other consumables, equipment, quests, terrain and RNG must remain
unchanged under the existing independent engine/analyzer checks.

Delivery uses package com.solji.imptravel402.strength.indev and label
Solji Imp 힘물약 4.0.2, so the previous APK and saves can remain installed.
