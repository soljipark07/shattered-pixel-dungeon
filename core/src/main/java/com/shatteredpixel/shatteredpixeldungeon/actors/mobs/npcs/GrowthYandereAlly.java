package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LifeLink;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.watabou.utils.Bundle;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

import java.util.HashSet;

/**
 * 정식 플레이용 성장형 얀데레.
 *
 * AI/광란/대화 구조는 YandereAlly를 그대로 공유하고, 성장형 전용 전투 능력과
 * 하트 단계별 해금을 이 클래스에서 덮어쓴다.
 */
public class GrowthYandereAlly extends YandereAlly {

    public static final int MAX_GROWTH_HEARTS = 48;

    public static final int BASE_HP = 45;
    public static final int HP_PER_HEART = 3;
    public static final int BASE_ATTACK_MIN = 4;
    public static final int BASE_ATTACK_MAX = 8;
    public static final int BASE_ACCURACY = 12;
    public static final int BASE_DEFENSE = 6;

    public static final float REGEN_INTERVAL = 10f;
    public static final float BASE_REGEN_PERCENT = 0.01f;

    // 하트 단계별 해금 지점
    public static final int HEART_GUARD_I = 3;
    public static final int HEART_REGEN_I = 6;
    public static final int HEART_MISSED_WARNING = 9;
    public static final int HEART_GUARD_II = 12;
    public static final int HEART_LOW_HP_I = 15;
    public static final int HEART_INTERCEPT_I = 18;
    public static final int HEART_REGEN_II = 21;
    public static final int HEART_SECRET_I = 24;
    public static final int HEART_LOW_HP_II = 33;
    public static final int HEART_SECRET_II = 36;
    public static final int HEART_INTERCEPT_II = 39;
    public static final int HEART_SECRET_III = 45;

    private static final int SECRET_NEARBY_RANGE = 2;

    private int growthHearts = 0;
    private float lastRegenClock = -1f;
    private int lastInterceptRollTurn = Integer.MIN_VALUE;

    // 탐색 감지는 정확한 비밀문 위치를 알려주지 않고, 같은 대상에 대사를 반복하지 않도록만 기록한다.
    private final HashSet<Integer> warnedNearbySecretDoors = new HashSet<>();
    private final HashSet<Integer> warnedSecretRooms = new HashSet<>();
    private int lastRoomSignature = Integer.MIN_VALUE;
    private int lastHeroPosForTrapScan = -1;

    {
        HP = HT = BASE_HP;
        defenseSkill = BASE_DEFENSE;
    }

    public GrowthYandereAlly() {
        super();
        lastRegenClock = globalGrowthClock();
    }

    @Override
    public String description() {
        if (hostileToHero()) {
            return "애정 결핍으로 이성이 끊어졌다. 성장형이어도 광란 추격 중 접촉 공격은 거의 즉사급이다. "
                    + "도망치면서 하트를 찾아 던지면 다시 진정시킬 수 있다.";
        }
        return "하트를 받을수록 천천히 강해지는 성장형 얀데레 아군이다. "
                + "기본 능력치는 평범하지만 최대 48개의 성장 하트로 체력과 전투 능력을 키울 수 있다. "
                + "오랫동안 애정표현을 받지 못하면 광란도가 오른다.";
    }

    public int growthHearts() {
        return growthHearts;
    }

    public void configureGrowth(int hearts, int savedHP) {
        growthHearts = clampHearts(hearts);
        HT = growthMaxHP();
        defenseSkill = growthDefenseSkill();
        HP = savedHP >= 0 ? Math.max(1, Math.min(HT, savedHP)) : HT;
        lastRegenClock = globalGrowthClock();
        lastInterceptRollTurn = Integer.MIN_VALUE;
        lastHeroPosForTrapScan = Dungeon.hero == null ? -1 : Dungeon.hero.pos;
        lastRoomSignature = Integer.MIN_VALUE;
        warnedNearbySecretDoors.clear();
        warnedSecretRooms.clear();
    }

    public void syncGrowthHearts(int hearts) {
        int oldHT = HT;
        growthHearts = clampHearts(hearts);
        HT = growthMaxHP();
        defenseSkill = growthDefenseSkill();

        // 최대 HP 상승분만큼 현재 HP도 함께 오른다. 하트 하나가 풀회복을 시키지는 않는다.
        if (HT > oldHT) {
            HP = Math.min(HT, HP + (HT - oldHT));
        } else {
            HP = Math.min(HP, HT);
        }
    }

