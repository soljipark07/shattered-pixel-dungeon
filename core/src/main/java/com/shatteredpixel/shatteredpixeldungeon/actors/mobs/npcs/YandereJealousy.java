package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.items.RedRibbon;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.DriedRose;
import com.watabou.utils.Bundle;

import java.util.HashMap;
import java.util.Map;

/**
 * Central Stage 9 jealousy logic for the growth-profile yandere.
 *
 * Jealousy is deliberately separate from rage and permanent obsession.  The
 * state is kept on the hero in a hidden persistent buff so it survives save/
 * restore and floor changes without being tied to the current ally instance.
 */
public final class YandereJealousy {

    private static final int GENERAL_STRONG = 5;
    private static final int GENERAL_WARNING = 10;
    private static final int GENERAL_KILL = 15;

    private static final int ROSE_THREAT = 2;
    private static final int ROSE_WARNING = 4;
    private static final int ROSE_KILL = 5;

    private static final int HEART_NPC_BUMP = 5;
    private static final int HEART_ROSE_BUMP = 2;
    private static final int HEART_ENEMY_RAGE = 5;

    private YandereJealousy() {}

    /**
     * Called from Hero at the exact point where an adjacent character
     * interaction is about to happen. Returns true only when jealousy has
     * removed the target and the original interaction must therefore stop.
     */
    public static boolean onHeroInteract(Char target) {
        GrowthYandereAlly ally = currentGrowthAlly();
        if (!canReact(ally, target)) return false;

        Tracker tracker = tracker();
        if (tracker == null) return false;

        if (target instanceof DriedRose.GhostHero) {
            int count = tracker.addRose(1);
            return reactRoseInteraction(ally, target, count);
        }

        if (target instanceof Shopkeeper) {
            int count = tracker.addShop(1);
            return reactShopInteraction(ally, (Shopkeeper)target, count);
        }

        if (target instanceof NPC) {
            int count = tracker.addNpc(npcKey(target), 1);
            return reactNpcInteraction(ally, target, count);
        }

        return false;
    }

    /**
     * Called after a heart has already landed normally at a wrong target's
     * feet. Nothing here consumes or relocates the heart.
     */
    public static void onHeartMisthrown(Char target) {
        GrowthYandereAlly ally = currentGrowthAlly();
        if (!canReact(ally, target)) return;

        Tracker tracker = tracker();
        if (tracker == null) return;
        tracker.pendingMisthrow = true;

        if (target instanceof DriedRose.GhostHero) {
            int count = tracker.addRose(HEART_ROSE_BUMP);
            if (count >= ROSE_KILL) {
                ally.yell("주워. 당장. 또 걔한테 주려고 한 거면, 쟤 죽이고 직접 가져올 거야.");
                executeJealousKill(ally, target);
            } else {
                ally.yell("왜 그걸 걔한테 던져? 주워. 당장. 그건 내 거잖아.");
            }
            return;
        }

        if (target instanceof Shopkeeper) {
            int count = tracker.addShop(HEART_NPC_BUMP);
            if (count >= GENERAL_KILL) {
                ally.yell("그 하트 왜 저 사람 발밑에 있어? 됐어. 내가 저 사람부터 없애고 직접 가져올게.");
                executeJealousKill(ally, target);
            } else {
                ally.yell("그 하트 왜 저 사람 발밑에 있어? 주워. 지금. 그건 내 거야.");
            }
            return;
        }

        if (target instanceof NPC) {
            int count = tracker.addNpc(npcKey(target), HEART_NPC_BUMP);
            if (count >= GENERAL_KILL) {
                ally.yell("주워. 당장. 내가 쟤 죽이고 직접 가져올 거야. 그건 내 거니까.");
                executeJealousKill(ally, target);
            } else {
                ally.yell("왜 그걸 쟤한테 던져? 주워. 당장. 그건 내 거잖아.");
            }
            return;
        }

        if (target.alignment == Char.Alignment.ENEMY) {
            ally.addRage(HEART_ENEMY_RAGE);
            ally.yell("뭐 하는 거야? 그걸 왜 걔한테 던져. 내 거잖아.");
        }
    }

