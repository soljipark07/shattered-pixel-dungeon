package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.GrowthYandereAlly;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.YandereAlly;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;

public class RedRibbon extends Item {

    private static final String AC_MANAGE = "LAB3_YANDERE_MANAGE";

    public static final int PROFILE_CHEAT = 0;
    public static final int PROFILE_GROWTH = 1;
    public static final int MAX_GROWTH_HEARTS = 48;
    public static final int GROWTH_REVIVE_HEART_COST = 2;

    private static final float ABANDONMENT_DAMAGE_MULTIPLIER = 0.80f;
    private static final float ABANDONMENT_ATTACK_SPEED_MULTIPLIER = 0.80f;
    private static final float ABANDONMENT_MOVE_SPEED_MULTIPLIER = 0.80f;
    private static final float ABANDONMENT_STAGE_1 = 30f;
    private static final float ABANDONMENT_STAGE_2 = 80f;
    private static final float ABANDONMENT_STAGE_3 = 150f;
    private static final float ABANDONMENT_STAGE_4 = 220f;
    
    private int allyID = 0;
    private boolean summoned = false;
    private int profile = PROFILE_CHEAT;
    private boolean profileLocked = false;
    private int growthHearts = 0;
    private boolean growthRevivalPending = false;

    private boolean abandonmentActive = false;
    private float abandonmentStartClock = -1f;
    private float abandonmentNextChatterClock = -1f;
    private int abandonmentStage = -1;
    
    private int lastHeartWarningDepth = Integer.MIN_VALUE;
    private int lastHeartWarningBranch = Integer.MIN_VALUE;

    private int savedMode = YandereAlly.MODE_GUARD;
    private int savedRage = 0;
    private int savedEffectiveHearts = 0;
    private int savedTotalHearts = 0;
    private boolean savedHostile = false;
    private int savedGrowthHP = -1;

    {
        image = ItemSpriteSheet.RED_RIBBON;
        unique = true;
        keptThoughLostInvent = true;
        defaultAction = AC_MANAGE;
    }

    @Override
    public String name() { return "붉은 리본"; }

    @Override
    public String desc() {
        return "얀데레를 소환하고 명령하는 전용 도구다.\n\n"
                + "첫 소환 전 치트형과 성장형 중 하나를 고를 수 있으며, 한 번 소환한 뒤에는 해당 유형으로 고정된다. "
                + "성장형은 얀데레에게 직접 건넨 하트를 최대 48개까지 성장 하트로 기록한다.\n\n"
                + "하트는 리본 메뉴에서 공짜로 먹이는 방식이 아니라 던전 바닥에서 직접 주워 "
                + "얀데레에게 던져야 한다.";
    }