    private int clampHearts(int value) {
        return Math.max(0, Math.min(MAX_GROWTH_HEARTS, value));
    }

    private int growthMaxHP() {
        return BASE_HP + HP_PER_HEART * growthHearts;
    }

    private int growthAttackMin() {
        // 1, 3, 5...번째 하트마다 최소 공격력 +1
        return BASE_ATTACK_MIN + (growthHearts + 1) / 2;
    }

    private int growthAttackMax() {
        // 모든 하트마다 최대 공격력 +1
        return BASE_ATTACK_MAX + growthHearts;
    }

    private int growthDrMin() {
        // 6하트마다 최소 피해경감 +1
        return growthHearts / 6;
    }

    private int growthDrMax() {
        // 시작 0~2, 이후 3하트마다 최대 피해경감 +1
        return 2 + growthHearts / 3;
    }

    private int growthAccuracy() {
        // 명중/회피는 매 하트가 아니라 6하트마다 계단식 상승
        return BASE_ACCURACY + 3 * (growthHearts / 6);
    }

    private int growthDefenseSkill() {
        return BASE_DEFENSE + 3 * (growthHearts / 6);
    }

    public int guardRange() {
        if (growthHearts >= HEART_GUARD_II) return 5;
        if (growthHearts >= HEART_GUARD_I) return 3;
        return 2;
    }

    public int regenStage() {
        if (growthHearts >= HEART_REGEN_II) return 2;
        if (growthHearts >= HEART_REGEN_I) return 1;
        return 0;
    }

    public int lowHpAwakeningStage() {
        if (growthHearts >= HEART_LOW_HP_II) return 2;
        if (growthHearts >= HEART_LOW_HP_I) return 1;
        return 0;
    }

    public int interceptStage() {
        if (growthHearts >= HEART_INTERCEPT_II) return 2;
        if (growthHearts >= HEART_INTERCEPT_I) return 1;
        return 0;
    }

    public int secretSenseStage() {
        if (growthHearts >= HEART_SECRET_III) return 3;
        if (growthHearts >= HEART_SECRET_II) return 2;
        if (growthHearts >= HEART_SECRET_I) return 1;
        return 0;
    }

    public boolean hasMissedHeartWarning() {
        return growthHearts >= HEART_MISSED_WARNING;
    }

    private float lowHpCombatMultiplier() {
        if (Dungeon.hero == null || lowHpAwakeningStage() == 0) return 1f;

        float hpRatio = Dungeon.hero.HP / (float)Math.max(1, Dungeon.hero.HT);
        if (hpRatio > 0.30f) return 1f;

        // I: 30% 이하에서 공격/DR +20%
        if (lowHpAwakeningStage() == 1) return 1.20f;

        // II: I를 포함하고, 더 위험할수록 보호 본능이 급격히 강해진다.
        if (hpRatio <= 0.10f) return 1.80f;
        if (hpRatio <= 0.25f) return 1.50f;
        return 1.20f;
    }

    private static float globalGrowthClock() {
        return Statistics.duration + Actor.now();
    }

    private void updateNaturalRegen() {
        float now = globalGrowthClock();

        if (lastRegenClock < 0f) {
            lastRegenClock = now;
            return;
        }

        // 적대 중/만피 상태에서는 회복 시간을 미리 저축하지 않는다.
        if (hostileToHero() || HP >= HT) {
            lastRegenClock = now;
            return;
        }

        int pulses = (int)Math.floor((now - lastRegenClock) / REGEN_INTERVAL);
        if (pulses <= 0) return;

        int healPerPulse = Math.max(1, Math.round(HT * statRegenPercent()));
        HP = Math.min(HT, HP + healPerPulse * pulses);
        lastRegenClock += pulses * REGEN_INTERVAL;
    }

    @Override
    protected Char chooseEnemy() {
        Char chosen = super.chooseEnemy();

        // 부모의 복수대상/2칸 수비/광폭 로직은 우선 존중한다.
        // 성장형 수비 강화가 해금된 경우에만, 부모가 표적을 못 찾았을 때 범위를 확장한다.
        if (chosen != null || hostileToHero() || mode() != MODE_GUARD || guardRange() <= 2) {
            return chosen;
        }

        return nearestEnemyInGrowthGuardRange();
    }