    /** Called after a heart is correctly delivered to the growth yandere. */
    public static void onHeartGivenProperly(GrowthYandereAlly ally) {
        if (ally == null || Dungeon.hero == null) return;
        Tracker tracker = Dungeon.hero.buff(Tracker.class);
        if (tracker == null || !tracker.pendingMisthrow) return;

        tracker.pendingMisthrow = false;
        ally.yell("그래. 나한테 주는 거였지? 그럼 됐어. 다음엔 헷갈리지 마.");
    }

    private static boolean canReact(GrowthYandereAlly ally, Char target) {
        return ally != null
                && target != null
                && target != ally
                && target.isAlive()
                && !ally.hostileToHero();
    }

    private static GrowthYandereAlly currentGrowthAlly() {
        if (Dungeon.hero == null || Dungeon.hero.belongings == null || Dungeon.level == null) return null;
        RedRibbon ribbon = Dungeon.hero.belongings.getItem(RedRibbon.class);
        if (ribbon == null || !ribbon.isGrowthProfile()) return null;
        YandereAlly found = ribbon.findAlly();
        return found instanceof GrowthYandereAlly && found.isAlive()
                ? (GrowthYandereAlly)found : null;
    }

    private static Tracker tracker() {
        if (Dungeon.hero == null) return null;
        return Buff.affect(Dungeon.hero, Tracker.class);
    }

    private static String npcKey(Char target) {
        return target.getClass().getName() + "@" + Dungeon.depth + ":" + Dungeon.branch + "#" + target.id();
    }

    private static boolean reactNpcInteraction(GrowthYandereAlly ally, Char target, int count) {
        if (count >= GENERAL_KILL) {
            ally.yell("말했지. 다음은 없다고. 이제 쟤 없어.");
            executeJealousKill(ally, target);
            return true;
        }
        if (count >= GENERAL_WARNING) {
            ally.yell("한 번만 더 쟤한테 가 봐. 그땐 나 진짜 안 참아.");
        } else if (count >= GENERAL_STRONG) {
            ally.yell("그만 좀 쳐다봐. 계속 그러면 쟤가 없어지는 게 더 편하겠네.");
        } else {
            ally.yell(count == 1
                    ? "왜 또 쟤랑 얘기해? 나 여기 있는데."
                    : "또 쟤야? 나 보고도 굳이 쟤한테 가야 해?");
        }
        return false;
    }

    private static boolean reactShopInteraction(GrowthYandereAlly ally, Shopkeeper target, int count) {
        if (count >= GENERAL_KILL) {
            ally.yell("됐어. 이제 가게도, 저 사람도 필요 없어.");
            executeJealousKill(ally, target);
            return true;
        }
        if (count >= GENERAL_WARNING) {
            ally.yell("한 번만 더 저 사람한테 가 봐. 그땐 가게째 없애버릴 거야.");
        } else if (count >= GENERAL_STRONG) {
            ally.yell("필요한 것만 사고 와. 계속 저 사람이랑 붙어 있으면 나 진짜 화나.");
        } else {
            ally.yell(count == 1
                    ? "물건만 사고 와. 왜 자꾸 저 사람이랑 말 섞어?"
                    : "또 저 사람이야? 빨리 끝내고 나한테 와.");
        }
        return false;
    }

    private static boolean reactRoseInteraction(GrowthYandereAlly ally, Char target, int count) {
        if (count >= ROSE_KILL) {
            ally.yell("됐어. 이제 걔 없어. 나만 보면 돼.");
            executeJealousKill(ally, target);
            return true;
        }
        if (count >= ROSE_WARNING) {
            ally.yell("다음에도 걔부터 찾으면, 내가 직접 없앨 거야.");
        } else if (count >= ROSE_THREAT) {
            ally.yell("또 걔야? 하지 마. 진짜로. 걔 보기 싫어.");
        } else {
            ally.yell("걔랑 왜 그렇게 붙어 있어? 나 싫어.");
        }
        return false;
    }

