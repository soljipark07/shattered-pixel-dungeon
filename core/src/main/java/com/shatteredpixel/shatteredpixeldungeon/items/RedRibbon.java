package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
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

    private int allyID = 0;
    private boolean summoned = false;

    // 층 이동 때 아군 본체가 사라져도 리본이 상태를 들고 감.
    private int savedMode = YandereAlly.MODE_GUARD;
    private int savedRage = 0;
    private int savedEffectiveHearts = 0;
    private int savedTotalHearts = 0;
    private boolean savedHostile = false;

    {
        image = ItemSpriteSheet.RED_RIBBON;
        unique = true;
        keptThoughLostInvent = true;
        defaultAction = AC_MANAGE;
    }

    @Override
    public String name() {
        return "붉은 리본";
    }

    @Override
    public String desc() {
        return "얀데레를 소환하고 명령하는 전용 도구다.\n\n"
                + "하트는 리본 메뉴에서 공짜로 먹이는 방식이 아니라 던전 바닥에서 직접 주워 "
                + "얀데레에게 던져야 한다.";
    }

    @Override
    public boolean isIdentified() { return true; }

    @Override
    public boolean isUpgradable() { return false; }

    @Override
    public int value() { return 0; }

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = new ArrayList<>();
        actions.add(AC_MANAGE);
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

    private void showMainMenu(final Hero hero) {
        final YandereAlly ally = findAlly();

        String summary;
        if (ally == null) {
            summary = summoned
                    ? "얀데레가 현재 층에 없다.\n다음 영웅 행동 때 자동으로 따라온다."
                    : "현재 소환된 얀데레: 없음";
        } else {
            captureFrom(ally);
            summary = "태세: " + ally.modeName()
                    + "\n광란도: " + ally.rage() + "/100"
                    + "\n감정: " + ally.emotionName()
                    + (ally.hostileToHero() ? "\n\n경고: 현재 주인공을 추격 중!" : "");
        }

        GameScene.show(new WndOptions(
                new ItemSprite(this),
                "붉은 리본",
                summary,
                ally == null ? "소환" : "내 옆으로 불러오기",
                "상태 보기",
                "태세 변경",
                "대화하기",
                "테스트/밸런스"
        ) {
            @Override
            protected void onSelect(int index) {
                switch (index) {
                    case 0: spawnOrRecall(hero, true); break;
                    case 1: showStatus(); break;
                    case 2: showModeMenu(); break;
                    case 3: doTalk(); break;
                    case 4: showDebugMenu(); break;
                    default: break;
                }
            }
        });
    }

    public YandereAlly findAlly() {
        if (Dungeon.level == null) return null;

        if (allyID != 0) {
            Actor a = Actor.findById(allyID);
            if (a instanceof YandereAlly && ((YandereAlly)a).isAlive()
                    && Dungeon.level.mobs.contains(a)) {
                return (YandereAlly)a;
            }
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
            if (Actor.findChar(cell) == null
                    && (Dungeon.level.passable[cell] || Dungeon.level.avoid[cell])) {
                return cell;
            }
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
            if (d > bestDist) {
                best = cell;
                bestDist = d;
            }
        }

        return best;
    }

    private void spawnOrRecall(Hero hero, boolean manual) {
        if (Dungeon.level == null || hero == null) return;

        YandereAlly ally = findAlly();

        if (ally != null) {
            if (ally.hostileToHero()) {
                GLog.w("지금은 붉은 리본의 귀환 명령을 듣지 않는다. 하트를 찾아 던져야 해.");
                return;
            }

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

        summoned = true;
        createOnCurrentFloor(hero, manual);
    }

    private void createOnCurrentFloor(Hero hero, boolean manual) {
        if (Dungeon.level == null || hero == null || findAlly() != null) return;

        YandereAlly ally = new YandereAlly();
        ally.applyPersistentState(savedMode, savedRage, savedEffectiveHearts, savedTotalHearts, savedHostile);

        int cell;
        if (savedHostile) {
            // LAB3-y021: 광란 상태로 계단을 넘어와도 가능한 한 멀리서 추격 재개.
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

        ally.pos = cell;
        allyID = ally.id();

        GameScene.add(ally, 0f);
        Dungeon.level.occupyCell(ally);

        if (manual && !savedHostile) {
            ally.yell("찾았다♡ 이제 나 두고 혼자 가지 마. 끝까지 같이 가는 거야.");
        } else if (!manual && !savedHostile) {
            ally.queueFloorArrivalDialogue(false);
        } else if (savedHostile) {
            // 스프라이트가 완전히 붙기 전 yell()이 씹힐 수 있어서 다음 act에 예약.
            ally.queueFloorArrivalDialogue(true);
        }

        Item.updateQuickslot();
    }

    private void captureFrom(YandereAlly ally) {
        if (ally == null) return;
        savedMode = ally.mode();
        savedRage = ally.rage();
        savedEffectiveHearts = ally.effectiveHearts();
        savedTotalHearts = ally.totalHearts();
        savedHostile = ally.hostileToHero();
        allyID = ally.id();
        summoned = true;
    }

    public static void beforeTransition() {
        if (Dungeon.hero == null) return;
        RedRibbon ribbon = Dungeon.hero.belongings.getItem(RedRibbon.class);
        if (ribbon == null) return;

        YandereAlly ally = ribbon.findAlly();
        if (ally != null) ribbon.captureFrom(ally);
        ribbon.allyID = 0;
    }

    public static void ensureYanderePresent() {
        if (Dungeon.hero == null || Dungeon.level == null) return;

        RedRibbon ribbon = Dungeon.hero.belongings.getItem(RedRibbon.class);
        if (ribbon == null || !ribbon.summoned) return;

        YandereAlly ally = ribbon.findAlly();
        if (ally == null) {
            ribbon.createOnCurrentFloor(Dungeon.hero, false);
        } else {
            ribbon.captureFrom(ally);
        }
    }

    public static void onYandereDied(YandereAlly ally) {
        if (Dungeon.hero == null) return;

        RedRibbon ribbon = Dungeon.hero.belongings.getItem(RedRibbon.class);
        if (ribbon == null) return;

        // 죽여버린 경우 다음 턴 즉시 무한부활은 하지 않음.
        ribbon.summoned = false;
        ribbon.allyID = 0;
        ribbon.savedHostile = false;
        ribbon.savedRage = 0;
        if (ally != null) {
            ribbon.savedMode = ally.mode();
            ribbon.savedEffectiveHearts = ally.effectiveHearts();
            ribbon.savedTotalHearts = ally.totalHearts();
        }
    }

    private void showStatus() {
        YandereAlly ally = findAlly();

        if (ally == null) {
            String text = summoned
                    ? "층 이동 직후라면 영웅이 한 번 행동하면 자동으로 따라온다."
                    : "현재 소환된 얀데레가 없어.";
            GameScene.show(new WndOptions("얀데레 상태", text, "닫기"));
            return;
        }

        captureFrom(ally);

        String text =
                "태세: " + ally.modeName()
                + "\n감정: " + ally.emotionName()
                + "\n광란도: " + ally.rage() + "/100"
                + "\nHP: " + ally.HP + "/" + ally.HT
                + "\n공격력: " + ally.statAttackMin() + "~" + ally.statAttackMax()
                + "\n피해 경감: " + ally.statDrMin() + "~" + ally.statDrMax()
                + "\n명중: " + ally.statAccuracy()
                + "\n방어 스킬: " + ally.statDefenseSkill()
                + "\n이동속도: x" + String.format("%.2f", ally.statMoveSpeed())
                + "\n공격속도: x" + String.format("%.2f", ally.statAttackSpeed())
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
                + (ally.rage() >= 100 && !ally.hostileToHero()
                    ? "\n현재 한계 상태 경과: " + ally.turnsAtLimit() + "턴" : "")
                + "\n\n※ 유효 하트는 다음 성장/능력 해금 버전을 위해 미리 기록한다.";

        GameScene.show(new WndOptions("얀데레 상태", text, "닫기"));
    }

    private void showModeMenu() {
        final YandereAlly ally = findAlly();

        if (ally == null) {
            GLog.w("먼저 얀데레를 소환해.");
            return;
        }

        GameScene.show(new WndOptions(
                "전투 태세",
                "현재: " + ally.modeName()
                        + (ally.emergencyProtect()
                        ? "\n\n주인공 저체력 때문에 현재 명령보다 강제 보호가 우선된다." : ""),
                "광폭 — 층 전체 적을 찾아가서 학살",
                "수비 — 주인공 2칸 안의 적만 공격",
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
                "광란 0",
                "광란 50",
                "광란 70",
                "광란 85",
                "광란 95",
                "광란 100",
                "하트 x3 생성"
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
    private static final String SAVED_MODE = "lab3_yandere_saved_mode";
    private static final String SAVED_RAGE = "lab3_yandere_saved_rage";
    private static final String SAVED_EFFECTIVE = "lab3_yandere_saved_effective_hearts";
    private static final String SAVED_TOTAL = "lab3_yandere_saved_total_hearts";
    private static final String SAVED_HOSTILE = "lab3_yandere_saved_hostile";

    @Override
    public void storeInBundle(Bundle bundle) {
        YandereAlly ally = findAlly();
        if (ally != null) captureFrom(ally);

        super.storeInBundle(bundle);
        bundle.put(ALLY_ID, allyID);
        bundle.put(SUMMONED, summoned);
        bundle.put(SAVED_MODE, savedMode);
        bundle.put(SAVED_RAGE, savedRage);
        bundle.put(SAVED_EFFECTIVE, savedEffectiveHearts);
        bundle.put(SAVED_TOTAL, savedTotalHearts);
        bundle.put(SAVED_HOSTILE, savedHostile);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        if (bundle.contains(ALLY_ID)) allyID = bundle.getInt(ALLY_ID);
        if (bundle.contains(SUMMONED)) summoned = bundle.getBoolean(SUMMONED);
        if (bundle.contains(SAVED_MODE)) savedMode = bundle.getInt(SAVED_MODE);
        if (bundle.contains(SAVED_RAGE)) savedRage = bundle.getInt(SAVED_RAGE);
        if (bundle.contains(SAVED_EFFECTIVE)) savedEffectiveHearts = bundle.getInt(SAVED_EFFECTIVE);
        if (bundle.contains(SAVED_TOTAL)) savedTotalHearts = bundle.getInt(SAVED_TOTAL);
        if (bundle.contains(SAVED_HOSTILE)) savedHostile = bundle.getBoolean(SAVED_HOSTILE);
    }
}
