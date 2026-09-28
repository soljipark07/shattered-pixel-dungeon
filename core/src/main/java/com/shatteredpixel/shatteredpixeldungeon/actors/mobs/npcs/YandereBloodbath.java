package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.Splash;
import com.shatteredpixel.shatteredpixeldungeon.items.RedRibbon;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Fully-awakened growth-yandere massacre event.
 *
 * Only eligible hero time counts. Boss fights pause the counter completely.
 * The roll begins at 0.05%, rises gradually, and has a hard pity at 1,000
 * eligible turns. A floor can still only be cleared this way once.
 */
public final class YandereBloodbath {

    public static final int PITY_TURNS = 1000;
    private static final float BASE_CHANCE = 0.0005f;
    private static final float PRE_PITY_MAX_CHANCE = 0.0010f;
    private static final int BLOOD_FLASH = 0xCC780000;

    private YandereBloodbath() {}

    public static void onHeroTimeSpent(float time) {
        if (time <= 0f || Dungeon.hero == null || Dungeon.level == null || !Dungeon.hero.isAlive()) return;

        // Boss combat does not advance the pity counter at all.
        if (BossHealthBar.isAssigned()) return;

        RedRibbon ribbon = RedRibbon.findRibbonForRun();
        if (ribbon == null || !ribbon.isGrowthProfile()
                || ribbon.growthHearts() < GrowthYandereAlly.HEART_FINAL_AWAKENING) return;

        YandereAlly found = ribbon.findAlly();
        if (!(found instanceof GrowthYandereAlly) || !found.isAlive() || found.hostileToHero()) return;
        GrowthYandereAlly ally = (GrowthYandereAlly)found;
        if (!ally.isFullyAwakened()) return;

        Tracker tracker = Buff.affect(Dungeon.hero, Tracker.class);
        if (tracker == null || tracker.triggeredHere()) return;

        tracker.partialTurn += time;
        int rolls = (int)Math.floor(tracker.partialTurn);
        if (rolls <= 0) return;
        tracker.partialTurn -= rolls;

        for (int i = 0; i < rolls; i++) {
            tracker.eligibleTurns = Math.min(PITY_TURNS, tracker.eligibleTurns + 1);

            ArrayList<Mob> victims = eligibleVictims(ally);
            if (victims.isEmpty()) continue;

            boolean pity = tracker.eligibleTurns >= PITY_TURNS;
            if (!pity && Random.Float() >= chanceForTurn(tracker.eligibleTurns)) continue;

            trigger(ally, victims, tracker);
            return;
        }
    }

    private static float chanceForTurn(int turn) {
        if (turn >= PITY_TURNS) return 1f;
        if (turn <= 1) return BASE_CHANCE;
        float progress = (turn - 1f) / (PITY_TURNS - 2f);
        return BASE_CHANCE + (PRE_PITY_MAX_CHANCE - BASE_CHANCE) * progress;
    }

    private static void trigger(GrowthYandereAlly ally, ArrayList<Mob> victims, Tracker tracker) {
        tracker.markHere();
        tracker.eligibleTurns = 0;
        tracker.partialTurn = 0f;

        GameScene.flash(BLOOD_FLASH, false);
        PixelScene.shake(2.5f, 0.9f);
        GrowthYandereAlly.playHighLaugh();
        ally.yell("아하하하하하하♡ 봐, 전부 조용해졌어! 이제 너 건드릴 것들 하나도 안 남았네♡");

        for (Mob mob : victims) {
            if (mob == null || !mob.isAlive()) continue;
            if (Dungeon.level.heroFOV != null && mob.pos >= 0 && mob.pos < Dungeon.level.heroFOV.length
                    && Dungeon.level.heroFOV[mob.pos]) {
                Splash.at(mob.pos, 0xAA0000, 12);
            }

            mob.HP = 0;
            mob.die(ally);
        }

        Dungeon.observe();
        GameScene.updateFog();
    }

    private static ArrayList<Mob> eligibleVictims(GrowthYandereAlly ally) {
        ArrayList<Mob> result = new ArrayList<>();
        for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
            if (mob == null || mob == ally || !mob.isAlive() || mob.alignment != Char.Alignment.ENEMY) continue;

            if (mob.properties().contains(Char.Property.BOSS)
                    || mob.properties().contains(Char.Property.MINIBOSS)
                    || mob.properties().contains(Char.Property.BOSS_MINION)) continue;

            result.add(mob);
        }
        return result;
    }

    public static class Tracker extends Buff {

        private static final String TRIGGERED_FLOORS = "yandere_bloodbath_triggered_floors";
        private static final String ELIGIBLE_TURNS = "yandere_bloodbath_eligible_turns";
        private static final String PARTIAL_TURN = "yandere_bloodbath_partial_turn";

        private final HashSet<String> triggeredFloors = new HashSet<>();
        private int eligibleTurns = 0;
        private float partialTurn = 0f;

        {
            announced = false;
            revivePersists = true;
        }

        private static String floorKey() {
            return Dungeon.depth + ":" + Dungeon.branch;
        }

        public boolean triggeredHere() {
            return triggeredFloors.contains(floorKey());
        }

        public void markHere() {
            triggeredFloors.add(floorKey());
        }

        @Override
        public void storeInBundle(Bundle bundle) {
            super.storeInBundle(bundle);
            bundle.put(TRIGGERED_FLOORS, triggeredFloors.toArray(new String[0]));
            bundle.put(ELIGIBLE_TURNS, eligibleTurns);
            bundle.put(PARTIAL_TURN, partialTurn);
        }

        @Override
        public void restoreFromBundle(Bundle bundle) {
            super.restoreFromBundle(bundle);
            triggeredFloors.clear();
            if (bundle.contains(TRIGGERED_FLOORS)) {
                String[] floors = bundle.getStringArray(TRIGGERED_FLOORS);
                if (floors != null) {
                    for (String floor : floors) if (floor != null) triggeredFloors.add(floor);
                }
            }
            eligibleTurns = bundle.contains(ELIGIBLE_TURNS)
                    ? Math.max(0, Math.min(PITY_TURNS, bundle.getInt(ELIGIBLE_TURNS))) : 0;
            partialTurn = bundle.contains(PARTIAL_TURN)
                    ? Math.max(0f, Math.min(0.999f, bundle.getFloat(PARTIAL_TURN))) : 0f;
        }
    }
}
