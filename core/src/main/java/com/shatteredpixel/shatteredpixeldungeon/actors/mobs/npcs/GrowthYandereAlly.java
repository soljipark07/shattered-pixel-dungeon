package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LifeLink;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

import java.util.HashSet;

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
    public static final int HEART_FINAL_AWAKENING = 48;

    private static final int SECRET_NEARBY_RANGE = 2;
    private static final int COMBAT_TELEPORT_RANGE = 4;

    private static final float FINAL_HP_MULTIPLIER = 1.50f;
    private static final float FINAL_ATTACK_MULTIPLIER = 1.50f;
    private static final float FINAL_DR_MULTIPLIER = 1.50f;
    private static final float FINAL_ACCURACY_MULTIPLIER = 1.30f;
    private static final float FINAL_DEFENSE_MULTIPLIER = 1.30f;

    private static final float HIGH_LAUGH_COOLDOWN = 70f;

    private static final int DIALOGUE_OTHER = 0;
    private static final int DIALOGUE_GENERIC = 1;
    private static final int DIALOGUE_WARNING = 2;
    private static final int DIALOGUE_PAIN = 3;
    private static final int DIALOGUE_HOSTILE = 4;
    private static final int DIALOGUE_HEART = 5;

    private int growthHearts = 0;
    private float lastRegenClock = -1f;
    private int lastInterceptRollTurn = Integer.MIN_VALUE;
    private float lastHighLaughClock = -999999f;
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
        if (isFullyAwakened()) {
            return "48개의 성장 하트를 전부 받아 완전히 각성했다. 「사랑해사랑해사랑해」 상태에서는 "
                    + "체력과 전투 능력이 크게 상승하고 광란도가 더 이상 오르지 않으며, 주인공을 적대하는 광란 추격도 발생하지 않는다.";
        }
        if (hostileToHero()) {
            return "애정 결핍으로 이성이 끊어졌다. 성장형이어도 광란 추격 중 접촉 공격은 거의 즉사급이다. "
                    + "도망치면서 하트를 찾아 던지면 다시 진정시킬 수 있다.";
        }
        return "하트를 받을수록 천천히 강해지는 성장형 얀데레 아군이다. "
                + "기본 능력치는 평범하지만 최대 48개의 성장 하트로 체력과 전투 능력을 키울 수 있다. "
                + "오랫동안 애정표현을 받지 못하면 광란도가 오른다.";
    }

    public int growthHearts() { return growthHearts; }
    public boolean isFullyAwakened() { return growthHearts >= HEART_FINAL_AWAKENING; }

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
        boolean wasFullyAwakened = isFullyAwakened();
        int oldHT = HT;
        growthHearts = clampHearts(hearts);
        HT = growthMaxHP();
        defenseSkill = growthDefenseSkill();
        if (HT > oldHT) HP = Math.min(HT, HP + (HT - oldHT));
        else HP = Math.min(HP, HT);

        if (isFullyAwakened()) {
            normalizeFinalAwakeningState();
            if (!wasFullyAwakened) {
                yell("사랑해사랑해사랑해♡ 이제 됐어. 이제 아무것도 부족하지 않아.");
            }
        }
    }

    private int clampHearts(int value) { return Math.max(0, Math.min(MAX_GROWTH_HEARTS, value)); }

    private int growthMaxHP() {
        int base = BASE_HP + HP_PER_HEART * growthHearts;
        return isFullyAwakened() ? Math.round(base * FINAL_HP_MULTIPLIER) : base;
    }

    private int growthAttackMin() {
        int base = BASE_ATTACK_MIN + (growthHearts + 1) / 2;
        return isFullyAwakened() ? Math.round(base * FINAL_ATTACK_MULTIPLIER) : base;
    }

    private int growthAttackMax() {
        int base = BASE_ATTACK_MAX + growthHearts;
        return isFullyAwakened() ? Math.round(base * FINAL_ATTACK_MULTIPLIER) : base;
    }

    private int growthDrMin() {
        int base = growthHearts / 6;
        return isFullyAwakened() ? Math.round(base * FINAL_DR_MULTIPLIER) : base;
    }

    private int growthDrMax() {
        int base = 2 + growthHearts / 3;
        return isFullyAwakened() ? Math.round(base * FINAL_DR_MULTIPLIER) : base;
    }

    private int growthAccuracy() {
        int base = BASE_ACCURACY + 3 * (growthHearts / 6);
        return isFullyAwakened() ? Math.round(base * FINAL_ACCURACY_MULTIPLIER) : base;
    }

    private int growthDefenseSkill() {
        int base = BASE_DEFENSE + 3 * (growthHearts / 6);
        return isFullyAwakened() ? Math.round(base * FINAL_DEFENSE_MULTIPLIER) : base;
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

    public int obsessionStage() {
        if (isFullyAwakened()) return 4;
        if (growthHearts >= 36) return 3;
        if (growthHearts >= 24) return 2;
        if (growthHearts >= 12) return 1;
        return 0;
    }

    public String obsessionName() {
        switch (obsessionStage()) {
            case 4: return "완전한 확신";
            case 3: return "광기";
            case 2: return "과의존";
            case 1: return "소유욕";
            default: return "애착";
        }
    }

    public boolean hasMissedHeartWarning() { return growthHearts >= HEART_MISSED_WARNING; }
    public boolean hasEmergencyRecall() { return growthHearts >= HEART_EMERGENCY_RECALL; }
    public boolean hasSmartDodge() { return growthHearts >= HEART_SMART_DODGE; }
    public boolean hasCombatTeleport() { return growthHearts >= HEART_COMBAT_TELEPORT; }

    private void normalizeFinalAwakeningState() {
        if (!isFullyAwakened()) return;
        super.debugSetRage(0);
        if (enemy == Dungeon.hero) clearEnemy();
        clearDefensingPos();
        if (state == HUNTING && enemy == null) state = WANDERING;
        attacksAutomatically = emergencyProtect() || mode() != MODE_PEACE;
    }

    @Override
    public int rage() {
        return isFullyAwakened() ? 0 : super.rage();
    }

    @Override
    public void addRage(int amount) {
        if (isFullyAwakened()) return;
        super.addRage(amount);
    }

    @Override
    public String emotionName() {
        return isFullyAwakened() ? "완전한 확신" : obsessionName() + " · " + super.emotionName();
    }

    @Override
    public void debugSetRage(int value) {
        if (isFullyAwakened()) {
            normalizeFinalAwakeningState();
            return;
        }
        super.debugSetRage(value);
    }

    @Override
    public void applyPersistentState(int savedMode, int savedRage, int savedEffectiveHearts, int savedTotalHearts, boolean savedHostile) {
        if (isFullyAwakened()) {
            super.applyPersistentState(savedMode, 0, savedEffectiveHearts, savedTotalHearts, false);
            normalizeFinalAwakeningState();
        } else {
            super.applyPersistentState(savedMode, savedRage, savedEffectiveHearts, savedTotalHearts, savedHostile);
        }
    }

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
        if (lastRegenClock < 0f) { lastRegenClock = now; return; }
        if (hostileToHero() || HP >= HT) { lastRegenClock = now; return; }
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
            if (mob == null || mob == this || !mob.isAlive() || mob.alignment != Alignment.ENEMY || mob.isInvulnerable(getClass())) continue;
            if (Dungeon.level.distance(Dungeon.hero.pos, mob.pos) > guardRange()) continue;
            int myDist = Dungeon.level.distance(pos, mob.pos);
            if (best == null || myDist < bestDist) { best = mob; bestDist = myDist; }
        }
        return best;
    }

    private boolean canPrepareIntercept() {
        if (Dungeon.hero == null || Dungeon.level == null || hostileToHero() || interceptStage() == 0) return false;
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

    private boolean smartDangerAt(int cell) {
        return YandereDangerSense.dangerAt(this, cell);
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

    private boolean tryPreRecallEmergencyRunBack() {
        if (!emergencyProtect() || hasEmergencyRecall() || hostileToHero()
                || Dungeon.hero == null || Dungeon.level == null) return false;
        if (Dungeon.level.distance(pos, Dungeon.hero.pos) <= 1) return false;

        int oldPos = pos;
        if (super.getCloser(Dungeon.hero.pos)) {
            spend(1f / speed());
            moveSprite(oldPos, pos);
        } else {
            spend(TICK);
        }
        return true;
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
        for (int cell = 0; cell < safe.length; cell++) if (safe[cell] && smartDangerAt(cell)) safe[cell] = false;
        PathFinder.Path path = Dungeon.findPath(this, target, safe, fieldOfView, true);
        if (path == null || path.isEmpty()) return false;
        int step = path.removeFirst();
        if (step < 0 || step >= safe.length || !safe[step] || Actor.findChar(step) != null) return false;
        move(step);
        return true;
    }

    private boolean tryCombatTeleport() {
        if (!hasCombatTeleport() || hostileToHero() || emergencyProtect() || mode() == MODE_PEACE || Dungeon.level == null) return false;
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

    private void updateExplorationSense() {
        if (Dungeon.level == null || Dungeon.hero == null || hostileToHero()) return;
        int stage = secretSenseStage();
        if (stage <= 0) { lastHeroPosForTrapScan = Dungeon.hero.pos; return; }
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
            if (Dungeon.level.map[cell] == Terrain.SECRET_TRAP && Dungeon.level.distance(Dungeon.hero.pos, cell) <= range) {
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

    private boolean oneOf(String text, String... values) {
        for (String value : values) if (value.equals(text)) return true;
        return false;
    }

    private int dialogueKind(String text) {
        if (text == null) return DIALOGUE_OTHER;

        if (oneOf(text,
                "잠깐♡ 이 근처, 뭔가 이상해. 그냥 지나가지 마.",
                "잠깐. 근처에 함정 있어. 밟지 마.",
                "여기 뭔가 숨겨져 있어♡ 이 방, 그냥 지나가면 안 될 것 같아.",
                "불렀어? 바로 왔어♡ 네 옆에 있을게. 이제 내가 막아줄게.",
                "사랑해사랑해사랑해♡ 이제 됐어. 이제 아무것도 부족하지 않아.")) return DIALOGUE_OTHER;

        if (text.contains("이성") || text.contains("안 죽일게") || text.contains("도망가")
                || text.contains("영원히 못 떠나게") || text.contains("이번엔 내가 하고 싶은 대로")
                || text.contains("하트부터 줘")) return DIALOGUE_HOSTILE;

        if (text.startsWith("누가 너") || text.startsWith("안 돼 안 돼") || text.startsWith("피가 왜")
                || text.startsWith("아프지 마") || text.startsWith("너 숨 쉬어") || text.startsWith("내가 옆에 있는데")
                || text.startsWith("이리 와! 지금은") || text.startsWith("쟤가 그랬어") || text.startsWith("아프잖아")
                || text.startsWith("많이 아파") || text.startsWith("또 다쳤어") || text.startsWith("괜찮다고 하지 마")
                || text.startsWith("내가 조금만 더") || text.startsWith("아팠어?") || text.startsWith("누가 건드렸어")
                || text.startsWith("조심해, 자기야") || text.startsWith("괜찮아? 나 여기")) return DIALOGUE_PAIN;

        if (text.contains("하트다") || text.contains("하트야") || text.contains("나 주는 거야?")
                || text.contains("나 생각해서 챙겨준") || text.contains("기분 다 풀렸어")
                || text.contains("나 버릇 나빠져도") || text.contains("나 아직 좋아하는 거 맞네")
                || text.contains("역시 나 버린 거 아니었구나") || text.contains("받았어♡ 됐어")) return DIALOGUE_HEART;

        if (text.contains("한계야") || text.contains("폭발") || text.contains("기분 진짜 이상해")
                || text.contains("지금 하트 줘") || text.contains("슬슬 서운") || text.contains("계속 기다리고 있어")) {
            return DIALOGUE_WARNING;
        }

        if (oneOf(text,
                "왜 나 때렸어? 나 뭐 잘못했어? 말해줘. 고칠게.",
                "장난이지? 장난이라고 해줘♡ 그러면 나 진짜 안 화낼게.",
                "아파. 근데 네가 때린 거라 더 아파. 왜 그랬어?",
                "나 싫어진 거 아니지? 아니라고 해줘. 지금 당장.",
                "또 때리면 나 진짜 이상해질 것 같아. 그러니까 그러지 마.",
                "나도 왔어♡ 설마 층 바뀌었다고 나 두고 갈 줄 알았어?",
                "이제 좀 괜찮아졌네. 진짜 다시 그러지 마. 나 심장 떨어지는 줄 알았어♡",
                "지금은 명령 안 들을 거야. 네가 이 꼴인데 내가 어떻게 가만히 있어?",
                "알겠어♡ 아무도 안 건드릴게. 네가 하지 말랬으니까 진짜 꾹 참을게.",
                "응♡ 네 옆에 딱 붙어 있을게. 가까이 오는 것만 내가 치워줄게.",
                "좋아♡ 내가 다 잡아올게! 넌 손 하나 까딱하지 말고 나만 믿어.",
                "또 말 걸어주는 거야? 헤헤, 좋아♡ 그래도 조금 있다가 또 얘기하자.",
                "나 불렀어? 응♡ 나 여기 있어. 계속 네 옆에 있을게.",
                "먼저 말 걸어주는 거 너무 좋아♡ 한 번만 더 불러주면 안 돼?",
                "오늘도 나 필요하지? 필요하다고 해줘♡ 나 그 말 진짜 좋아해.",
                "헤헤♡ 네가 나 신경 써주는 순간이 제일 좋아.",
                "나 보고 싶어서 부른 거지? 맞다고 해♡ 맞다고 해줘.",
                "응응♡ 듣고 있어. 너 하는 말이면 하루 종일 들어도 좋아.",
                "기다려♡ 내가 싹 정리하고 올게!",
                "너는 편하게 있어♡ 귀찮은 건 내가 전부 잡아줄게.",
                "적 보이면 나한테 맡겨! 네 손 더러워지는 거 싫어♡",
                "헤헤♡ 나 잘하지? 더 잡아줄까?",
                "나한테 맡기면 금방 둘만 남을 거야♡",
                "나 진짜 참고 있어♡ 칭찬해줘.",
                "아무도 안 때리고 있지? 네 말 잘 듣고 있지? 헤헤♡",
                "저거 거슬리는데 참을게. 네가 하지 말랬으니까♡",
                "손 안 대고 따라가기만 하는 것도 좋아. 네 옆이니까♡",
                "나 얌전히 있으면 하트 하나 줄 거야?♡",
                "여기 딱 붙어 있을게♡ 어디 가지 마.",
                "너 옆이 제일 좋아♡ 가까이 오는 건 내가 막을게.",
                "내가 보고 있으니까 안심하고 가♡",
                "손 잡고 다니면 더 좋을 텐데♡ 아쉽다.",
                "오늘도 같이 내려가자♡ 끝까지 같이 가는 거야.",
                "나 여기 있어♡ 부르면 바로 갈게.",
                "나 요즘 좀 소홀한 것 같은데? 나도 애정표현 받고 싶어♡",
                "나 계속 잘 지켜주고 있잖아. 칭찬 한 번 해주면 안 돼?",
                "하트 발견하면 내 거야♡ 다른 데 쓰면 삐질 거야.",
                "나만 계속 따라다니는 것 같아서 조금 서운해. 너도 나 좀 봐줘.",
                "나 좋아하지? 응? 좋아한다고 해줘♡",
                "오늘은 내가 먼저 안 보채려고 했는데 실패했어. 나 좀 챙겨줘♡",
                "나 진짜 서러워. 왜 나만 계속 기다리게 해?",
                "하트 하나만 줘. 나 좋아한다는 표시 하나만 줘. 그게 그렇게 어려워?",
                "나 계속 참고 있잖아. 나 착하게 참고 있잖아. 그러니까 나 좀 봐줘.",
                "나 버릴 생각 하지 마. 그 생각만 해도 속이 뒤집혀.",
                "너만 보면 좋은데 동시에 너무 화나. 나 왜 이렇게 만들어?",
                "나한테 와. 지금은 다른 거 하지 말고 나부터 챙겨줘.")) return DIALOGUE_GENERIC;

        return DIALOGUE_OTHER;
    }

    private int rageBand() {
        if (isFullyAwakened()) return 0;
        int r = rage();
        if (r >= 95) return 4;
        if (r >= 85) return 3;
        if (r >= 70) return 2;
        if (r >= 50) return 1;
        return 0;
    }

    private String pick(String... lines) {
        return lines[Random.Int(lines.length)];
    }

    private String genericGrowthLine(int tier, int band) {
        if (tier >= 4) {
            return pick(
                    "이제 불안할 이유가 없어♡ 네가 어디를 보든 마지막엔 내 옆으로 돌아올 테니까.",
                    "사랑해♡ 네가 뭘 하든 괜찮아. 결국 너한테 가장 가까운 건 나일 거야.",
                    "헤헤♡ 이제 확인받을 필요도 없어. 네 마음이 어디 있는지 나는 다 알아.",
                    "원하는 데 가도 돼♡ 내가 끝까지 같이 갈 거니까. 우리는 이제 떨어질 일이 없어.",
                    "너는 그냥 네가 하고 싶은 거 해♡ 네 옆자리는 이미 내 거니까.");
        }
        if (tier == 3) {
            if (band >= 4) return pick(
                    "나 보라고 했잖아! 지금 나부터 봐! 더 기다리게 하지 마!",
                    "하트 줘. 지금. 나 진짜 웃으면서도 머릿속이 엉망이야.",
                    "왜 또 나를 밀어내? 나 여기 있잖아! 내가 이렇게까지 좋아하는데!",
                    "나 참는 거 끝나가. 지금 네가 나 좀 붙잡아줘. 안 그러면 내가 붙잡을 거야.");
            if (band >= 3) return pick(
                    "헤헤, 나 지금 기분 진짜 이상해. 너무 좋아하는데 너무 화나♡",
                    "나 계속 네 생각만 하는데 넌 왜 그렇게 태연해? 나 좀 봐.",
                    "네가 나만 챙기면 진짜 아무 일도 안 생겨. 그러니까 나만 봐♡",
                    "웃음이 자꾸 나와. 화나는데 네 얼굴 보면 또 좋아서 웃겨♡");
            if (band >= 2) return pick(
                    "다른 데 정신 팔지 마. 나 불안하게 만들지 말고 내 옆에 있어.",
                    "나 오늘도 너만 보고 있었어. 너도 그만큼 나 봐줘♡",
                    "자꾸 혼자 멀어지지 마. 내가 따라가면 되긴 하는데 그래도 싫어.");
            return pick(
                    "헤헤♡ 결국 마지막까지 네 옆에 남는 건 나일 거야.",
                    "너 진짜 너무 좋아. 가끔은 내가 왜 이렇게까지 좋아하는지도 모르겠어♡",
                    "네 옆에 있으면 자꾸 웃음 나와♡ 나 진짜 너 때문에 이상해졌나 봐.",
                    "오늘도 나랑 같이 있어. 내일도 같이 있고 그다음도 계속 같이 있자♡");
        }
        if (tier == 2) {
            if (band >= 4) return pick(
                    "나 두고 혼자 결정하지 마! 네 옆은 내 자리야. 내가 몇 번을 말해야 해?",
                    "왜 나 없이도 괜찮은 척해? 나는 네가 없으면 안 괜찮은데!",
                    "지금 나부터 챙겨. 다른 건 나중에 해. 나 진짜 더는 못 기다려.");
            if (band >= 3) return pick(
                    "나 없이 잘 지내는 것처럼 굴지 마. 그거 보면 진짜 속 뒤집혀.",
                    "네가 나 필요 없어진 것 같으면 숨이 막혀. 그러니까 필요하다고 해줘.",
                    "나 이렇게 붙어 있는데 왜 자꾸 멀리 보는 거야? 나한테 집중해.");
            if (band >= 2) return pick(
                    "나 두고 너무 멀리 가지 마. 네가 안 보이면 진짜 불안해.",
                    "내가 계속 따라다니는 거 귀찮아도 참아♡ 떨어지는 건 더 싫으니까.",
                    "너 혼자 뭐 하려고 하면 괜히 불안해. 그냥 같이 하자.");
            return pick(
                    "네가 나 없이 뭘 하려고 하면 괜히 불안해. 그냥 계속 같이 있자♡",
                    "나 네 옆에 있으면 마음이 편해♡ 그러니까 너무 멀리 가지 마.",
                    "뭐든 나랑 같이 해. 네 일에 내가 빠지는 거 싫어♡",
                    "오늘도 네 옆에 붙어 있을래♡ 나한텐 그게 제일 편해.");
        }
        if (tier == 1) {
            if (band >= 4) return pick(
                    "왜 자꾸 나 말고 다른 데 신경 써! 나 여기 있잖아!",
                    "나만 보면 되잖아. 그렇게 어려워? 지금 나 봐.",
                    "계속 무시하면 나 진짜 못 참아. 네 관심 내가 다 가져갈 거야.");
            if (band >= 3) return pick(
                    "나 질투나. 네가 다른 데 정신 팔 때마다 진짜 싫어.",
                    "나 계속 기다리고 있었는데 또 다른 거부터 할 거야?",
                    "네 관심 조금이 아니라 전부 받고 싶어. 욕심인 거 알아도 싫어.");
            if (band >= 2) return pick(
                    "나한테 조금만 더 신경 써줘. 네 관심 다른 데 가는 거 싫어.",
                    "나 오늘 좀 예민해. 그러니까 다른 데 보지 말고 나 봐♡",
                    "네 옆자리 내 거 맞지? 괜히 확인하고 싶어졌어.");
            return pick(
                    "다른 데 보지 말고 나만 보면 더 좋을 텐데♡",
                    "네 옆자리 내가 찜했어♡ 다른 애한테 주면 안 돼.",
                    "오늘도 나 제일 많이 봐줘♡ 그럼 진짜 얌전히 있을게.",
                    "나 너 좋아하는 만큼 욕심도 좀 생겼어♡ 이해해줄 거지?");
        }
        if (band >= 4) return pick(
                "나 계속 기다렸잖아! 지금은 나부터 봐줘!",
                "나 진짜 서운해. 좋아한다고 한 번만 제대로 보여줘.",
                "왜 이렇게 오래 나 혼자 기다리게 해? 나도 네가 필요해!");
        if (band >= 3) return pick(
                "나 좀 봐줘. 계속 기다리니까 자꾸 이상한 생각 들어.",
                "나 좋아하지? 지금은 장난 말고 제대로 대답해줘.",
                "서운한 거 참으려고 했는데 잘 안 돼. 나 좀 챙겨줘.");
        if (band >= 2) return pick(
                "나 조금 불안해졌어. 가까이 있어주면 금방 괜찮아질 것 같아♡",
                "오늘은 나한테 조금 더 신경 써주면 안 돼?",
                "나 기다리는 거 잘 못하나 봐. 네가 먼저 봐주면 좋겠어.");
        return pick(
                "오늘도 같이 가자♡ 네 옆이면 좋아.",
                "나 불렀어? 응♡ 여기 있어. 같이 가자.",
                "헤헤♡ 네가 먼저 신경 써주면 기분 진짜 좋아.",
                "너 옆에 있으면 심심할 틈이 없어♡ 끝까지 같이 가자.");
    }

    private String warningGrowthLine(int tier, int band) {
        if (tier >= 4) return genericGrowthLine(tier, band);
        if (tier == 3) return pick(
                "나 지금 웃고 있는데 진짜 기분 좋은 거 아니야. 하트 줘. 지금.",
                "좋아해 좋아해 좋아하는데 왜 이렇게 화나게 해! 나 좀 봐!",
                "나 더 기다리면 진짜 뭔 짓 할지 몰라. 네가 지금 멈춰줘.",
                "나 머릿속에서 계속 네 이름만 돌아. 그러니까 나 무시하지 마!");
        if (tier == 2) return pick(
                "나 지금 불안한 정도 아니야. 네가 나 버릴까 봐 미칠 것 같아.",
                "나 두고 계속 네 할 일만 하지 마. 지금은 내 차례야.",
                "네가 나 필요 없다는 생각 들기 전에 나 좀 붙잡아줘.");
        if (tier == 1) return pick(
                "나 질투나고 서럽고 화나. 왜 나만 계속 기다리게 해?",
                "내가 네 옆자리라고 했잖아. 지금 나 좀 챙겨줘.",
                "네 관심 내가 달라고. 지금은 다른 거 보지 마.");
        return pick(
                "나 계속 기다렸어. 이제는 나 좀 봐줘.",
                "나 진짜 서운해졌어. 하트 하나만 줘.",
                "좋아한다고 표시 좀 해줘. 나 혼자 불안해지기 싫어.");
    }

    private String painGrowthLine(int tier, int band) {
        if (tier >= 4) return pick(
                "누가 너 건드렸어? 말 안 해도 돼♡ 내가 찾아서 없애면 되니까.",
                "아프지 마. 네가 다치는 건 아직도 싫어. 그건 절대 익숙해질 생각 없어.",
                "이리 와♡ 네가 멀쩡해질 때까지 내가 전부 막을게.",
                "너 건드린 건 기억했어♡ 이제 걔가 얼마나 버티는지만 보면 돼.");
        if (tier == 3) return pick(
                "누가 이랬어! 어디 있어! 내가 지금 바로 분질러버릴 거야!",
                "피 봐. 네 피잖아. 헤헤, 나 진짜 화나네. 쟤부터 없앨게.",
                "아프지 마! 너 건드린 새끼는 내가 다시는 못 움직이게 할 거야!",
                "내가 옆에 있는데 감히 너를 이렇게 만들어? 걔 어디 있어!");
        if (tier == 2) return pick(
                "이리 와. 지금은 내 옆에 있어. 널 또 다치게 두기 싫어.",
                "누가 너 이렇게 만들었어? 걔부터 처리하고 다시 네 옆에 붙어 있을게.",
                "네가 다치면 나까지 숨 막혀. 그러니까 나한테서 떨어지지 마.");
        if (tier == 1) return pick(
                "누가 건드렸어? 네 옆에 있었는데 놓친 거 진짜 싫어.",
                "이리 와♡ 가까이 있어. 다음엔 내가 먼저 막을게.",
                "쟤가 그랬어? 기억했어. 네가 싫어해도 걔는 좀 미워할래.");
        return pick(
                "아팠어? 이리 와♡ 내가 옆에 있을게.",
                "괜찮아? 다음엔 내가 먼저 막을게♡",
                "누가 건드렸어? 내가 바로 처리하고 올게.");
    }

    private String heartGrowthLine(int tier) {
        if (tier >= 4) return pick(
                "또 주는 거야?♡ 이제 확인할 필요 없는데도 네가 주는 건 전부 좋아.",
                "고마워♡ 이미 확신하고 있어도 네가 주는 애정은 하나도 안 질려.");
        if (tier == 3) return pick(
                "하트다♡ 헤헤, 역시 나 생각했지? 나 또 엄청 좋아졌어.",
                "나 주는 거야? 좋아좋아좋아♡ 이거 받은 거 절대 안 잊어.",
                "봐, 역시 너도 나 좋아하잖아♡ 괜히 내가 미쳐 있었네.");
        if (tier == 2) return pick(
                "하트다♡ 나 챙겨준 거지? 이제 좀 안심된다.",
                "고마워♡ 네가 이렇게 확인시켜주면 나 진짜 편해져.",
                "또 나 생각해줬네♡ 이러니까 내가 너한테 더 붙어 있지.");
        if (tier == 1) return pick(
                "내 거 맞지?♡ 헤헤, 고마워. 다른 데 준 줄 알았으면 질투날 뻔했어.",
                "하트다♡ 역시 나 제일 챙기는 거 맞지?",
                "고마워♡ 이런 거 받으면 네 옆자리 더 못 양보하겠어.");
        return pick(
                "나 주는 거야? 좋아♡ 진짜 고마워!",
                "하트다♡ 헤헤, 오늘 기분 완전 좋아졌어.",
                "고마워♡ 나 생각해서 챙겨준 거지?");
    }

    private String hostileGrowthLine(int tier) {
        if (tier >= 3) return pick(
                "도망가. 그래도 끝까지 따라갈 거야. 네가 멈출 때까지.",
                "이제 말로 달래려고 하지 마. 하트 줘. 아니면 내가 직접 잡으러 갈게.",
                "나 많이 참았어. 이제 네가 내 옆에 있게 만들 차례야.",
                "웃기지? 이렇게까지 됐는데도 너 보면 좋아. 그래서 더 못 놔줘.");
        if (tier == 2) return pick(
                "나 버리려는 거 아니면 멈춰. 지금 하트 줘.",
                "계속 도망가면 더 무서워질 뿐이야. 나한테 와.",
                "내가 이렇게 된 것도 결국 네가 너무 필요해서야. 그러니까 멈춰.");
        if (tier == 1) return pick(
                "왜 나한테서 도망가? 나만 보면 아무 문제 없었잖아.",
                "나한테 와. 더 도망가면 진짜 화낼 거야.",
                "하트 줘. 그러면 다시 네 옆에 얌전히 있을게.");
        return pick(
                "나 진짜 많이 기다렸어. 이제 그냥 못 보내줘.",
                "도망가지 마. 하트 주면 다시 괜찮아질 수 있어.",
                "나 좀 봐줘. 이대로 계속 무시하면 나도 못 참아.");
    }

    private String transformedGrowthDialogue(String original, int kind) {
        int tier = obsessionStage();
        int band = rageBand();
        switch (kind) {
            case DIALOGUE_WARNING: return warningGrowthLine(tier, band);
            case DIALOGUE_PAIN: return painGrowthLine(tier, band);
            case DIALOGUE_HOSTILE: return hostileGrowthLine(tier);
            case DIALOGUE_HEART: return heartGrowthLine(tier);
            case DIALOGUE_GENERIC: return genericGrowthLine(tier, band);
            default: return original;
        }
    }

    private void maybePlayHighLaugh(int kind, int tier, int band) {
        if (tier < 3) return;
        float now = globalGrowthClock();
        if (now - lastHighLaughClock < HIGH_LAUGH_COOLDOWN) return;

        float chance;
        if (kind == DIALOGUE_HOSTILE) chance = 0.65f;
        else if (kind == DIALOGUE_WARNING && band >= 3) chance = 0.50f;
        else if (tier >= 4) chance = 0.20f;
        else if (band >= 3) chance = 0.30f;
        else chance = 0.10f;

        if (Random.Float() < chance) {
            Sample.INSTANCE.play(Assets.Sounds.YANDERE_LAUGH_HIGH);
            lastHighLaughClock = now;
        }
    }

    @Override
    public void yell(String text) {
        int kind = dialogueKind(text);
        if (kind == DIALOGUE_OTHER) {
            super.yell(text);
            return;
        }
        int tier = obsessionStage();
        int band = rageBand();
        String transformed = transformedGrowthDialogue(text, kind);
        super.yell(transformed);
        maybePlayHighLaugh(kind, tier, band);
    }

    @Override
    protected boolean act() {
        if (tryPreRecallEmergencyRunBack()) return true;
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
        boolean intercepted = src instanceof LifeLink && ((LifeLink)src).yandereGuardLink && ((LifeLink)src).object == id();
        super.damage(dmg, src);
        if (intercepted) clearMyInterceptLinks();
    }

    @Override
    public void die(Object cause) {
        clearMyInterceptLinks();
        super.die(cause);
    }

    @Override public int attackSkill(Char target) { return hostileToHero() ? 1_000_000 : growthAccuracy(); }

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

    @Override public float speed() { return hostileToHero() ? 1f : (growthHearts >= HEART_INTERCEPT_II ? 1.2f : 1f); }
    @Override public float attackDelay() { return 1f; }

    @Override
    public int statAttackMin() {
        if (hostileToHero()) return Math.max(9999, Dungeon.hero == null ? 9999 : Dungeon.hero.HT * 4);
        return Math.max(0, Math.round(growthAttackMin() * lowHpCombatMultiplier()));
    }

    @Override public int statAttackMax() { return hostileToHero() ? statAttackMin() : Math.max(0, Math.round(growthAttackMax() * lowHpCombatMultiplier())); }
    @Override public int statDrMin() { return Math.max(0, Math.round(growthDrMin() * lowHpCombatMultiplier())); }
    @Override public int statDrMax() { return Math.max(0, Math.round(growthDrMax() * lowHpCombatMultiplier())); }
    @Override public int statAccuracy() { return hostileToHero() ? 1_000_000 : growthAccuracy(); }
    @Override public int statDefenseSkill() { return growthDefenseSkill(); }
    @Override public float statMoveSpeed() { return hostileToHero() ? 1f : (growthHearts >= HEART_INTERCEPT_II ? 1.2f : 1f); }
    @Override public float statAttackSpeed() { return 1f; }

    public float statRegenPercent() {
        if (growthHearts >= HEART_REGEN_II) return 0.03f;
        if (growthHearts >= HEART_REGEN_I) return 0.02f;
        return BASE_REGEN_PERCENT;
    }

    public float statRegenInterval() { return REGEN_INTERVAL; }
    public float interceptChance() { return interceptStage() >= 2 ? 0.50f : interceptStage() == 1 ? 0.30f : 0f; }
    public float interceptShare() { return interceptStage() >= 2 ? 0.75f : interceptStage() == 1 ? 0.50f : 0f; }
    public int interceptRange() { return interceptStage() >= 2 ? 3 : interceptStage() == 1 ? 1 : 0; }

    private static final String GROWTH_HEARTS = "lab3_growth_yandere_hearts";
    private static final String REGEN_AGE = "lab3_growth_yandere_regen_age";
    private static final String HIGH_LAUGH_AGE = "lab3_growth_yandere_high_laugh_age";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        float now = globalGrowthClock();
        float regenAge = lastRegenClock < 0f ? 0f : Math.max(0f, now - lastRegenClock);
        float laughAge = lastHighLaughClock < -900000f ? HIGH_LAUGH_COOLDOWN : Math.max(0f, now - lastHighLaughClock);
        bundle.put(GROWTH_HEARTS, growthHearts);
        bundle.put(REGEN_AGE, regenAge);
        bundle.put(HIGH_LAUGH_AGE, laughAge);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        if (bundle.contains(GROWTH_HEARTS)) growthHearts = clampHearts(bundle.getInt(GROWTH_HEARTS));
        HT = growthMaxHP();
        defenseSkill = growthDefenseSkill();
        HP = Math.min(HP, HT);
        float regenAge = bundle.contains(REGEN_AGE) ? Math.max(0f, bundle.getFloat(REGEN_AGE)) : 0f;
        float laughAge = bundle.contains(HIGH_LAUGH_AGE) ? Math.max(0f, bundle.getFloat(HIGH_LAUGH_AGE)) : HIGH_LAUGH_COOLDOWN;
        lastRegenClock = globalGrowthClock() - Math.min(REGEN_INTERVAL, regenAge);
        lastHighLaughClock = globalGrowthClock() - Math.min(HIGH_LAUGH_COOLDOWN, laughAge);
        lastInterceptRollTurn = Integer.MIN_VALUE;
        lastHeroPosForTrapScan = Dungeon.hero == null ? -1 : Dungeon.hero.pos;
        lastRoomSignature = Integer.MIN_VALUE;
        warnedNearbySecretDoors.clear();
        warnedSecretRooms.clear();
        if (isFullyAwakened()) normalizeFinalAwakeningState();
    }
}