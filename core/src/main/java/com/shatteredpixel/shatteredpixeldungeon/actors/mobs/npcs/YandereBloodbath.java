package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.Splash;
import com.shatteredpixel.shatteredpixeldungeon.items.RedRibbon;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Rare 48-heart event. Every real hero step gets a tiny chance to make the
 * fully-awakened growth yandere erase the ordinary hostile population of the
 * current floor. Boss combat is deliberately excluded.
 */
public final class YandereBloodbath {

    // 0.05% per actual travelling step: about one roll success per 2,000 steps.
    private static final float STEP_CHANCE = 0.0005f;
    private static final int BLOOD_FLASH = 0xCC780000;

    private YandereBloodbath() {}

    public static void onHeroStep() {
        if (Dungeon.hero == null || Dungeon.level == null) return;

        // No bloodbath rolls at all while a boss encounter is actively assigned.
        if (BossHealthBar.isAssigned()) return;

        RedRibbon ribbon = RedRibbon.findRibbonForRun();
        if (ribbon == null || !ribbon.isGrowthProfile() || ribbon.growthHearts() < GrowthYandereAlly.HEART_FINAL_AWAKENING) return;

        YandereAlly found = ribbon.findAlly();
        if (!(found instanceof GrowthYandereAlly) || !found.isAlive() || found.hostileToHero()) return;
        GrowthYandereAlly ally = (GrowthYandereAlly)found;
        if (!ally.isFullyAwakened()) return;

        Tracker tracker = Buff.affect(Dungeon.hero, Tracker.class);
        if (tracker == null || tracker.triggeredHere()) return;

        ArrayList<Mob> victims = eligibleVictims(ally);
        if (victims.isEmpty() || Random.Float() >= STEP_CHANCE) return;

        tracker.markHere();

        // The whole scene washes blood-red for roughly a second while the
        // high-obsession laugh and massacre happen. This is intentionally a
        // screen effect rather than permanent terrain recolouring.
        GameScene.flash(BLOOD_FLASH, false);
        PixelScene.shake(2.5f, 0.9f);
        Sample.INSTANCE.play(Assets.Sounds.YANDERE_LAUGH_HIGH);
        ally.yell("아하하하하하하♡ 봐, 전부 조용해졌어! 이제 너 건드릴 것들 하나도 안 남았네♡");

        for (Mob mob : victims) {
            if (mob == null || !mob.isAlive()) continue;
            if (Dungeon.level.heroFOV != null && mob.pos >= 0 && mob.pos < Dungeon.level.heroFOV.length
                    && Dungeon.level.heroFOV[mob.pos]) {
                Splash.at(mob.pos, 0xAA0000, 12);
            }

            // Use each mob's normal death path so loot/EXP/death hooks still run,
            // but set HP to zero first so damage caps or invulnerability don't
            // turn the event into a partial clear.
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

            // Bosses, minibosses, and boss-linked combat pieces are excluded even
            // outside the active-boss-bar guard. This also keeps progression-
            // critical set pieces from being deleted by the rare event.
            if (mob.properties().contains(Char.Property.BOSS)
                    || mob.properties().contains(Char.Property.MINIBOSS)
                    || mob.properties().contains(Char.Property.BOSS_MINION)) continue;

            result.add(mob);
        }
        return result;
    }

    public static class Tracker extends Buff {

        private static final String TRIGGERED_FLOORS = "yandere_bloodbath_triggered_floors";
        private final HashSet<String> triggeredFloors = new HashSet<>();

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
        }
    }
}