    private Char nearestEnemyInGrowthGuardRange() {
        if (Dungeon.level == null || Dungeon.hero == null) return null;

        Mob best = null;
        int bestDist = Integer.MAX_VALUE;

        for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
            if (mob == null || mob == this || !mob.isAlive()
                    || mob.alignment != Alignment.ENEMY
                    || mob.isInvulnerable(getClass())) {
                continue;
            }

            int heroDist = Dungeon.level.distance(Dungeon.hero.pos, mob.pos);
            if (heroDist > guardRange()) continue;

            int myDist = Dungeon.level.distance(pos, mob.pos);
            if (best == null || myDist < bestDist) {
                best = mob;
                bestDist = myDist;
            }
        }

        return best;
    }

    private boolean canPrepareIntercept() {
        if (Dungeon.hero == null || Dungeon.level == null || hostileToHero()) return false;
        if (interceptStage() == 0) return false;

        // 사용자가 회복시키려고 공격 금지로 돌렸을 때는 대신 맞기를 완전히 끈다.
        // 단, 주인공이 25% 이하라 강제 보호가 켜진 경우에는 평화 명령보다 보호가 우선된다.
        if (mode() == MODE_PEACE && !emergencyProtect()) return false;

        int range = interceptStage() >= 2 ? 3 : 1;
        if (Dungeon.level.distance(pos, Dungeon.hero.pos) > range) return false;

        // 전투가 아닌 상황에서 환경 피해를 대신 받는 일을 최대한 줄이기 위해
        // 현재 표적이 있거나 주인공 수비범위 안에 실제 적이 있을 때만 준비한다.
        if (enemy != null && enemy.isAlive() && enemy.alignment == Alignment.ENEMY) return true;

        int threatRange = Math.max(guardRange(), 3);
        for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
            if (mob != null && mob != this && mob.isAlive() && mob.alignment == Alignment.ENEMY
                    && Dungeon.level.distance(Dungeon.hero.pos, mob.pos) <= threatRange) {
                return true;
            }
        }
        return false;
    }

    private boolean hasNormalLifeLink() {
        if (Dungeon.hero == null) return false;
        for (LifeLink link : Dungeon.hero.buffs(LifeLink.class)) {
            if (!link.yandereGuardLink) return true;
        }
        return false;
    }

    public void clearMyInterceptLinks() {
        if (Dungeon.hero == null) return;
        for (LifeLink link : Dungeon.hero.buffs(LifeLink.class).toArray(new LifeLink[0])) {
            if (link.yandereGuardLink && link.object == id()) {
                link.detach();
            }
        }
    }

    private void updateInterceptLinks() {
        if (Dungeon.hero == null) return;

        if (!canPrepareIntercept() || hasNormalLifeLink()) {
            clearMyInterceptLinks();
            lastInterceptRollTurn = Integer.MIN_VALUE;
            return;
        }

        // 이동속도 1.2 해금 뒤에도 같은 한 턴에 여러 번 확률을 굴리지 않게 한다.
        int turnKey = (int)Math.floor(globalGrowthClock());
        if (turnKey == lastInterceptRollTurn) return;
        lastInterceptRollTurn = turnKey;

        clearMyInterceptLinks();

        int stage = interceptStage();
        float chance = stage >= 2 ? 0.50f : 0.30f;
        if (Random.Float() >= chance) return;

        // LifeLink 한 개 = 피해를 둘이 반씩 부담.
        // 세 개 = 영웅 1/4, 얀데레 3/4 부담이라 II의 75% 대신 맞기를 구현한다.
        int links = stage >= 2 ? 3 : 1;
        for (int i = 0; i < links; i++) {
            LifeLink.attachYandereGuardLink(Dungeon.hero, id(), 1.25f);
        }
    }

    private void updateExplorationSense() {
        if (Dungeon.level == null || Dungeon.hero == null || hostileToHero()) return;

        int stage = secretSenseStage();
        if (stage <= 0) {
            lastHeroPosForTrapScan = Dungeon.hero.pos;
            return;
        }

        warnNearbySecretDoors();

        if (stage >= 3) {
            warnOnSecretRoomEntry();
        }

        // 함정은 영웅이 계속 이동 중일 때 미리 밝혀주지 않는다.
        // 얀데레가 연속 두 행동에서 같은 영웅 위치를 확인한 경우에만 '멈춰 있었다'고 본다.
        boolean heroStayedStill = lastHeroPosForTrapScan == Dungeon.hero.pos;
        if (stage >= 2 && heroStayedStill) {
            revealNearbyHiddenTraps(stage >= 3 ? 3 : 2);
        }
        lastHeroPosForTrapScan = Dungeon.hero.pos;
    }

    private void warnNearbySecretDoors() {
        boolean foundNew = false;
        for (int cell = 0; cell < Dungeon.level.length(); cell++) {
            if (Dungeon.level.map[cell] == Terrain.SECRET_DOOR
                    && Dungeon.level.distance(Dungeon.hero.pos, cell) <= SECRET_NEARBY_RANGE
                    && warnedNearbySecretDoors.add(cell)) {
                foundNew = true;
            }
        }

        if (foundNew) {
            yell("잠깐♡ 이 근처, 뭔가 이상해. 그냥 지나가지 마.");
        }
    }

    private void revealNearbyHiddenTraps(int range) {
        boolean found = false;
        for (int cell = 0; cell < Dungeon.level.length(); cell++) {
            if (Dungeon.level.map[cell] == Terrain.SECRET_TRAP
                    && Dungeon.level.distance(Dungeon.hero.pos, cell) <= range) {
                Dungeon.level.discover(cell);
                found = true;
            }
        }

        if (found) {
            yell("잠깐. 근처에 함정 있어. 밟지 마.");
        }
    }

    private void warnOnSecretRoomEntry() {
        if (!(Dungeon.level instanceof RegularLevel)) return;

        Room room = ((RegularLevel)Dungeon.level).room(Dungeon.hero.pos);
        int signature = roomSignature(room);
        if (signature == lastRoomSignature) return;
        lastRoomSignature = signature;

        if (room == null || warnedSecretRooms.contains(signature)) return;

        boolean hasSecretDoor = false;
        for (Point point : room.getPoints()) {
            int cell = Dungeon.level.pointToCell(point);
            if (cell >= 0 && cell < Dungeon.level.length()
                    && Dungeon.level.map[cell] == Terrain.SECRET_DOOR) {
                hasSecretDoor = true;
                break;
            }
        }

        if (hasSecretDoor) {
            warnedSecretRooms.add(signature);
            yell("여기 뭔가 숨겨져 있어♡ 이 방, 그냥 지나가면 안 될 것 같아.");
        }
    }

    private int roomSignature(Room room) {
        if (room == null) return Integer.MIN_VALUE;
        int result = room.left;
        result = 31 * result + room.top;
        result = 31 * result + room.right;
        result = 31 * result + room.bottom;
        return result;
    }

    @Override
    protected boolean act() {
        int hpBeforeParentAct = HP;
        boolean wasFriendly = !hostileToHero();

        boolean result = super.act();

        // 부모 치트형의 act당 +100 회복만 되돌리고 성장형 자연회복을 적용한다.
        if (wasFriendly && HP > hpBeforeParentAct) {
            HP = Math.min(HT, hpBeforeParentAct);
        }
        updateNaturalRegen();
        updateInterceptLinks();
        updateExplorationSense();

        return result;
    }

    @Override
    public void setMode(int newMode) {
        super.setMode(newMode);
        if (newMode == MODE_PEACE && !emergencyProtect()) {
            clearMyInterceptLinks();
        }
    }

    @Override
    public void damage(int dmg, Object src) {
        boolean intercepted = src instanceof LifeLink
                && ((LifeLink)src).yandereGuardLink
                && ((LifeLink)src).object == id();
        super.damage(dmg, src);

        // 대신 맞기는 한 번의 피해 사건을 막고 소모된다.
        // II의 3개 링크는 Char.damage()가 이미 복사한 링크 목록을 순회하므로 같은 타격의 75%는 끝까지 전달된다.
        if (intercepted) clearMyInterceptLinks();
    }

    @Override
    public void die(Object cause) {
        clearMyInterceptLinks();
        super.die(cause);
    }

    @Override
    public int attackSkill(Char target) {
        if (hostileToHero()) return 1_000_000;
        return growthAccuracy();
    }

    @Override
    public int damageRoll() {
        if (hostileToHero() && Dungeon.hero != null) {
            return Math.max(9999, Dungeon.hero.HT * 4);
        }
        int base = Random.NormalIntRange(growthAttackMin(), growthAttackMax());
        return Math.max(0, Math.round(base * lowHpCombatMultiplier()));
    }

    @Override
    public int drRoll() {
        int base = Random.NormalIntRange(growthDrMin(), growthDrMax());
        return Math.max(0, Math.round(base * lowHpCombatMultiplier()));
    }

    @Override
    public float speed() {
        if (hostileToHero()) return 1f;
        // 「네가 아픈 건 싫어♡」 II와 함께 주인공에게 더 빨리 붙도록 1.2배.
        return growthHearts >= HEART_INTERCEPT_II ? 1.2f : 1f;
    }

    @Override
    public float attackDelay() {
        return 1f;
    }

    @Override
    public int statAttackMin() {
        if (hostileToHero()) {
            return Math.max(9999, Dungeon.hero == null ? 9999 : Dungeon.hero.HT * 4);
        }
        return Math.max(0, Math.round(growthAttackMin() * lowHpCombatMultiplier()));
    }

    @Override
    public int statAttackMax() {
        return hostileToHero() ? statAttackMin()
                : Math.max(0, Math.round(growthAttackMax() * lowHpCombatMultiplier()));
    }

    @Override
    public int statDrMin() {
        return Math.max(0, Math.round(growthDrMin() * lowHpCombatMultiplier()));
    }

    @Override
    public int statDrMax() {
        return Math.max(0, Math.round(growthDrMax() * lowHpCombatMultiplier()));
    }

    @Override
    public int statAccuracy() {
        return hostileToHero() ? 1_000_000 : growthAccuracy();
    }

    @Override
    public int statDefenseSkill() {
        return growthDefenseSkill();
    }

    @Override
    public float statMoveSpeed() {
        return hostileToHero() ? 1f : (growthHearts >= HEART_INTERCEPT_II ? 1.2f : 1f);
    }

    @Override
    public float statAttackSpeed() {
        return 1f;
    }

    public float statRegenPercent() {
        if (growthHearts >= HEART_REGEN_II) return 0.03f;
        if (growthHearts >= HEART_REGEN_I) return 0.02f;
        return BASE_REGEN_PERCENT;
    }

    public float statRegenInterval() {
        return REGEN_INTERVAL;
    }

    public float interceptChance() {
        if (interceptStage() >= 2) return 0.50f;
        if (interceptStage() == 1) return 0.30f;
        return 0f;
    }

    public float interceptShare() {
        if (interceptStage() >= 2) return 0.75f;
        if (interceptStage() == 1) return 0.50f;
        return 0f;
    }

    public int interceptRange() {
        if (interceptStage() >= 2) return 3;
        if (interceptStage() == 1) return 1;
        return 0;
    }

    private static final String GROWTH_HEARTS = "lab3_growth_yandere_hearts";
    private static final String REGEN_AGE = "lab3_growth_yandere_regen_age";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);

        float now = globalGrowthClock();
        float regenAge = lastRegenClock < 0f ? 0f : Math.max(0f, now - lastRegenClock);

        bundle.put(GROWTH_HEARTS, growthHearts);
        bundle.put(REGEN_AGE, regenAge);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);

        if (bundle.contains(GROWTH_HEARTS)) {
            growthHearts = clampHearts(bundle.getInt(GROWTH_HEARTS));
        }

        HT = growthMaxHP();
        defenseSkill = growthDefenseSkill();
        HP = Math.min(HP, HT);

        float regenAge = bundle.contains(REGEN_AGE)
                ? Math.max(0f, bundle.getFloat(REGEN_AGE)) : 0f;
        lastRegenClock = globalGrowthClock() - Math.min(REGEN_INTERVAL, regenAge);
        lastInterceptRollTurn = Integer.MIN_VALUE;
        lastHeroPosForTrapScan = Dungeon.hero == null ? -1 : Dungeon.hero.pos;
        lastRoomSignature = Integer.MIN_VALUE;
        warnedNearbySecretDoors.clear();
        warnedSecretRooms.clear();
    }
}
