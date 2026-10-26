package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.ImpBeacon;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.watabou.utils.Bundle;

import java.io.IOException;
import java.util.Iterator;

/** Additional start items and travel preparation; never replaces original level generation. */
public final class ImpTravel {
    public static final int IMP_ARRIVAL = -3;
    public static final int FIRST_ARRIVAL = -4;
    private ImpTravel() {}

    public static void grantStartingItems() {
        new ImpBeacon().collect();
    }

    /** Called only after all original generation and placement RNG has completed. */
    public static void removeNaturalStartingItems(Level level) {
        ImpBeacon beacon = Dungeon.hero == null ? null : Dungeon.hero.belongings.getItem(ImpBeacon.class);
        boolean removeStrength = beacon != null && beacon.relocatesStrength();
        for (Heap heap : level.heaps.valueList()) {
            Iterator<Item> items = heap.items.iterator();
            while (items.hasNext()) {
                Item item = items.next();
                if (item instanceof ScrollOfUpgrade || (removeStrength && item instanceof PotionOfStrength)) items.remove();
            }
            if (heap.items.isEmpty()) level.heaps.remove(heap.pos);
        }
    }

    /** Save the normal prefix through the real Imp, keeping the hero on the original first floor. */
    public static void prepare(Level first) throws IOException {
        ImpBeacon beacon = Dungeon.hero.belongings.getItem(ImpBeacon.class);
        if (beacon == null) throw new IOException("Starting Imp beacon is missing");
        Bundle statistics = new Bundle();
        Statistics.storeInBundle(statistics);
        boolean completedWithNoKilling = Statistics.completedWithNoKilling;
        int originalDepth = Dungeon.depth;
        int originalBranch = Dungeon.branch;
        try {
            Dungeon.saveLevel(GamesInProgress.curSlot);
            for (int depth = 2; depth <= 19; depth++) {
                Dungeon.depth = depth;
                Dungeon.branch = 0;
                Dungeon.level = Dungeon.newLevel();
                if (Statistics.qualifiedForBossRemainsBadge) beacon.recordRemainsFloor(depth);
                Dungeon.saveLevel(GamesInProgress.curSlot);
                for (Mob mob : Dungeon.level.mobs) {
                    if (mob instanceof Imp) {
                        beacon.setDestination(depth, landingCell(Dungeon.level, mob.pos));
                        // Shop bag selection consults inventory. Grant these only after the saved
                        // prefix is generated, still before the player sees or acts on floor 1.
                        new ScrollOfUpgrade().identify(false).quantity(15).collect();
                        new PotionOfStrength().identify(false).quantity(10).collect();
                        return;
                    }
                }
            }
            throw new IOException("No Imp found on the normally generated floors 17-19");
        } finally {
            Dungeon.depth = originalDepth;
            Dungeon.branch = originalBranch;
            Dungeon.level = first;
            Statistics.restoreFromBundle(statistics);
            // This transient flag is intentionally absent from the original Statistics bundle.
            Statistics.completedWithNoKilling = completedWithNoKilling;
        }
    }

    /** Generated floors become visited only when the hero actually arrives there. */
    public static void onVisit() {
        if (Dungeon.branch != 0) return;
        ImpBeacon beacon = Dungeon.hero.belongings.getItem(ImpBeacon.class);
        if (beacon == null || !beacon.visitFloor(Dungeon.depth)) return;
        // Later floors still go through Dungeon.newLevel's original visit bookkeeping.
        if (!beacon.isPreparedFloor(Dungeon.depth)) return;
        if (Dungeon.depth > Statistics.deepestFloor) {
            Statistics.deepestFloor = Dungeon.depth;
            Statistics.completedWithNoKilling = Statistics.qualifiedForNoKilling;
        }
        Statistics.qualifiedForNoKilling = !Dungeon.bossLevel();
        Statistics.qualifiedForBossRemainsBadge = beacon.remainsOnFloor(Dungeon.depth);
        Statistics.qualifiedForBossChallengeBadge = false;
    }

    private static int landingCell(Level level, int imp) throws IOException {
        int best = -1, distance = Integer.MAX_VALUE;
        for (int cell = 0; cell < level.length(); cell++) {
            if (!level.passable[cell] || level.solid[cell] || level.avoid[cell]
                    || level.traps.get(cell) != null || level.findMob(cell) != null) continue;
            int d = Math.max(Math.abs(cell % level.width() - imp % level.width()),
                    Math.abs(cell / level.width() - imp / level.width()));
            if (d < distance) {
                best = cell;
                distance = d;
            }
        }
        if (best >= 0) return best;
        throw new IOException("No safe arrival cell on the destination floor");
    }

    /** Resolve only our sentinels, after loading, so a later ward or enemy cannot occupy arrival. */
    public static int resolveArrival(Level level, int requested) throws IOException {
        if (requested == FIRST_ARRIVAL) return landingCell(level, level.entrance());
        if (requested != IMP_ARRIVAL) return requested;
        for (Mob mob : level.mobs) {
            if (mob instanceof Imp) return landingCell(level, mob.pos);
        }
        ImpBeacon beacon = Dungeon.hero.belongings.getItem(ImpBeacon.class);
        int anchor = beacon == null ? level.entrance() : beacon.destinationCell();
        return landingCell(level, anchor >= 0 && anchor < level.length() ? anchor : level.entrance());
    }
}
