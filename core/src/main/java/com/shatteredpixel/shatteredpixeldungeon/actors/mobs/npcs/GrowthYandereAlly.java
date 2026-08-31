package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.CorrosiveGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LifeLink;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Eye;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.RipperDemon;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

import java.util.HashSet;

/**
 * 정식 플레이용 성장형 얀데레.
 * AI/광란/대화 구조는 YandereAlly를 공유하고 성장형 전용 성장과 해금을 덮어쓴다.
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
    public static final int HEART_EMERGENCY_RECALL = 27;
    public static final int HEART_SMART_DODGE = 30;
    public static final int HEART_LOW_HP_II = 33;
    public static final int HEART_SECRET_II = 36;
    public static final int HEART_INTERCEPT_II = 39;
    public static final int HEART_COMBAT_TELEPORT = 42;
    public static final int HEART_SECRET_III = 45;

    private static final int SECRET_NEARBY_RANGE = 2;
    private static final int COMBAT_TELEPORT_RANGE = 4;

    private int growthHearts = 0;
    private float lastRegenClock = -1f;
    private int lastInterceptRollTurn = Integer.MIN_VALUE;

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

    public int growthHearts() { return growthHearts; }

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
        if (HT > oldHT) HP = Math.min(HT, HP + (HT - oldHT));
        else HP = Math.min(HP, HT);
    }

    private int clampHearts(int value) { return Math.max(0, Math.min(MAX_GROWTH_HEARTS, value)); }
    private int growthMaxHP() { return BASE_HP + HP_PER_HEART * growthHearts; }
    private int growthAttackMin() { return BASE_ATTACK_MIN + (growthHearts + 1) / 2; }
    private int growthAttackMax() { return BASE_ATTACK_MAX + growthHearts; }
    private int growthDrMin() { return growthHearts / 6; }
    private int growthDrMax() { return 2 + growthHearts / 3; }
    private int growthAccuracy() { return BASE_ACCURACY + 3 * (growthHearts / 6); }
    private int growthDefenseSkill() { return BASE_DEFENSE + 3 * (growthHearts / 6); }

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

    public boolean hasMissedHeartWarning() { return growthHearts >= HEART_MISSED_WARNING; }
    public boolean hasEmergencyRecall() { return growthHearts >= HEART_EMERGENCY_RECALL; }
    public boolean hasSmartDodge() { return growthHearts >= HEART_SMART_DODGE; }
    public boolean hasCombatTeleport() { return growthHearts >= HEART_COMBAT_TELEPORT; }

    private float lowHpCombatMultiplier() {
        if (Dungeon.hero == null || lowHpAwakeningStage() == 0) return 1f;
        float hpRatio = Dungeon.hero.HP / (float)Math.max(1, Dungeon.hero.HT);
        if (hpRatio > 0.30f) return 1f;
        if (lowHpAwakeningStage() == 1) return 1.20f;
        if (hpRatio <= 0.10f) return 1.80f;
        if (hpRatio <= 0.25f) return 1.50f;
        return 1.20f;
    }

    private static float globalGrowthClock() { return Statistics.duration + Actor.now(); }

    private void updateNaturalRegen() {
        float now = globalGrowthClock();
        if (lastRegenClock < 0f) {
            lastRegenClock = now;
            return;
        }
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
        if (chosen != null || hostileToHero() || mode() != MODE_GUARD || guardRange() <= 2) return chosen;
        return nearestEnemyInGrowthGuardRange();
    }

    private Char nearestEnemyInGrowthGuardRange() {
        if (Dungeon.level == null || Dungeon.hero == null) return null;
        Mob best = null;
        int bestDist = Integer.MAX_VALUE;
        for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
            if (mob == null || mob == this || !mob.isAlive()
                    || mob.alignment != Alignment.ENEMY || mob.isInvulnerable(getClass())) continue;
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
        if (mode() == MODE_PEACE && !emergencyProtect()) return false;
        int range = interceptStage() >= 2 ? 3 : 1;
        if (Dungeon.level.distance(pos, Dungeon.hero.pos) > range) return false;
        if (enemy != null && enemy.isAlive() && enemy.alignment == Alignment.ENEMY) return true;
        int threatRange = Math.max(guardRange(), 3);
        for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
            if (mob != null && mob != this && mob.isAlive() && mob.alignment == Alignment.ENEMY
                    && Dungeon.level.distance(Dungeon.hero.pos, mob.pos) <= threatRange) return true;
        }
        return false;
    }

    private boolean hasNormalLifeLink() {
        if (Dungeon.hero == null) return false;
        for (LifeLink link : Dungeon.hero.buffs(LifeLink.class)) if (!link.yandereGuardLink) return true;
        return false;
    }

    public void clearMyInterceptLinks() {
        if (Dungeon.hero == null) return;
        for (LifeLink link : Dungeon.hero.buffs(LifeLink.class).toArray(new LifeLink[0])) {
            if (link.yandereGuardLink && link.object == id()) link.detach();
        }
    }

    private void updateInterceptLinks() {
        if (Dungeon.hero == null) return;
        if (!canPrepareIntercept() || hasNormalLifeLink()) {
            clearMyInterceptLinks();
            lastInterceptRollTurn = Integer.MIN_VALUE;
            return;
        }
        int turnKey = (int)Math.floor(globalGrowthClock());
        if (turnKey == lastInterceptRollTurn) return;
        lastInterceptRollTurn = turnKey;
        clearMyInterceptLinks();
        int stage = interceptStage();
        float chance = stage >= 2 ? 0.50f : 0.30f;
        if (Random.Float() >= chance) return;
        int links = stage >= 2 ? 3 : 1;
        for (int i = 0; i < links; i++) LifeLink.attachYandereGuardLink(Dungeon.hero, id(), 1.25f);
    }

    // ---------- 27/30/42하트 고급 이동/전투 AI ----------

    /** 27하트 이후 리본의 '도움 요청하기'. 쿨다운은 없다. */
    public boolean requestHelp() {
        if (!hasEmergencyRecall() || hostileToHero() || Dungeon.hero == null || Dungeon.level == null) return false;

        if (Dungeon.level.distance(pos, Dungeon.hero.pos) > 1) {
            int cell = safeAdjacentCell(Dungeon.hero.pos);
            if (cell == -1) return false;
            ScrollOfTeleportation.appear(this, cell);
        }

        boolean wasPeace = mode() == MODE_PEACE;
        if (wasPeace) setMode(MODE_GUARD);
        else followHero();

        Char threat = nearestEnemyInGrowthGuardRange();
        if (threat != null) targetChar(threat);
        if (!wasPeace) yell("불렀어? 바로 왔어♡ 네 옆에 있을게. 이제 내가 막아줄게.");
        return true;
    }

    private boolean environmentalHazardAt(int cell) {
        if (Dungeon.level == null || cell < 0 || cell >= Dungeon.level.length()) return true;
        return Blob.volumeAt(cell, Fire.class) > 0
                || Blob.volumeAt(cell, ToxicGas.class) > 0
                || Blob.volumeAt(cell, CorrosiveGas.class) > 0;
    }

    private boolean telegraphedAttackAt(int cell) {
        if (Dungeon.level == null) return false;
        for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
            if (mob == null || !mob.isAlive() || mob.alignment != Alignment.ENEMY) continue;
            if (mob instanceof Eye && ((Eye)mob).chargedBeamThreatens(cell)) return true;
            if (mob instanceof RipperDemon && ((RipperDemon)mob).leapThreatens(cell)) return true;
        }
        return false;
    }

    private boolean smartDangerAt(int cell) {
        return environmentalHazardAt(cell) || telegraphedAttackAt(cell);
    }

    private boolean safeOpenCell(int cell) {
        if (Dungeon.level == null || cell < 0 || cell >= Dungeon.level.length()) return false;
        if (!Dungeon.level.insideMap(cell) || !Dungeon.level.passable[cell] || Dungeon.level.pit[cell]) return false;
        if (Actor.findChar(cell) != null) return false;
        return !hasSmartDodge() || !smartDangerAt(cell);
    }

    private int safeAdjacentCell(int center) {
        if (Dungeon.level == null) return -1;
        int[] dirs = PathFinder.NEIGHBOURS8.clone();
        Random.shuffle(dirs);
        for (int off : dirs) {
            int cell = center + off;
            if (safeOpenCell(cell)) return cell;
        }
        return -1;
    }

    private boolean trySmartEmergencyDodge() {
        if (!hasSmartDodge() || Dungeon.level == null || rooted || !smartDangerAt(pos)) return false;
        int cell = safeAdjacentCell(pos);
        if (cell == -1) return false;
        int oldPos = pos;
        move(cell);
        spend(1f / speed());
        moveSprite(oldPos, pos);
        return true;
    }

    @Override
    protected boolean getCloser(int target) {
        if (!hasSmartDodge() || Dungeon.level == null) return super.getCloser(target);
        if (rooted || target == pos || !Dungeon.level.insideMap(target)) return false;

        boolean[] safe = Dungeon.level.passable.clone();
        for (int cell = 0; cell < safe.length; cell++) {
            if (safe[cell] && smartDangerAt(cell)) safe[cell] = false;
        }

        PathFinder.Path path = Dungeon.findPath(this, target, safe, fieldOfView, true);
        if (path == null || path.isEmpty()) return false;
        int step = path.removeFirst();
        if (step < 0 || step >= safe.length || !safe[step] || Actor.findChar(step) != null) return false;
        move(step);
        return true;
    }

    private boolean tryCombatTeleport() {
        if (!hasCombatTeleport() || hostileToHero() || emergencyProtect()
                || mode() == MODE_PEACE || Dungeon.level == null) return false;

        Char targetChar = chooseEnemy();
        if (targetChar == null || !targetChar.isAlive()) return false;
        int distance = Dungeon.level.distance(pos, targetChar.pos);
        if (distance <= 1 || distance > COMBAT_TELEPORT_RANGE) return false;

        int cell = safeAdjacentCell(targetChar.pos);
        if (cell == -1) return false;

        ScrollOfTeleportation.appear(this, cell);
        targetChar(targetChar);
        spend(1f / speed());
        return true;
    }

    // ---------- 탐색 성장 ----------

    private void updateExplorationSense() {
        if (Dungeon.level == null || Dungeon.hero == null || hostileToHero()) return;
        int stage = secretSenseStage();
        if (stage <= 0) {
            lastHeroPosForTrapScan = Dungeon.hero.pos;
            return;
        }
        warnNearbySecretDoors();
        if (stage >= 3) warnOnSecretRoomEntry();
        boolean heroStayedStill = lastHeroPosForTrapScan == Dungeon.hero.pos;
        if (stage >= 2 && heroStayedStill) revealNearbyHiddenTraps(stage >= 3 ? 3 : 2);
        lastHeroPosForTrapScan = Dungeon.hero.pos;
    }

    private void warnNearbySecretDoors() {
        boolean foundNew = false;
        for (int cell = 0; cell < Dungeon.level.length(); cell++) {
            if (Dungeon.level.map[cell] == Terrain.SECRET_DOOR
                    && Dungeon.level.distance(Dungeon.hero.pos, cell) <= SECRET_NEARBY_RANGE
                    && warnedNearbySecretDoors.add(cell)) foundNew = true;
        }
        if (foundNew) yell("잠깐♡ 이 근처, 뭔가 이상해. 그냥 지나가지 마.");
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
        if (found) yell("잠깐. 근처에 함정 있어. 밟지 마.");
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
            if (cell >= 0 && cell < Dungeon.level.length() && Dungeon.level.map[cell] == Terrain.SECRET_DOOR) {
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
        if (trySmartEmergencyDodge()) return true;
        if (tryCombatTeleport()) return true;

        int hpBeforeParentAct = HP;
        boolean wasFriendly = !hostileToHero();
        boolean result = super.act();

        if (wasFriendly && HP > hpBeforeParentAct) HP = Math.min(HT, hpBeforeParentAct);
        updateNaturalRegen();
        updateInterceptLinks();
        updateExplorationSense();
        return result;
    }

    @Override
    public void setMode(int newMode) {
        super.setMode(newMode);
        if (newMode == MODE_PEACE && !emergencyProtect()) clearMyInterceptLinks();
    }

    @Override
    public void damage(int dmg, Object src) {
        boolean intercepted = src instanceof LifeLink
                && ((LifeLink)src).yandereGuardLink
                && ((LifeLink)src).object == id();
        super.damage(dmg, src);
        if (intercepted) clearMyInterceptLinks();
    }

    @Override
    public void die(Object cause) {
        clearMyInterceptLinks();
        super.die(cause);
    }

    @Override
    public int attackSkill(Char target) { return hostileToHero() ? 1_000_000 : growthAccuracy(); }

    @Override
    public int damageRoll() {
        if (hostileToHero() && Dungeon.hero != null) return Math.max(9999, Dungeon.hero.HT * 4);
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
        return growthHearts >= HEART_INTERCEPT_II ? 1.2f : 1f;
    }

    @Override
    public float attackDelay() { return 1f; }

    @Override
    public int statAttackMin() {
        if (hostileToHero()) return Math.max(9999, Dungeon.hero == null ? 9999 : Dungeon.hero.HT * 4);
        return Math.max(0, Math.round(growthAttackMin() * lowHpCombatMultiplier()));
    }

    @Override
    public int statAttackMax() {
        return hostileToHero() ? statAttackMin() : Math.max(0, Math.round(growthAttackMax() * lowHpCombatMultiplier()));
    }

    @Override
    public int statDrMin() { return Math.max(0, Math.round(growthDrMin() * lowHpCombatMultiplier())); }

    @Override
    public int statDrMax() { return Math.max(0, Math.round(growthDrMax() * lowHpCombatMultiplier())); }

    @Override
    public int statAccuracy() { return hostileToHero() ? 1_000_000 : growthAccuracy(); }

    @Override
    public int statDefenseSkill() { return growthDefenseSkill(); }

    @Override
    public float statMoveSpeed() { return hostileToHero() ? 1f : (growthHearts >= HEART_INTERCEPT_II ? 1.2f : 1f); }

    @Override
    public float statAttackSpeed() { return 1f; }

    public float statRegenPercent() {
        if (growthHearts >= HEART_REGEN_II) return 0.03f;
        if (growthHearts >= HEART_REGEN_I) return 0.02f;
        return BASE_REGEN_PERCENT;
    }

    public float statRegenInterval() { return REGEN_INTERVAL; }

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
        if (bundle.contains(GROWTH_HEARTS)) growthHearts = clampHearts(bundle.getInt(GROWTH_HEARTS));
        HT = growthMaxHP();
        defenseSkill = growthDefenseSkill();
        HP = Math.min(HP, HT);
        float regenAge = bundle.contains(REGEN_AGE) ? Math.max(0f, bundle.getFloat(REGEN_AGE)) : 0f;
        lastRegenClock = globalGrowthClock() - Math.min(REGEN_INTERVAL, regenAge);
        lastInterceptRollTurn = Integer.MIN_VALUE;
        lastHeroPosForTrapScan = Dungeon.hero == null ? -1 : Dungeon.hero.pos;
        lastRoomSignature = Integer.MIN_VALUE;
        warnedNearbySecretDoors.clear();
        warnedSecretRooms.clear();
    }
}