    private static void executeJealousKill(GrowthYandereAlly ally, Char target) {
        if (ally == null || target == null || !target.isAlive()) return;

        if (ally.sprite != null) {
            ally.sprite.turnTo(ally.pos, target.pos);
            ally.sprite.showAlert();
        }

        // Shops have special cleanup semantics; fleeing removes the shop and
        // its sale state safely. Other NPCs, including the Rose ghost, are
        // killed directly because several quest NPCs intentionally ignore damage().
        if (target instanceof Shopkeeper) {
            ((Shopkeeper)target).flee();
        } else {
            target.die(ally);
        }
    }

    public static class Tracker extends Buff {

        private static final String NPC_KEYS = "jealous_npc_keys";
        private static final String NPC_COUNTS = "jealous_npc_counts";
        private static final String ROSE_COUNT = "jealous_rose_count";
        private static final String SHOP_COUNT = "jealous_shop_count";
        private static final String SHOP_DEPTH = "jealous_shop_depth";
        private static final String SHOP_BRANCH = "jealous_shop_branch";
        private static final String PENDING_MISTHROW = "jealous_pending_misthrow";

        private final HashMap<String, Integer> npcCounts = new HashMap<>();
        private int roseCount = 0;
        private int shopCount = 0;
        private int shopDepth = Integer.MIN_VALUE;
        private int shopBranch = Integer.MIN_VALUE;
        private boolean pendingMisthrow = false;

        {
            announced = false;
            revivePersists = true;
        }

        private int addNpc(String key, int amount) {
            int next = Math.max(0, npcCounts.containsKey(key) ? npcCounts.get(key) : 0) + amount;
            npcCounts.put(key, next);
            return next;
        }

        private int addRose(int amount) {
            roseCount = Math.max(0, roseCount + amount);
            return roseCount;
        }

        private int addShop(int amount) {
            if (shopDepth != Dungeon.depth || shopBranch != Dungeon.branch) {
                shopDepth = Dungeon.depth;
                shopBranch = Dungeon.branch;
                shopCount = 0;
            }
            shopCount = Math.max(0, shopCount + amount);
            return shopCount;
        }

        @Override
        public void storeInBundle(Bundle bundle) {
            super.storeInBundle(bundle);

            String[] keys = new String[npcCounts.size()];
            int[] counts = new int[npcCounts.size()];
            int i = 0;
            for (Map.Entry<String, Integer> entry : npcCounts.entrySet()) {
                keys[i] = entry.getKey();
                counts[i] = entry.getValue();
                i++;
            }
            bundle.put(NPC_KEYS, keys);
            bundle.put(NPC_COUNTS, counts);
            bundle.put(ROSE_COUNT, roseCount);
            bundle.put(SHOP_COUNT, shopCount);
            bundle.put(SHOP_DEPTH, shopDepth);
            bundle.put(SHOP_BRANCH, shopBranch);
            bundle.put(PENDING_MISTHROW, pendingMisthrow);
        }

        @Override
        public void restoreFromBundle(Bundle bundle) {
            super.restoreFromBundle(bundle);
            npcCounts.clear();

            if (bundle.contains(NPC_KEYS) && bundle.contains(NPC_COUNTS)) {
                String[] keys = bundle.getStringArray(NPC_KEYS);
                int[] counts = bundle.getIntArray(NPC_COUNTS);
                if (keys != null && counts != null) {
                    for (int i = 0; i < Math.min(keys.length, counts.length); i++) {
                        npcCounts.put(keys[i], counts[i]);
                    }
                }
            }
            roseCount = bundle.contains(ROSE_COUNT) ? bundle.getInt(ROSE_COUNT) : 0;
            shopCount = bundle.contains(SHOP_COUNT) ? bundle.getInt(SHOP_COUNT) : 0;
            shopDepth = bundle.contains(SHOP_DEPTH) ? bundle.getInt(SHOP_DEPTH) : Integer.MIN_VALUE;
            shopBranch = bundle.contains(SHOP_BRANCH) ? bundle.getInt(SHOP_BRANCH) : Integer.MIN_VALUE;
            pendingMisthrow = bundle.contains(PENDING_MISTHROW) && bundle.getBoolean(PENDING_MISTHROW);
        }
    }
}
