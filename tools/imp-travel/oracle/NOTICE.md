# Oracle provenance

`AnalyzerOracle.java` is adapted from `SoljiPlanOracle.java` in the user's provided
`engine-source.tar.gz`, matching the HTML's WASM SHA-256
`6cc018027737352749866ad8a0b5684b234f7c24b0065cad4c68378aff09192d`.
That source adapts the GPL-3.0-or-later ParityOracle from
akhial/shpd-seed-seeker commit e81581ec545730aed99eb19955700695d422e482.
Original copyright/license notices are retained. Repository COPYING applies.

This adapter initializes the actual modified APK core and saves/reloads the
pre-generated Imp prefix. It captures quest rewards on their NPC's real floor,
exports actual trinket draws without retaining deck changes, and verifies
artifact internal levels before translating the analyzer's transfer units.
It is test support, not application runtime code.