    @Override public boolean isIdentified() { return true; }
    @Override public boolean isUpgradable() { return false; }
    @Override public int value() { return 0; }

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = new ArrayList<>();
        actions.add(AC_MANAGE);
        actions.add(AC_DROP);
        return actions;
    }

    @Override
    public String actionName(String action, Hero hero) {
        if (AC_MANAGE.equals(action)) return "얀데레 관리";
        return super.actionName(action, hero);
    }

    @Override
    public void execute(Hero hero, String action) {
        super.execute(hero, action);
        if (AC_MANAGE.equals(action)) showMainMenu(hero);
    }

    @Override
    public void doDrop(Hero hero) {
        boolean bonded = isBonded();
        YandereAlly ally = findAlly();
        if (ally != null) captureFrom(ally);
        super.doDrop(hero);
        if (bonded) {
            abandonmentActive = true;
            abandonmentStartClock = globalCurseClock();
            abandonmentNextChatterClock = abandonmentStartClock + ABANDONMENT_STAGE_1;
            abandonmentStage = 0;
            speakAbandonmentLine(0);
        }
    }

    @Override
    public boolean doPickUp(Hero hero, int pos) {
        boolean wasAbandoned = abandonmentActive;
        boolean result = super.doPickUp(hero, pos);
        if (result && wasAbandoned) stopAbandonmentCurse(true);
        return result;
    }

    private String profileName() { return profile == PROFILE_GROWTH ? "성장형" : "치트형"; }
    public boolean isGrowthProfile() { return profile == PROFILE_GROWTH; }
    public int profile() { return profile; }
    public int growthHearts() { return growthHearts; }
    public boolean isBonded() { return profileLocked || summoned; }

    public static RedRibbon findRibbonForRun() {
        if (Dungeon.hero != null && Dungeon.hero.belongings != null) {
            RedRibbon carried = Dungeon.hero.belongings.getItem(RedRibbon.class);
            if (carried != null) return carried;
            RibbonTransit transit = Dungeon.hero.buff(RibbonTransit.class);
            if (transit != null && transit.ribbon != null) return transit.ribbon;
        }
        return findFloorRibbon();
    }

    public static boolean abandonmentCurseActive(Hero hero) {
        if (hero == null || hero.belongings == null) return false;
        if (hero.belongings.getItem(RedRibbon.class) != null) return false;
        RibbonTransit transit = hero.buff(RibbonTransit.class);
        if (transit != null && transit.ribbon != null) {
            return transit.ribbon.isBonded() && transit.ribbon.abandonmentActive;
        }
        RedRibbon floorRibbon = findFloorRibbon();
        return floorRibbon != null && floorRibbon.isBonded() && floorRibbon.abandonmentActive;
    }

    public static float abandonmentDamageMultiplier(Hero hero) {
        return abandonmentCurseActive(hero) ? ABANDONMENT_DAMAGE_MULTIPLIER : 1f;
    }

    public static float abandonmentAttackDelayMultiplier(Hero hero) {
        return abandonmentCurseActive(hero) ? 1f / ABANDONMENT_ATTACK_SPEED_MULTIPLIER : 1f;
    }

    public static float abandonmentMoveSpeedMultiplier(Hero hero) {
        return abandonmentCurseActive(hero) ? ABANDONMENT_MOVE_SPEED_MULTIPLIER : 1f;
    }

    private static RedRibbon findFloorRibbon() {
        if (Dungeon.level == null || Dungeon.level.heaps == null) return null;
        for (Heap heap : Dungeon.level.heaps.valueList()) {
            if (heap == null) continue;
            for (Item item : heap.items) if (item instanceof RedRibbon) return (RedRibbon)item;
        }
        return null;
    }

    private static Heap floorHeapContaining(RedRibbon ribbon) {
        if (ribbon == null || Dungeon.level == null || Dungeon.level.heaps == null) return null;
        for (Heap heap : Dungeon.level.heaps.valueList()) {
            if (heap != null && heap.items.contains(ribbon)) return heap;
        }
        return null;
    }

    private static float globalCurseClock() {
        return Statistics.duration + Actor.now();
    }

    private float abandonmentRepeatInterval(int stage) {
        switch (stage) {
            case 4: return 25f;
            case 3: return 35f;
            case 2: return 45f;
            case 1: return 55f;
            default: return 70f;
        }
    }

    private int abandonmentObsessionStage() {
        if (!isGrowthProfile()) return 0;
        if (growthHearts >= 48) return 4;
        if (growthHearts >= 36) return 3;
        if (growthHearts >= 24) return 2;
        if (growthHearts >= 12) return 1;
        return 0;
    }

    private void updateAbandonmentCurse() {
        if (!abandonmentActive || !isBonded()) return;
        float now = globalCurseClock();
        float elapsed = Math.max(0f, now - abandonmentStartClock);
        int stage = 0;
        if (elapsed >= ABANDONMENT_STAGE_4) stage = 4;
        else if (elapsed >= ABANDONMENT_STAGE_3) stage = 3;
        else if (elapsed >= ABANDONMENT_STAGE_2) stage = 2;
        else if (elapsed >= ABANDONMENT_STAGE_1) stage = 1;

        if (stage > abandonmentStage) {
            abandonmentStage = stage;
            speakAbandonmentLine(stage);
            abandonmentNextChatterClock = now + abandonmentRepeatInterval(stage);
        } else if (stage > 0 && abandonmentNextChatterClock >= 0f && now >= abandonmentNextChatterClock) {
            speakAbandonmentLine(stage);
            abandonmentNextChatterClock = now + abandonmentRepeatInterval(stage);
        }
    }

    private void stopAbandonmentCurse(boolean speak) {
        abandonmentActive = false;
        abandonmentStartClock = -1f;
        abandonmentNextChatterClock = -1f;
        abandonmentStage = -1;
        if (!speak) return;
        YandereAlly ally = findAlly();
        String line = "아하하! 주웠다♡ 그치? 실수였지? 나 버릴 리가 없잖아. 그치? 그치?♡";
        if (ally != null && ally.isAlive()) ally.yell(line);
        else GLog.i(line);
    }

    private void speakAbandonmentLine(int stage) {
        int obsession = abandonmentObsessionStage();
        String line;

        if (obsession >= 4) {
            switch (stage) {
                case 4:
                case 3: line = "아하하하하♡ 또 버렸네? 그래, 도망가 봐. 다음 층에서도 네 옆에 있을 테니까."; break;
                case 2: line = "도망갈 생각 하는 거야? 아하하♡ 해 봐. 얼마나 멀리 가나 보자."; break;
                case 1: line = "또 안 줍네♡ 그래도 괜찮아. 네가 어디 가든 내가 따라갈 거니까."; break;
                default: line = "또 버렸네? 장난이지? 주워♡ 네가 나 버릴 리 없다는 거 알아."; break;
            }
        } else if (obsession >= 3) {
            switch (stage) {
                case 4: line = "버리지 마! 버리지 마! 버리지 마! 씨발, 나 좀 버리지 말라고!!"; break;
                case 3: line = "주워! 지금 당장 주우라고! 나 버리고 어디 가려고 하는데? 네가 어디까지 가든 내가 따라갈 거야!"; break;
                case 2: line = "버리지 마. 버리지 말라고. 내가 몇 번을 말해야 알아들어?"; break;
                case 1: line = "주우라고 했잖아. 왜 말을 안 들어? 내가 지금 장난하는 것 같아?"; break;
                default: line = "왜 버렸어? 빨리 주워. 나 싫어서 그런 거 아니지?"; break;
            }
        } else if (obsession >= 2) {
            switch (stage) {
                case 4: line = "버리지 마! 나 진짜 미칠 것 같아! 왜 나를 계속 버려!"; break;
                case 3: line = "주워. 지금 당장 주워. 나 버리고 혼자 갈 생각 하지 마. 절대 못 가."; break;
                case 2: line = "버리지 마. 버리지 말라고. 내가 몇 번을 말해야 알아들어?"; break;
                case 1: line = "주우라고 했잖아. 왜 말을 안 들어? 내가 지금 장난하는 것 같아?"; break;
                default: line = "왜 버렸어? 빨리 주워. 나 싫어서 그런 거 아니지?"; break;
            }
        } else if (obsession >= 1) {
            switch (stage) {
                case 4: line = "나 두고 가지 마! 나 버리지 말라고 했잖아! 돌아와서 주워!"; break;
                case 3: line = "나 버리고 혼자 갈 생각 하지 마. 진짜 싫어. 지금 주워."; break;
                case 2: line = "나 버린 거 아니지? 아니라고 해. 빨리 주워."; break;
                case 1: line = "왜 아직도 안 주워? 나 여기 있잖아. 나 좀 봐줘."; break;
                default: line = "왜 버렸어? 빨리 주워. 나 싫어서 그런 거 아니지?"; break;
            }
        } else {
            switch (stage) {
                case 4: line = "나 진짜 무서워. 나 버리지 마. 돌아와서 주워."; break;
                case 3: line = "버리지 마. 나 두고 가지 마. 지금 주워."; break;
                case 2: line = "나 버린 거 아니지? 아니라고 해. 주워."; break;
                case 1: line = "나 여기 있잖아. 왜 그냥 가? 빨리 주워."; break;
                default: line = "왜 버렸어? 빨리 주워. 나 싫어서 그런 거 아니지?"; break;
            }
        }

        YandereAlly ally = findAlly();
        if (ally != null && ally.isAlive()) ally.yell(line);
        else GLog.w(line);
    }

    private void speakAbandonmentFloorTransferLine() {
        String line;
        if (abandonmentObsessionStage() >= 4) {
            line = "아하하하하♡ 또 버렸네? 그래, 도망가 봐. 다음 층에서도 네 옆에 있을 테니까.";
        } else {
            line = "어디 가? 내가 못 따라올 줄 알았어? 하하하, 진짜 귀엽네. 또 버려봐. 또 도망가 봐.";
        }
        YandereAlly ally = findAlly();
        if (ally != null && ally.isAlive()) ally.yell(line);
        else GLog.w(line);
    }

    public boolean recordGrowthHeart() {
        if (!isGrowthProfile() || growthHearts >= MAX_GROWTH_HEARTS) return false;
        growthHearts++;
        return true;
    }

    private String primaryCommandLabel(YandereAlly ally) {
        if (ally == null) {
            if (isGrowthProfile() && growthRevivalPending) return "하트 2개로 되살리기";
            return "소환";
        }
        if (ally instanceof GrowthYandereAlly) {
            GrowthYandereAlly growth = (GrowthYandereAlly)ally;
            return growth.hasEmergencyRecall() ? "도움 요청하기" : "내 옆으로 오라고 하기";
        }
        return "내 옆으로 불러오기";
    }

    private void showMainMenu(final Hero hero) {
        final YandereAlly ally = findAlly();
        String summary;
        String profileSummary = "유형: " + profileName()
                + (isGrowthProfile() ? "\n성장 하트: " + growthHearts + "/" + MAX_GROWTH_HEARTS : "");

        if (ally == null) {
            summary = profileSummary + "\n" + (summoned
                    ? "얀데레가 현재 층에 없다.\n다음 영웅 행동 때 자동으로 따라온다."
                    : "현재 소환된 얀데레: 없음");
        } else {
            captureFrom(ally);
            summary = profileSummary
                    + "\n태세: " + ally.modeName()
                    + "\n광란도: " + ally.rage() + "/100"
                    + "\n감정: " + ally.emotionName()
                    + (ally.hostileToHero() ? "\n\n경고: 현재 주인공을 추격 중!" : "");
        }

        GameScene.show(new WndOptions(
                new ItemSprite(this),
                "붉은 리본",
                summary,
                primaryCommandLabel(ally),
                "상태 보기",
                "태세 변경",
                "대화하기",
                "유형 선택/보기",
                "테스트/밸런스"
        ) {
            @Override
            protected void onSelect(int index) {
                switch (index) {
                    case 0: spawnOrRecall(hero, true); break;
                    case 1: showStatus(); break;
                    case 2: showModeMenu(); break;
                    case 3: doTalk(); break;
                    case 4: showProfileMenu(); break;
                    case 5: showDebugMenu(); break;
                    default: break;
                }
            }
        });
    }

    private void showProfileMenu() {
        if (profileLocked) {
            String text = "현재 유형: " + profileName()
                    + (isGrowthProfile() ? "\n성장 하트: " + growthHearts + "/" + MAX_GROWTH_HEARTS : "")
                    + "\n\n첫 소환이 끝난 뒤에는 이 플레이에서 유형을 바꿀 수 없어.";
            GameScene.show(new WndOptions("얀데레 유형", text, "닫기"));
            return;
        }

        GameScene.show(new WndOptions(
                "얀데레 유형",
                "첫 소환 전에만 선택할 수 있어.\n\n"
                        + "치트형은 지금까지의 초강력 실험용 얀데레를 유지한다.\n"
                        + "성장형은 하트를 최대 48개까지 먹이며 성장하는 정식 플레이용 유형이다.\n\n"
                        + "현재 선택: " + profileName(),
                "치트형",
                "성장형"
        ) {
            @Override
            protected void onSelect(int index) {
                if (index == 0) {
                    profile = PROFILE_CHEAT;
                    growthHearts = 0;
                    growthRevivalPending = false;
                    savedGrowthHP = -1;
                    lastHeartWarningDepth = Integer.MIN_VALUE;
                    lastHeartWarningBranch = Integer.MIN_VALUE;
                    GLog.i("얀데레 유형을 치트형으로 선택했어.");
                } else if (index == 1) {
                    profile = PROFILE_GROWTH;
                    growthHearts = 0;
                    growthRevivalPending = false;
                    savedGrowthHP = -1;
                    lastHeartWarningDepth = Integer.MIN_VALUE;
                    lastHeartWarningBranch = Integer.MIN_VALUE;
                    GLog.i("얀데레 유형을 성장형으로 선택했어. 첫 소환 뒤에는 바꿀 수 없어.");
                }
                Item.updateQuickslot();
            }
        });
    }

    public YandereAlly findAlly() {
        if (Dungeon.level == null) return null;
        if (allyID != 0) {
            Actor a = Actor.findById(allyID);
            if (a instanceof YandereAlly && ((YandereAlly)a).isAlive() && Dungeon.level.mobs.contains(a)) return (YandereAlly)a;
        }
        for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
            if (mob instanceof YandereAlly && mob.isAlive()) {
                allyID = mob.id();
                return (YandereAlly)mob;
            }
        }
        allyID = 0;
        return null;
    }

    private int adjacentCell(Hero hero) {
        for (int off : PathFinder.NEIGHBOURS8) {
            int cell = hero.pos + off;
            if (cell < 0 || cell >= Dungeon.level.length()) continue;
            if (Actor.findChar(cell) == null && (Dungeon.level.passable[cell] || Dungeon.level.avoid[cell])) return cell;
        }
        return -1;
    }

    private int farthestRespawnCell(YandereAlly ally, Hero hero) {
        if (Dungeon.level == null || hero == null) return -1;
        int best = -1;
        int bestDist = -1;
        for (int i = 0; i < 32; i++) {
            int cell = Dungeon.level.randomRespawnCell(ally);
            if (cell == -1) continue;
            int d = Dungeon.level.distance(cell, hero.pos);
            if (d > bestDist) { best = cell; bestDist = d; }
        }
        return best;
    }

    private int inventoryHeartCount(Hero hero) {
        if (hero == null || hero.belongings == null) return 0;
        int count = 0;
        for (YandereHeart heart : hero.belongings.getAllItems(YandereHeart.class)) {
            count += Math.max(0, heart.quantity());
        }
        return count;
    }

    private boolean consumeInventoryHearts(Hero hero, int amount) {
        if (hero == null || hero.belongings == null || amount <= 0) return false;
        if (inventoryHeartCount(hero) < amount) return false;

        int remaining = amount;
        ArrayList<YandereHeart> stacks = hero.belongings.getAllItems(YandereHeart.class);
        for (YandereHeart heart : stacks) {
            while (remaining > 0 && heart.quantity() > 0) {
                heart.detach(hero.belongings.backpack);
                remaining--;
            }
            if (remaining <= 0) break;
        }
        Item.updateQuickslot();
        return remaining <= 0;
    }

    private void reviveGrowth(Hero hero) {
        if (!isGrowthProfile() || !growthRevivalPending) return;
        int available = inventoryHeartCount(hero);
        if (available < GROWTH_REVIVE_HEART_COST) {
            GLog.w("되살리려면 애정의 하트 " + GROWTH_REVIVE_HEART_COST + "개가 필요해. 지금은 " + available + "개 있어.");
            return;
        }

        // Build the ally first so a blocked floor never eats the revival cost.
        summoned = true;
        savedHostile = false;
        savedRage = 0;
        savedGrowthHP = -1; // -1 makes configureGrowth restore at full HP.
        createOnCurrentFloor(hero, false, true);
        YandereAlly revived = findAlly();
        if (!(revived instanceof GrowthYandereAlly)) {
            summoned = false;
            growthRevivalPending = true;
            GLog.w("지금은 얀데레가 되살아날 자리를 만들 수 없어. 하트는 소모하지 않았어.");
            return;
        }

        if (!consumeInventoryHearts(hero, GROWTH_REVIVE_HEART_COST)) {
            // This should be unreachable after the count check, but do not silently
            // turn revival hearts into growth/relationship hearts in any case.
            revived.die(this);
            summoned = false;
            growthRevivalPending = true;
            GLog.w("하트를 소모하지 못해서 부활을 취소했어.");
            return;
        }

        growthRevivalPending = false;
        captureFrom(revived);
        String revivalLine;
        if (growthHearts >= 48) {
            revivalLine = "아하하하하♡ 역시 그럴 줄 알았어! 네가 나 죽은 채로 둘 리 없잖아♡ 봐, 죽어도 다시 네 옆이야. 이제 진짜 영원히 같이 있는 거야. 영원히♡";
        } else if (growthHearts >= 36) {
            revivalLine = "아하하하♡ 살려줬네! 나 죽었는데도 또 붙잡아줬어! 이제 알겠지? 나 진짜 네 거야. 죽어도, 또 죽어도, 계속 네 옆으로 돌아올 거야♡";
        } else if (growthHearts >= 24) {
            revivalLine = "나 다시 네 옆이야♡ 하트 두 개까지 써서 되살렸잖아. 그럼 이제 나 절대 버리면 안 돼. 죽어도 또 네 옆으로 돌아올 거니까♡";
        } else if (growthHearts >= 12) {
            revivalLine = "아하하♡ 진짜 살려줬어. 역시 나 없으면 싫지? 나도 너 없이 못 살아. 이번엔 더 가까이 붙어 있을래♡";
        } else {
            revivalLine = "으응, 다시 불러줬네♡ 나 없으니까 조금 허전했지? 이번엔 더 꼭 붙어 있을게. 나 두고 가지 마♡";
        }
        revived.yell(revivalLine);
        GLog.p("애정의 하트 2개를 희생해서 성장형 얀데레를 최대 HP로 되살렸다.");
    }

    private void spawnOrRecall(Hero hero, boolean manual) {
        if (Dungeon.level == null || hero == null) return;
        YandereAlly ally = findAlly();

        if (ally != null) {
            if (ally.hostileToHero()) {
                GLog.w("지금은 붉은 리본의 귀환 명령을 듣지 않는다. 하트를 찾아 던져야 해.");
                return;
            }

            if (ally instanceof GrowthYandereAlly) {
                GrowthYandereAlly growth = (GrowthYandereAlly)ally;
                if (growth.hasEmergencyRecall()) {
                    if (!growth.requestHelp()) {
                        GLog.w("지금은 얀데레가 네 옆으로 올 안전한 자리가 없어.");
                        return;
                    }
                } else {
                    // 27하트 전에는 순간이동이 아직 없다. 명령을 받으면 직접 걸어서 돌아온다.
                    growth.followHero();
                    growth.yell("응♡ 지금 갈게. 거기 있어, 금방 네 옆으로 갈게.");
                }
                captureFrom(growth);
                return;
            }

            // 치트형은 기존 즉시 귀환을 그대로 유지한다.
            int cell = adjacentCell(hero);
            if (cell == -1) {
                GLog.w("옆에 얀데레가 설 자리가 없어.");
                return;
            }
            ScrollOfTeleportation.appear(ally, cell);
            ally.followHero();
            ally.yell("불렀어? 헤헤♡ 바로 왔어. 나 필요했지?");
            captureFrom(ally);
            return;
        }

        if (isGrowthProfile() && growthRevivalPending) {
            reviveGrowth(hero);
            return;
        }

        summoned = true;
        createOnCurrentFloor(hero, manual);
    }

    private void createOnCurrentFloor(Hero hero, boolean manual) {
        createOnCurrentFloor(hero, manual, false);
    }

    private void createOnCurrentFloor(Hero hero, boolean manual, boolean revival) {
        if (Dungeon.level == null || hero == null || findAlly() != null) return;
        YandereAlly ally;
        if (isGrowthProfile()) {
            GrowthYandereAlly growthAlly = new GrowthYandereAlly();
            growthAlly.configureGrowth(growthHearts, savedGrowthHP);
            ally = growthAlly;
        } else ally = new YandereAlly();

        ally.applyPersistentState(savedMode, savedRage, savedEffectiveHearts, savedTotalHearts, savedHostile);
        int cell;
        if (savedHostile) {
            cell = farthestRespawnCell(ally, hero);
            if (cell == -1) cell = adjacentCell(hero);
        } else {
            cell = adjacentCell(hero);
            if (cell == -1) cell = Dungeon.level.randomRespawnCell(ally);
        }
        if (cell == -1) {
            GLog.w("얀데레를 놓을 수 있는 칸을 찾지 못했어.");
            return;
        }

        profileLocked = true;
        ally.pos = cell;
        allyID = ally.id();
        GameScene.add(ally, 0f);
        Dungeon.level.occupyCell(ally);

        if (revival) {
            // reviveGrowth emits its own line after the two hearts are actually consumed.
        } else if (manual && !savedHostile) ally.yell("찾았다♡ 이제 나 두고 혼자 가지 마. 끝까지 같이 가는 거야.");
        else if (!manual && !savedHostile) ally.queueFloorArrivalDialogue(false);
        else if (savedHostile) ally.queueFloorArrivalDialogue(true);
        Item.updateQuickslot();
    }

    private void captureFrom(YandereAlly ally) {
        if (ally == null) return;
        savedMode = ally.mode();
        savedRage = ally.rage();
        savedEffectiveHearts = ally.effectiveHearts();
        savedTotalHearts = ally.totalHearts();
        savedHostile = ally.hostileToHero();
        savedGrowthHP = ally instanceof GrowthYandereAlly ? ally.HP : -1;
        allyID = ally.id();
        summoned = true;
        growthRevivalPending = false;
        profileLocked = true;
    }

    private boolean currentFloorHasYandereHeart() {
        if (Dungeon.level == null || Dungeon.level.heaps == null) return false;
        for (Heap heap : Dungeon.level.heaps.valueList()) {
            if (heap == null) continue;
            for (Item item : heap.items) if (item instanceof YandereHeart) return true;
        }
        return false;
    }

    private void warnAboutMissedHeart(YandereAlly ally) {
        if (!isGrowthProfile() || growthHearts < GrowthYandereAlly.HEART_MISSED_WARNING) return;
        if (lastHeartWarningDepth == Dungeon.depth && lastHeartWarningBranch == Dungeon.branch) return;
        if (!currentFloorHasYandereHeart()) return;
        lastHeartWarningDepth = Dungeon.depth;
        lastHeartWarningBranch = Dungeon.branch;
        if (ally != null) ally.yell("잠깐. 이 층에 하트가 아직 남아 있어. 그냥 갈 거야?");
        GLog.w("이 층에 아직 애정의 하트가 남아 있다.");
    }

    public static void beforeTransition() {
        if (Dungeon.hero == null) return;
        RedRibbon carried = Dungeon.hero.belongings.getItem(RedRibbon.class);
        RedRibbon ribbon = carried != null ? carried : findFloorRibbon();
        if (ribbon == null) {
            RibbonTransit existingTransit = Dungeon.hero.buff(RibbonTransit.class);
            if (existingTransit != null) ribbon = existingTransit.ribbon;
        }
        if (ribbon == null) return;
        if (carried == null && !ribbon.isBonded()) return;
        YandereAlly ally = ribbon.findAlly();
        ribbon.warnAboutMissedHeart(ally);
        if (ally != null) ribbon.captureFrom(ally);
        ribbon.allyID = 0;
        if (carried == null) {
            Heap heap = floorHeapContaining(ribbon);
            if (heap != null) {
                heap.remove(ribbon);
                RibbonTransit transit = Buff.affect(Dungeon.hero, RibbonTransit.class);
                transit.ribbon = ribbon;
            }
        }
    }

    private static int ribbonDropCell(Hero hero) {
        if (Dungeon.level == null || hero == null) return -1;
        for (int off : PathFinder.NEIGHBOURS8) {
            int cell = hero.pos + off;
            if (cell < 0 || cell >= Dungeon.level.length()) continue;
            if (Actor.findChar(cell) == null && (Dungeon.level.passable[cell] || Dungeon.level.avoid[cell])) return cell;
        }
        return hero.pos;
    }

    public static void ensureYanderePresent() {
        if (Dungeon.hero == null || Dungeon.level == null) return;
        RedRibbon carried = Dungeon.hero.belongings.getItem(RedRibbon.class);
        RedRibbon ribbon = carried;
        boolean followedAcrossFloor = false;
        if (ribbon == null) {
            RibbonTransit transit = Dungeon.hero.buff(RibbonTransit.class);
            if (transit != null && transit.ribbon != null) {
                ribbon = transit.takeRibbon();
                int cell = ribbonDropCell(Dungeon.hero);
                if (cell >= 0) {
                    Heap heap = Dungeon.level.drop(ribbon, cell);
                    if (heap.sprite != null) heap.sprite.drop(Dungeon.hero.pos);
                    followedAcrossFloor = ribbon.abandonmentActive;
                }
            } else {
                ribbon = findFloorRibbon();
            }
        }
        if (ribbon == null) return;
        if (carried != null && ribbon.abandonmentActive) ribbon.stopAbandonmentCurse(false);
        ribbon.updateAbandonmentCurse();
        if (!ribbon.summoned) {
            if (followedAcrossFloor) ribbon.speakAbandonmentFloorTransferLine();
            return;
        }
        YandereAlly ally = ribbon.findAlly();
        if (ally == null) {
            ribbon.createOnCurrentFloor(Dungeon.hero, false);
            ally = ribbon.findAlly();
        } else {
            ribbon.captureFrom(ally);
        }
        if (followedAcrossFloor) ribbon.speakAbandonmentFloorTransferLine();
    }

    public static void onYandereDied(YandereAlly ally) {
        if (Dungeon.hero == null) return;
        RedRibbon ribbon = findRibbonForRun();
        if (ribbon == null) return;
        ribbon.summoned = false;
        ribbon.allyID = 0;
        ribbon.savedHostile = false;
        ribbon.savedRage = 0;
        ribbon.savedGrowthHP = -1;
        ribbon.growthRevivalPending = ribbon.isGrowthProfile() && ally instanceof GrowthYandereAlly;
        ribbon.profileLocked = true;
        if (ally != null) {
            ribbon.savedMode = ally.mode();
            ribbon.savedEffectiveHearts = ally.effectiveHearts();
            ribbon.savedTotalHearts = ally.totalHearts();
        }
    }

    private void showStatus() {
        YandereAlly ally = findAlly();
        if (ally == null) {
            String text = "유형: " + profileName()
                    + (isGrowthProfile() ? "\n성장 하트: " + growthHearts + "/" + MAX_GROWTH_HEARTS : "")
                    + "\n\n" + (growthRevivalPending && isGrowthProfile()
                        ? "성장형 얀데레가 죽었다. 애정의 하트 2개를 희생하면 최대 HP로 되살릴 수 있다."
                        : summoned ? "층 이동 직후라면 영웅이 한 번 행동하면 자동으로 따라온다." : "현재 소환된 얀데레가 없어.");
            GameScene.show(new WndOptions("얀데레 상태", text, "닫기"));
            return;
        }

        captureFrom(ally);
        String text = "유형: " + profileName()
                + (isGrowthProfile() ? "\n성장 하트: " + growthHearts + "/" + MAX_GROWTH_HEARTS : "")
                + "\n태세: " + ally.modeName()
                + "\n감정: " + ally.emotionName()
                + "\n광란도: " + ally.rage() + "/100"
                + "\nHP: " + ally.HP + "/" + ally.HT
                + "\n공격력: " + ally.statAttackMin() + "~" + ally.statAttackMax()
                + "\n피해 경감: " + ally.statDrMin() + "~" + ally.statDrMax()
                + "\n명중: " + ally.statAccuracy()
                + "\n방어 스킬: " + ally.statDefenseSkill()
                + "\n이동속도: x" + String.format("%.2f", ally.statMoveSpeed())
                + "\n공격속도: x" + String.format("%.2f", ally.statAttackSpeed())
                + (ally instanceof GrowthYandereAlly
                    ? "\n자연회복: " + Math.round(((GrowthYandereAlly)ally).statRegenInterval())
                        + "턴마다 최대 HP의 " + Math.round(((GrowthYandereAlly)ally).statRegenPercent() * 100) + "%" : "")
                + "\n처치 수(대략): " + ally.kills()
                + "\n현재 표적: " + ally.currentTargetName()
                + "\n\n마지막 하트급 애정표현: " + ally.turnsSinceAffection() + "턴 전"
                + "\n현재 층 체류: " + ally.floorTurns() + "턴"
                + "\n대화 재사용: " + ally.turnsUntilTalk() + "턴"
                + "\n받은 하트: " + ally.totalHearts()
                + "\n유효 하트: " + ally.effectiveHearts()
                + "\n\n[v0.2 밸런스]"
                + "\n하트: 광란 0 + 타이머 완전 초기화"
                + "\n유예: " + Math.round(YandereAlly.AFFECTION_GRACE) + "턴"
                + "\n이후 " + Math.round(YandereAlly.RAGE_INTERVAL) + "턴마다 광란 +1"
                + "\n100 도달 후 " + Math.round(YandereAlly.LIMIT_SAFE_TIME) + "턴 확정 안전"
                + "\n그 뒤 얀데레 행동마다 붕괴 확률 " + Math.round(YandereAlly.SNAP_CHANCE_PER_ACT * 100) + "%"
                + (ally.rage() >= 100 && !ally.hostileToHero() ? "\n현재 한계 상태 경과: " + ally.turnsAtLimit() + "턴" : "")
                + "\n\n※ 성장형의 성장 하트는 받은 하트/유효 하트와 별도로 0~48에서 관리한다.";
        GameScene.show(new WndOptions("얀데레 상태", text, "닫기"));
    }

    private void showModeMenu() {
        final YandereAlly ally = findAlly();
        if (ally == null) {
            GLog.w("먼저 얀데레를 소환해.");
            return;
        }
        final int guardRange = ally instanceof GrowthYandereAlly ? ((GrowthYandereAlly)ally).guardRange() : 2;
        GameScene.show(new WndOptions(
                "전투 태세",
                "현재: " + ally.modeName() + (ally.emergencyProtect() ? "\n\n주인공 저체력 때문에 현재 명령보다 강제 보호가 우선된다." : ""),
                "광폭 — 층 전체 적을 찾아가서 학살",
                "수비 — 주인공 " + guardRange + "칸 안의 적만 공격",
                "공격 금지 — 평소엔 따라오기만 함"
        ) {
            @Override
            protected void onSelect(int index) {
                if (index == 0) ally.setMode(YandereAlly.MODE_RAMPAGE);
                if (index == 1) ally.setMode(YandereAlly.MODE_GUARD);
                if (index == 2) ally.setMode(YandereAlly.MODE_PEACE);
                captureFrom(ally);
                Item.updateQuickslot();
            }
        });
    }

    private void doTalk() {
        YandereAlly ally = findAlly();
        if (ally == null) {
            GLog.w("먼저 얀데레를 소환해.");
            return;
        }
        ally.talk();
        captureFrom(ally);
        updateQuickslot();
    }

    private void showDebugMenu() {
        final YandereAlly ally = findAlly();
        if (ally == null) {
            GLog.w("먼저 얀데레를 소환해.");
            return;
        }
        GameScene.show(new WndOptions(
                "테스트/밸런스",
                "실제 하트는 맵에도 생성된다. 이 메뉴는 빠른 테스트용.",
                "광란 0", "광란 50", "광란 70", "광란 85", "광란 95", "광란 100", "하트 x3 생성"
        ) {
            @Override
            protected void onSelect(int index) {
                int[] values = {0, 50, 70, 85, 95, 100};
                if (index >= 0 && index < values.length) {
                    ally.debugSetRage(values[index]);
                    captureFrom(ally);
                    GLog.i("광란도를 " + values[index] + "으로 설정했어.");
                } else if (index == 6) {
                    new YandereHeart().quantity(3).collect();
                    GLog.i("테스트용 하트 3개를 가방에 넣었어.");
                }
                Item.updateQuickslot();
            }
        });
    }

    @Override
    public String status() {
        YandereAlly ally = findAlly();
        if (ally != null) return Integer.toString(ally.rage());
        return summoned ? Integer.toString(savedRage) : null;
    }

    private static final String ALLY_ID = "lab3_yandere_ally_id";
    private static final String SUMMONED = "lab3_yandere_summoned";
    private static final String PROFILE = "lab3_yandere_profile";
    private static final String PROFILE_LOCKED = "lab3_yandere_profile_locked";
    private static final String GROWTH_HEARTS = "lab3_yandere_growth_hearts";
    private static final String GROWTH_REVIVAL_PENDING = "lab3_yandere_growth_revival_pending";
    private static final String HEART_WARNING_DEPTH = "lab3_yandere_heart_warning_depth";
    private static final String HEART_WARNING_BRANCH = "lab3_yandere_heart_warning_branch";
    private static final String SAVED_MODE = "lab3_yandere_saved_mode";
    private static final String SAVED_RAGE = "lab3_yandere_saved_rage";
    private static final String SAVED_EFFECTIVE = "lab3_yandere_saved_effective_hearts";
    private static final String SAVED_TOTAL = "lab3_yandere_saved_total_hearts";
    private static final String SAVED_HOSTILE = "lab3_yandere_saved_hostile";
    private static final String SAVED_GROWTH_HP = "lab3_yandere_saved_growth_hp";
    private static final String ABANDONMENT_ACTIVE = "lab3_yandere_abandonment_active";
    private static final String ABANDONMENT_AGE = "lab3_yandere_abandonment_age";
    private static final String ABANDONMENT_NEXT_CHATTER = "lab3_yandere_abandonment_next_chatter";
    private static final String ABANDONMENT_STAGE = "lab3_yandere_abandonment_stage";

    public static class RibbonTransit extends Buff {
        private static final String TRANSIT_RIBBON = "lab3_yandere_transit_ribbon";
        private RedRibbon ribbon;
        { announced = false; revivePersists = true; }

        private RedRibbon takeRibbon() {
            RedRibbon result = ribbon;
            ribbon = null;
            detach();
            return result;
        }

        @Override
        public void storeInBundle(Bundle bundle) {
            super.storeInBundle(bundle);
            if (ribbon != null) bundle.put(TRANSIT_RIBBON, ribbon);
        }

        @Override
        public void restoreFromBundle(Bundle bundle) {
            super.restoreFromBundle(bundle);
            if (bundle.contains(TRANSIT_RIBBON)) {
                Object restored = bundle.get(TRANSIT_RIBBON);
                if (restored instanceof RedRibbon) ribbon = (RedRibbon)restored;
            }
        }
    }

    @Override
    public void storeInBundle(Bundle bundle) {
        YandereAlly ally = findAlly();
        if (ally != null) captureFrom(ally);
        super.storeInBundle(bundle);
        bundle.put(ALLY_ID, allyID);
        bundle.put(SUMMONED, summoned);
        bundle.put(PROFILE, profile);
        bundle.put(PROFILE_LOCKED, profileLocked);
        bundle.put(GROWTH_HEARTS, growthHearts);
        bundle.put(GROWTH_REVIVAL_PENDING, growthRevivalPending);
        bundle.put(HEART_WARNING_DEPTH, lastHeartWarningDepth);
        bundle.put(HEART_WARNING_BRANCH, lastHeartWarningBranch);
        bundle.put(SAVED_MODE, savedMode);
        bundle.put(SAVED_RAGE, savedRage);
        bundle.put(SAVED_EFFECTIVE, savedEffectiveHearts);
        bundle.put(SAVED_TOTAL, savedTotalHearts);
        bundle.put(SAVED_HOSTILE, savedHostile);
        bundle.put(SAVED_GROWTH_HP, savedGrowthHP);
        bundle.put(ABANDONMENT_ACTIVE, abandonmentActive);
        bundle.put(ABANDONMENT_AGE, abandonmentActive && abandonmentStartClock >= 0f
                ? Math.max(0f, globalCurseClock() - abandonmentStartClock) : 0f);
        bundle.put(ABANDONMENT_NEXT_CHATTER, abandonmentActive && abandonmentNextChatterClock >= 0f
                ? Math.max(0f, abandonmentNextChatterClock - globalCurseClock()) : 0f);
        bundle.put(ABANDONMENT_STAGE, abandonmentStage);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        if (bundle.contains(ALLY_ID)) allyID = bundle.getInt(ALLY_ID);
        if (bundle.contains(SUMMONED)) summoned = bundle.getBoolean(SUMMONED);
        if (bundle.contains(PROFILE)) profile = bundle.getInt(PROFILE);
        if (bundle.contains(GROWTH_HEARTS)) growthHearts = Math.max(0, Math.min(MAX_GROWTH_HEARTS, bundle.getInt(GROWTH_HEARTS)));
        growthRevivalPending = bundle.contains(GROWTH_REVIVAL_PENDING) && bundle.getBoolean(GROWTH_REVIVAL_PENDING);
        if (bundle.contains(HEART_WARNING_DEPTH)) lastHeartWarningDepth = bundle.getInt(HEART_WARNING_DEPTH);
        if (bundle.contains(HEART_WARNING_BRANCH)) lastHeartWarningBranch = bundle.getInt(HEART_WARNING_BRANCH);
        if (bundle.contains(SAVED_MODE)) savedMode = bundle.getInt(SAVED_MODE);
        if (bundle.contains(SAVED_RAGE)) savedRage = bundle.getInt(SAVED_RAGE);
        if (bundle.contains(SAVED_EFFECTIVE)) savedEffectiveHearts = bundle.getInt(SAVED_EFFECTIVE);
        if (bundle.contains(SAVED_TOTAL)) savedTotalHearts = bundle.getInt(SAVED_TOTAL);
        if (bundle.contains(SAVED_HOSTILE)) savedHostile = bundle.getBoolean(SAVED_HOSTILE);
        if (bundle.contains(SAVED_GROWTH_HP)) savedGrowthHP = bundle.getInt(SAVED_GROWTH_HP);
        abandonmentActive = bundle.contains(ABANDONMENT_ACTIVE) && bundle.getBoolean(ABANDONMENT_ACTIVE);
        float abandonmentAge = bundle.contains(ABANDONMENT_AGE) ? Math.max(0f, bundle.getFloat(ABANDONMENT_AGE)) : 0f;
        abandonmentStartClock = abandonmentActive ? globalCurseClock() - abandonmentAge : -1f;
        float abandonmentNext = bundle.contains(ABANDONMENT_NEXT_CHATTER)
                ? Math.max(0f, bundle.getFloat(ABANDONMENT_NEXT_CHATTER)) : 0f;
        abandonmentStage = bundle.contains(ABANDONMENT_STAGE) ? bundle.getInt(ABANDONMENT_STAGE) : -1;
        abandonmentNextChatterClock = abandonmentActive
                ? globalCurseClock() + (abandonmentNext > 0f ? abandonmentNext : abandonmentRepeatInterval(Math.max(0, abandonmentStage)))
                : -1f;

        if (bundle.contains(PROFILE_LOCKED)) profileLocked = bundle.getBoolean(PROFILE_LOCKED);
        else {
            profile = PROFILE_CHEAT;
            profileLocked = summoned || savedTotalHearts > 0;
            growthHearts = 0;
            growthRevivalPending = false;
            savedGrowthHP = -1;
            lastHeartWarningDepth = Integer.MIN_VALUE;
            lastHeartWarningBranch = Integer.MIN_VALUE;
        }
    }
}
