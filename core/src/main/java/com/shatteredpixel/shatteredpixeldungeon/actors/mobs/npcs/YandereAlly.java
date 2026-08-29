package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ClericSpell;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.RedRibbon;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.sprites.YandereSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class YandereAlly extends DirectableAlly {

    public static final int MODE_RAMPAGE = 0;
    public static final int MODE_GUARD   = 1;
    public static final int MODE_PEACE   = 2;

    // ==========================================================
    // LAB3-y02 BALANCE BLOCK
    // 한 층에 한 번쯤 신경쓰게 만드는 방향의 첫 테스트값.
    // 여기 숫자만 바꾸면 다음 밸런스 조정이 쉬움.
    // ==========================================================
    public static final float AFFECTION_GRACE = 60f;
    public static final float RAGE_INTERVAL = 5f;
    public static final int TALK_REDUCTION = 5;
    public static final float TALK_COOLDOWN = 250f;
    public static final float TALK_TIME_CREDIT = 60f;

    public static final float LIMIT_SAFE_TIME = 12f;
    public static final float SNAP_CHANCE_PER_ACT = 0.04f;

    public static final float CHATTER_MIN = 35f;
    public static final float CHATTER_MAX = 70f;

    public static final float EMERGENCY_HP = 0.25f;
    public static final float EMERGENCY_RELEASE_HP = 0.40f;

    public static final int HERO_HIT_RAGE = 15;
    public static final int EFFECTIVE_HEART_MIN_RAGE = 20;
    // ==========================================================

    private int mode = MODE_GUARD;
    private int modeBeforeHostile = MODE_GUARD;

    // 하트를 받으면 rageBase=0 + affectionClock=현재 시각.
    private int rageBase = 0;
    private float affectionClock = -1f;
    private float lastTalkClock = -999999f;

    // RedRibbon은 층 이동 때 얀데레 본체를 새로 만든다.
    // 그래서 리본이 savedRage만 넘겨도 마지막 하트 타이머가 리셋되지 않도록
    // 직전 본체의 정확한 시간 기반 상태를 짧게 보관한다.
    private static final float FLOOR_TRANSFER_WINDOW = 200f;
    private static float transferSnapshotClock = -1f;
    private static float transferAffectionAge = 0f;
    private static float transferLimitAge = -1f;
    private static int transferCurrentRage = -1;
    private static int transferRageBase = 0;
    private static int transferMode = -1;
    private static int transferTotalHearts = -1;
    private static int transferEffectiveHearts = -1;
    private static int transferWarnedStage = 0;
    private static boolean transferHostile = false;

    private int warnedStage = 0;
    private float limitReachedClock = -1f;

    private int revengeTargetID = -1;
    private boolean emergencyProtect = false;
    private boolean hostileToHero = false;

    private int effectiveHearts = 0;
    private int totalHearts = 0;
    private int kills = 0;

    private float nextChatterClock = -1f;
    private float lastPainDialogueClock = -999999f;
    private int heroHpStage = 0;
    private int pendingArrivalDialogue = 0;

    private int lastDepth = -999;
    private int lastBranch = -999;
    private float floorEnterClock = -1f;

    {
        spriteClass = YandereSprite.class;
        flying = false;

        HP = HT = 9999;
        defenseSkill = 1000;
        viewDistance = 24;

        EXP = 0;
        maxLvl = -5;

        intelligentAlly = false;
        attacksAutomatically = true;
        state = WANDERING;
    }

    public YandereAlly() {
        super();
        ensureClocks();
    }

    @Override
    public String name() {
        return hostileToHero ? "광란한 얀데레" : "얀데레";
    }

    @Override
    public String description() {
        if (hostileToHero) {
            return "애정 결핍으로 이성이 끊어졌다. 공격은 거의 즉사급이지만 이동속도는 주인공과 같은 1배속이다. "
                    + "도망치면서 하트를 찾아 던지면 다시 진정시킬 수 있다.";
        }
        return "개발자 실험용 초강력 얀데레 아군이다. 적은 압도적으로 잘 잡지만, "
                + "오랫동안 애정표현을 받지 못하면 광란도가 오른다. "
                + "주인공이 심하게 다치면 어떤 명령을 내렸든 무시하고 즉시 돌아온다.";
    }

    // ---------- combat stats ----------

    @Override
    public int attackSkill(Char target) {
        return 1_000_000;
    }

    @Override
    public int damageRoll() {
        if (hostileToHero && Dungeon.hero != null) {
            // 추격전은 접촉 자체가 공포가 되도록 원킬급.
            return Math.max(9999, Dungeon.hero.HT * 4);
        }
        return Random.NormalIntRange(180, 320);
    }

    @Override
    public int drRoll() {
        return Random.NormalIntRange(350, 550);
    }

    @Override
    public float speed() {
        // 광란 추격전만 정확히 1배속.
        if (hostileToHero) return 1f;
        return super.speed() * 1.5f;
    }

    @Override
    public float attackDelay() {
        if (hostileToHero) return 1f;
        return super.attackDelay() * 0.65f;
    }

    public int statAttackMin() { return hostileToHero ? Math.max(9999, Dungeon.hero == null ? 9999 : Dungeon.hero.HT * 4) : 180; }
    public int statAttackMax() { return hostileToHero ? statAttackMin() : 320; }
    public int statDrMin() { return 350; }
    public int statDrMax() { return 550; }
    public int statAccuracy() { return 1_000_000; }
    public int statDefenseSkill() { return 1000; }
    public float statMoveSpeed() { return hostileToHero ? 1f : 1.5f; }
    public float statAttackSpeed() { return hostileToHero ? 1f : (1f / 0.65f); }
    public int kills() { return kills; }

    @Override
    public int attackProc(Char enemy, int damage) {
        boolean wasAlive = enemy != null && enemy.isAlive();
        int result = super.attackProc(enemy, damage);
        // 실제 사망은 attackProc 이후 처리되므로 다음 act에서도 정리되지만,
        // 치트 아군이 얼마나 일했는지 대략 보기 위한 카운터.
        if (wasAlive && enemy != null && enemy.HP - result <= 0 && enemy != Dungeon.hero) {
            kills++;
        }
        return result;
    }

    // ---------- hero accidentally attacks her ----------

    private boolean heroCaused(Object src) {
        return src == Dungeon.hero
                || src instanceof Wand
                || src instanceof ClericSpell
                || src instanceof ArmorAbility;
    }

    @Override
    public void damage(int dmg, Object src) {
        if (!hostileToHero && heroCaused(src)) {
            addRage(HERO_HIT_RAGE);
            int pick = Random.Int(5);
            if (pick == 0) {
                yell("왜 나 때렸어? 나 뭐 잘못했어? 말해줘. 고칠게.");
            } else if (pick == 1) {
                yell("장난이지? 장난이라고 해줘♡ 그러면 나 진짜 안 화낼게.");
            } else if (pick == 2) {
                yell("아파. 근데 네가 때린 거라 더 아파. 왜 그랬어?");
            } else if (pick == 3) {
                yell("나 싫어진 거 아니지? 아니라고 해줘. 지금 당장.");
            } else {
                yell("또 때리면 나 진짜 이상해질 것 같아. 그러니까 그러지 마.");
            }
            // 실수 한 방에 치트 아군이 사라지는 건 막음.
            return;
        }
        super.damage(dmg, src);
    }

    @Override
    public void die(Object cause) {
        RedRibbon.onYandereDied(this);
        super.die(cause);
    }

    // ---------- main AI ----------

    @Override
    protected boolean act() {
        ensureClocks();
        detectFloorChange();

        if (pendingArrivalDialogue != 0) {
            int kind = pendingArrivalDialogue;
            pendingArrivalDialogue = 0;

            if (kind == 2) {
                yell("계단으로 도망가면 끝날 줄 알았어? 나도 따라왔어. 이번엔 어디까지 갈 건데?");
            } else {
                yell("나도 왔어♡ 설마 층 바뀌었다고 나 두고 갈 줄 알았어?");
            }
        }

        if (!hostileToHero && HP < HT) {
            HP = Math.min(HT, HP + 100);
        }

        if (!hostileToHero && emergencyProtect && Dungeon.hero != null
                && Dungeon.hero.HP / (float)Math.max(1, Dungeon.hero.HT) > EMERGENCY_RELEASE_HP) {
            emergencyProtect = false;
            revengeTargetID = -1;
            clearEnemy();
            clearDefensingPos();
            if (mode == MODE_PEACE) attacksAutomatically = false;
            state = WANDERING;
            yell("이제 좀 괜찮아졌네. 진짜 다시 그러지 마. 나 심장 떨어지는 줄 알았어♡");
        }

        updateWarnings();
        maybeSnap();
        maybeChatter();

        if (hostileToHero) {
            alignment = Alignment.ENEMY;
            attacksAutomatically = true;
            emergencyProtect = false;
            revengeTargetID = -1;
            if (Dungeon.hero != null && Dungeon.hero.isAlive()) {
                enemy = Dungeon.hero;
                target = Dungeon.hero.pos;
                state = HUNTING;
            }
        } else if (emergencyProtect) {
            alignment = Alignment.ALLY;
            attacksAutomatically = true; // 공격 금지 명령도 무시
            Char revenge = revengeTarget();
            if (revenge != null) {
                targetChar(revenge);
            } else {
                state = WANDERING;
            }
        } else if (mode == MODE_PEACE) {
            alignment = Alignment.ALLY;
            attacksAutomatically = false;
            revengeTargetID = -1;
            clearEnemy();
            clearDefensingPos();
            state = WANDERING;
        } else {
            alignment = Alignment.ALLY;
            attacksAutomatically = true;

            // 광폭은 층 전체의 '도달 가능한' 적을 적극적으로 추적한다.
            // 적이 없거나 현재 지형상 도달할 수 없으면 멈춰 있지 않고 순찰한다.
            if (mode == MODE_RAMPAGE && revengeTarget() == null) {
                Char hunt = nearestEnemyAnywhere();
                if (hunt != null) {
                    clearDefensingPos();
                    targetChar(hunt);
                } else {
                    clearEnemy();
                    ensureRampagePatrol();
                }
            }
        }

        return super.act();
    }

    @Override
    protected Char chooseEnemy() {
        if (hostileToHero) {
            return Dungeon.hero != null && Dungeon.hero.isAlive() ? Dungeon.hero : null;
        }

        if (emergencyProtect) {
            Char revenge = revengeTarget();
            if (revenge != null) return revenge;
            return nearestEnemyNearHero(3);
        }

        if (mode == MODE_PEACE) return null;

        Char revenge = revengeTarget();
        if (revenge != null) return revenge;

        if (mode == MODE_GUARD) {
            return nearestEnemyNearHero(2);
        }

        if (mode == MODE_RAMPAGE) {
            Char all = nearestEnemyAnywhere();
            if (all != null) return all;
        }

        return super.chooseEnemy();
    }

    private Char nearestEnemyNearHero(int range) {
        if (Dungeon.level == null || Dungeon.hero == null) return null;

        Mob best = null;
        int bestDist = Integer.MAX_VALUE;

        for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
            if (!validEnemy(mob)) continue;

            int heroDist = Dungeon.level.distance(Dungeon.hero.pos, mob.pos);
            if (heroDist <= range) {
                int myDist = Dungeon.level.distance(pos, mob.pos);
                if (best == null || myDist < bestDist) {
                    best = mob;
                    bestDist = myDist;
                }
            }
        }
        return best;
    }

    private Char nearestEnemyAnywhere() {
        if (Dungeon.level == null) return null;

        // 단순 직선거리가 아니라 실제 걸어서 갈 수 있는 적만 고른다.
        // 이 검사가 없으면 막힌 적을 계속 재지정하면서 매 턴 '?'만 띄울 수 있다.
        PathFinder.buildDistanceMap(pos, Dungeon.level.passable);

        Mob best = null;
        int bestDist = Integer.MAX_VALUE;

        for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
            if (!validEnemy(mob)) continue;

            int d = PathFinder.distance[mob.pos];
            if (d == Integer.MAX_VALUE) continue;

            if (best == null || d < bestDist) {
                best = mob;
                bestDist = d;
            }
        }
        return best;
    }

    private void ensureRampagePatrol() {
        if (Dungeon.level == null) return;

        // 이미 순찰 목적지로 걸어가는 중이면 계속 간다.
        if (movingToDefendPos && defendingPos != -1 && pos != defendingPos) {
            state = WANDERING;
            return;
        }

        // 현재 위치에서 실제로 도달 가능한 칸 중 멀리 떨어진 후보를 뽑는다.
        // 도착할 때마다 새 목적지를 골라 광폭 상태에서는 계속 맵을 돌아다닌다.
        PathFinder.buildDistanceMap(pos, Dungeon.level.passable);

        int best = -1;
        int bestDist = 0;

        for (int i = 0; i < 48; i++) {
            int cell = Random.Int(Dungeon.level.length());
            if (cell == pos || Actor.findChar(cell) != null) continue;

            int d = PathFinder.distance[cell];
            if (d == Integer.MAX_VALUE || d <= 0) continue;

            if (d > bestDist) {
                best = cell;
                bestDist = d;
            }
        }

        if (best != -1) {
            defendPos(best);
        } else {
            // 극단적으로 이동 가능한 칸이 없는 방에서도 HUNTING에 남아 '?'를 반복하지 않는다.
            clearDefensingPos();
            state = WANDERING;
        }
    }

    private boolean validEnemy(Mob mob) {
        return mob != null
                && mob != this
                && mob.isAlive()
                && mob.alignment == Alignment.ENEMY
                && !mob.isInvulnerable(getClass());
    }

    public void queueFloorArrivalDialogue(boolean hostileArrival) {
        pendingArrivalDialogue = hostileArrival ? 2 : 1;
    }

    // ---------- commands ----------

    public void setMode(int newMode) {
        if (hostileToHero) {
            yell("지금도 나한테 명령하려고? 싫어. 이번엔 내가 하고 싶은 대로 할 거야.");
            return;
        }
        if (newMode < MODE_RAMPAGE || newMode > MODE_PEACE) return;

        mode = newMode;
        revengeTargetID = -1;
        clearEnemy();
        clearDefensingPos();
        state = WANDERING;

        if (emergencyProtect) {
            yell("지금은 명령 안 들을 거야. 네가 이 꼴인데 내가 어떻게 가만히 있어?");
            return;
        }

        if (mode == MODE_PEACE) {
            attacksAutomatically = false;
            yell("알겠어♡ 아무도 안 건드릴게. 네가 하지 말랬으니까 진짜 꾹 참을게.");
        } else if (mode == MODE_GUARD) {
            attacksAutomatically = true;
            yell("응♡ 네 옆에 딱 붙어 있을게. 가까이 오는 것만 내가 치워줄게.");
        } else {
            attacksAutomatically = true;
            yell("좋아♡ 내가 다 잡아올게! 넌 손 하나 까딱하지 말고 나만 믿어.");
        }
    }

    public int mode() { return mode; }

    public String modeName() {
        if (hostileToHero) return "광란 추격";
        if (emergencyProtect) return "강제 보호";
        switch (mode) {
            case MODE_RAMPAGE: return "광폭";
            case MODE_PEACE: return "공격 금지";
            case MODE_GUARD:
            default: return "수비";
        }
    }

    // ---------- hero damage reaction ----------

    public static void onHeroDamaged(int effectiveDamage, int preHP, int postHP, Object src) {
        if (Dungeon.level == null || Dungeon.hero == null || effectiveDamage <= 0) return;

        Char attacker = src instanceof Char ? (Char)src : null;
        if (attacker != null
                && (attacker == Dungeon.hero || attacker.alignment != Alignment.ENEMY || !attacker.isAlive())) {
            attacker = null;
        }

        for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
            if (mob instanceof YandereAlly) {
                ((YandereAlly)mob).reactToHeroDamage(effectiveDamage, preHP, postHP, attacker);
            }
        }
    }

    private void reactToHeroDamage(int damage, int preHP, int postHP, Char attacker) {
        if (hostileToHero || Dungeon.hero == null) return;

        float hp = Math.max(0f, Dungeon.hero.HP / (float)Math.max(1, Dungeon.hero.HT));
        int newStage = hp <= 0.10f ? 4 : hp <= 0.25f ? 3 : hp <= 0.50f ? 2 : hp <= 0.75f ? 1 : 0;
        boolean severeHit = damage >= Math.max(5, Dungeon.hero.HT / 5);
        boolean crossed = newStage > heroHpStage;

        if (newStage < heroHpStage) heroHpStage = newStage;

        if (attacker != null) {
            revengeTargetID = attacker.id();
            if (mode != MODE_PEACE || hp <= EMERGENCY_HP) {
                targetChar(attacker);
            }
        }

        // 25% 이하는 명령보다 보호 본능이 우선.
        if (hp <= EMERGENCY_HP) {
            emergencyProtect = true;
            attacksAutomatically = true;
            emergencyRecall();
        }

        float now = globalClock();
        if (crossed || severeHit || now - lastPainDialogueClock >= 14f) {
            lastPainDialogueClock = now;
            sayPainLine(newStage, attacker);
        }

        heroHpStage = Math.max(heroHpStage, newStage);
    }

    private void sayPainLine(int stage, Char attacker) {
        if (stage >= 4) {
            switch (Random.Int(5)) {
                case 0:
                    yell("안 돼 안 돼 안 돼! 너 죽으면 안 돼! 나 봐, 나 여기 있어!");
                    break;
                case 1:
                    yell("누가 이랬어! 누구야! 저년을 내가 분질러버릴 거야, 분질러버릴 거야!");
                    break;
                case 2:
                    yell("피가 왜 이렇게 빠졌어? 누가 했어? 말해! 내가 걔부터 갈아버릴게!");
                    break;
                case 3:
                    yell("아프지 마! 제발 아프지 마! 너 건드린 건 내가 전부 없애버릴 테니까!");
                    break;
                default:
                    yell("너 숨 쉬어. 계속 숨 쉬어! 나머지는 내가 할게, 내가 다 할게!");
                    break;
            }
        } else if (stage >= 3) {
            switch (Random.Int(5)) {
                case 0:
                    yell("누가 너 이렇게 만들었어? 걔 어디 있어. 빨리 말해.");
                    break;
                case 1:
                    yell("내가 옆에 있는데 왜 이렇게까지 다쳐! 화나, 진짜 너무 화나!");
                    break;
                case 2:
                    yell("이리 와! 지금은 내 말 들어. 네가 뭐라고 해도 나 안 떨어질 거야.");
                    break;
                case 3:
                    yell("쟤가 그랬어? 쟤 맞지? 좋아. 쟤부터 부숴버리면 되는 거지?");
                    break;
                default:
                    yell("아프잖아! 왜 참아! 나한테 와, 내가 다 막아줄게!");
                    break;
            }
        } else if (stage >= 2) {
            switch (Random.Int(4)) {
                case 0:
                    yell("많이 아파? 나한테 와♡ 내가 옆에 있을게.");
                    break;
                case 1:
                    yell("또 다쳤어? 진짜 속상해. 너 때린 애는 내가 기억했어.");
                    break;
                case 2:
                    yell("괜찮다고 하지 마. 안 괜찮아 보이니까 내가 더 붙어 있을 거야♡");
                    break;
                default:
                    yell("내가 조금만 더 잘 지켰어야 했는데. 이제 안 놓칠게.");
                    break;
            }
        } else {
            switch (Random.Int(4)) {
                case 0:
                    yell("아팠어? 이리 와♡ 내가 봐줄게.");
                    break;
                case 1:
                    yell("누가 건드렸어? 내가 바로 처리하고 올게♡");
                    break;
                case 2:
                    yell("조심해, 자기야♡ 너 다치면 나 진짜 싫어.");
                    break;
                default:
                    yell("괜찮아? 나 여기 있어. 다음엔 내가 먼저 막을게♡");
                    break;
            }
        }
    }

    private void emergencyRecall() {
        if (Dungeon.hero == null || Dungeon.level == null) return;
        if (Dungeon.level.distance(pos, Dungeon.hero.pos) <= 1) return;

        int cell = adjacentTo(Dungeon.hero);
        if (cell != -1) {
            ScrollOfTeleportation.appear(this, cell);
        }
    }

    private int adjacentTo(Char ch) {
        if (ch == null || Dungeon.level == null) return -1;

        int[] dirs = PathFinder.NEIGHBOURS8.clone();
        Random.shuffle(dirs);

        for (int off : dirs) {
            int cell = ch.pos + off;
            if (cell < 0 || cell >= Dungeon.level.length()) continue;
            if (Actor.findChar(cell) == null
                    && (Dungeon.level.passable[cell] || Dungeon.level.avoid[cell])) {
                return cell;
            }
        }
        return -1;
    }

    private Char revengeTarget() {
        if (revengeTargetID == -1) return null;

        Actor actor = Actor.findById(revengeTargetID);
        if (actor instanceof Char) {
            Char ch = (Char)actor;
            if (ch.isAlive() && ch.isActive()
                    && ch.alignment == Alignment.ENEMY
                    && Actor.chars().contains(ch)) {
                return ch;
            }
        }

        revengeTargetID = -1;
        return null;
    }

    public String currentTargetName() {
        if (hostileToHero) return Dungeon.hero == null ? "주인공" : Dungeon.hero.name();
        Char revenge = revengeTarget();
        if (revenge != null) return revenge.name();
        if (enemy != null && enemy.isAlive()) return enemy.name();
        return "없음";
    }

    // ---------- affection / rage ----------

    private static float globalClock() {
        return Statistics.duration + Actor.now();
    }

    private void ensureClocks() {
        float now = globalClock();
        if (affectionClock < 0f) affectionClock = now;
        if (floorEnterClock < 0f) floorEnterClock = now;
        if (nextChatterClock < 0f) nextChatterClock = now + Random.Float(CHATTER_MIN, CHATTER_MAX);

        if (Dungeon.level != null) {
            if (lastDepth == -999) lastDepth = Dungeon.depth;
            if (lastBranch == -999) lastBranch = Dungeon.branch;
        }
    }

    private void detectFloorChange() {
        if (Dungeon.level == null) return;
        if (lastDepth != Dungeon.depth || lastBranch != Dungeon.branch) {
            lastDepth = Dungeon.depth;
            lastBranch = Dungeon.branch;
            floorEnterClock = globalClock();
        }
    }

    private void snapshotFloorTransfer(float now, float affectionAge, int currentRage) {
        transferSnapshotClock = now;
        transferAffectionAge = Math.max(0f, affectionAge);
        transferLimitAge = limitReachedClock < 0f ? -1f : Math.max(0f, now - limitReachedClock);
        transferCurrentRage = currentRage;
        transferRageBase = rageBase;
        transferMode = mode;
        transferTotalHearts = totalHearts;
        transferEffectiveHearts = effectiveHearts;
        transferWarnedStage = warnedStage;
        transferHostile = hostileToHero;
    }

    private static boolean matchesFloorTransfer(int savedMode, int savedRage, int savedEffectiveHearts,
                                                int savedTotalHearts, boolean savedHostile, float now) {
        return transferSnapshotClock >= 0f
                && now >= transferSnapshotClock
                && now - transferSnapshotClock <= FLOOR_TRANSFER_WINDOW
                && transferMode == savedMode
                && transferCurrentRage == Math.max(0, Math.min(100, savedRage))
                && transferEffectiveHearts == Math.max(0, savedEffectiveHearts)
                && transferTotalHearts == Math.max(transferEffectiveHearts, savedTotalHearts)
                && transferHostile == savedHostile;
    }

    private static void clearFloorTransferSnapshot() {
        transferSnapshotClock = -1f;
        transferAffectionAge = 0f;
        transferLimitAge = -1f;
        transferCurrentRage = -1;
        transferRageBase = 0;
        transferMode = -1;
        transferTotalHearts = -1;
        transferEffectiveHearts = -1;
        transferWarnedStage = 0;
        transferHostile = false;
    }

    public int rage() {
        ensureClocks();

        float now = globalClock();
        float elapsed = Math.max(0f, now - affectionClock);

        if (hostileToHero) {
            snapshotFloorTransfer(now, elapsed, 100);
            return 100;
        }

        int gained = 0;

        if (elapsed > AFFECTION_GRACE) {
            gained = (int)Math.floor((elapsed - AFFECTION_GRACE) / RAGE_INTERVAL);
        }

        int current = Math.max(0, Math.min(100, rageBase + gained));
        snapshotFloorTransfer(now, elapsed, current);
        return current;
    }

    public void addRage(int amount) {
        int current = rage();
        rageBase = Math.max(0, Math.min(100, current + amount));
        affectionClock = globalClock();
        onRageChanged();
    }

    private void onRageChanged() {
        int r = rage();
        if (r >= 100) {
            if (limitReachedClock < 0f) limitReachedClock = globalClock();
        } else {
            limitReachedClock = -1f;
        }
        warnedStage = Math.min(warnedStage, stage(r));
    }

    public String emotionName() {
        if (hostileToHero) return "이성 붕괴";
        int r = rage();
        if (r >= 100) return "한계";
        if (r >= 95) return "폭발 직전";
        if (r >= 85) return "위험";
        if (r >= 70) return "불안";
        if (r >= 50) return "서운";
        return "안정";
    }

    public int turnsSinceAffection() {
        ensureClocks();
        return Math.max(0, Math.round(globalClock() - affectionClock));
    }

    public int floorTurns() {
        ensureClocks();
        detectFloorChange();
        return Math.max(0, Math.round(globalClock() - floorEnterClock));
    }

    public int turnsUntilTalk() {
        return Math.max(0, Math.round(TALK_COOLDOWN - (globalClock() - lastTalkClock)));
    }

    public int turnsAtLimit() {
        if (limitReachedClock < 0f) return 0;
        return Math.max(0, Math.round(globalClock() - limitReachedClock));
    }

    public boolean canTalk() {
        return globalClock() - lastTalkClock >= TALK_COOLDOWN;
    }

    public int effectiveHearts() { return effectiveHearts; }
    public int totalHearts() { return totalHearts; }
    public boolean hostileToHero() { return hostileToHero; }
    public boolean emergencyProtect() { return emergencyProtect; }

    public void talk() {
        if (hostileToHero) {
            yell("말로 끝내려고? 하트부터 줘. 지금은 그거 아니면 못 믿겠어.");
            return;
        }

        if (!canTalk()) {
            yell("또 말 걸어주는 거야? 헤헤, 좋아♡ 그래도 조금 있다가 또 얘기하자.");
            return;
        }

        int before = rage();
        rageBase = Math.max(0, before - TALK_REDUCTION);

        // 대화는 작은 애정표현. 하트처럼 완전 초기화는 하지 않음.
        float now = globalClock();
        affectionClock = Math.min(now, affectionClock + TALK_TIME_CREDIT);
        lastTalkClock = now;
        warnedStage = stage(rage());

        switch (Random.Int(6)) {
            case 0:
                yell("나 불렀어? 응♡ 나 여기 있어. 계속 네 옆에 있을게.");
                break;
            case 1:
                yell("먼저 말 걸어주는 거 너무 좋아♡ 한 번만 더 불러주면 안 돼?");
                break;
            case 2:
                yell("오늘도 나 필요하지? 필요하다고 해줘♡ 나 그 말 진짜 좋아해.");
                break;
            case 3:
                yell("헤헤♡ 네가 나 신경 써주는 순간이 제일 좋아.");
                break;
            case 4:
                yell("나 보고 싶어서 부른 거지? 맞다고 해♡ 맞다고 해줘.");
                break;
            default:
                yell("응응♡ 듣고 있어. 너 하는 말이면 하루 종일 들어도 좋아.");
                break;
        }
    }

    public void receiveHeart() {
        int before = rage();
        totalHearts++;

        boolean effective = before >= EFFECTIVE_HEART_MIN_RAGE;
        if (effective) effectiveHearts++;

        rageBase = 0;
        affectionClock = globalClock();
        warnedStage = 0;
        limitReachedClock = -1f;

        boolean wasHostile = hostileToHero;
        if (hostileToHero) {
            hostileToHero = false;
            alignment = Alignment.ALLY;
            mode = modeBeforeHostile;
            emergencyProtect = false;
            revengeTargetID = -1;
            clearEnemy();
            clearDefensingPos();
            state = WANDERING;
            attacksAutomatically = mode != MODE_PEACE;
        }

        snapshotFloorTransfer(globalClock(), 0f, 0);

        if (wasHostile) {
            switch (Random.Int(4)) {
                case 0:
                    yell("하트다. 나 주는 거야? 진짜 나 주는 거야? 응, 알겠어. 이제 안 죽일게♡");
                    break;
                case 1:
                    yell("나 아직 좋아하는 거 맞네. 다행이다, 다행이다♡ 나 진짜 무서웠어.");
                    break;
                case 2:
                    yell("받았어♡ 됐어. 이제 됐어. 다시 네 편 할게. 계속 네 편 할게.");
                    break;
                default:
                    yell("역시 나 버린 거 아니었구나♡ 나 다시 착하게 있을게. 진짜야.");
                    break;
            }
        } else if (effective) {
            switch (Random.Int(6)) {
                case 0:
                    yell("나 주는 거야? 좋아♡ 진짜 너무 좋아!");
                    break;
                case 1:
                    yell("나 생각해서 챙겨준 거지? 이거 절대 안 잊을 거야♡");
                    break;
                case 2:
                    yell("역시 나 좋아하는 거 맞네♡ 괜히 혼자 속상해했잖아.");
                    break;
                case 3:
                    yell("하트다♡ 헤헤, 기분 다 풀렸어. 이제 또 열심히 지켜줄게!");
                    break;
                case 4:
                    yell("고마워 고마워♡ 나 이거 받은 거 계속 기억할 거야.");
                    break;
                default:
                    yell("사랑해♡ 아니, 네가 먼저 준 거니까 나도 말해도 되지? 사랑해♡");
                    break;
            }
        } else {
            switch (Random.Int(3)) {
                case 0:
                    yell("벌써 또 주는 거야? 헤헤♡ 좋아. 많이 줘도 다 받을래.");
                    break;
                case 1:
                    yell("나 아직 괜찮았는데도 챙겨줬네♡ 이런 거 너무 좋아.");
                    break;
                default:
                    yell("또 하트야? 나 버릇 나빠져도 책임져야 해♡");
                    break;
            }
        }
    }

    // ---------- 100 rage -> probabilistic snap ----------

    private void maybeSnap() {
        if (hostileToHero) return;

        int r = rage();
        if (r < 100) {
            limitReachedClock = -1f;
            return;
        }

        if (limitReachedClock < 0f) {
            limitReachedClock = globalClock();
            return;
        }

        if (globalClock() - limitReachedClock < LIMIT_SAFE_TIME) return;

        if (Random.Float() < SNAP_CHANCE_PER_ACT) {
            modeBeforeHostile = mode;
            hostileToHero = true;
            emergencyProtect = false;
            revengeTargetID = -1;
            alignment = Alignment.ENEMY;
            attacksAutomatically = true;
            clearDefensingPos();
            enemy = Dungeon.hero;
            target = Dungeon.hero.pos;
            state = HUNTING;

            // LAB3-y021: 광란 시작 순간 거리를 벌려 추격전을 성립시킨다.
            startChaseAtDistance();

            switch (Random.Int(5)) {
                case 0:
                    yell("됐어. 나 이제 못 참아. 네가 나 안 봐주면 영원히 못 떠나게 하면 되잖아.");
                    break;
                case 1:
                    yell("나 진짜 많이 참았어! 많이 참았다고! 이제 네가 내 옆에 있게 만들 거야.");
                    break;
                case 2:
                    yell("싫어 싫어 싫어! 나 무시하지 마! 도망가도 내가 끝까지 따라갈 거야!");
                    break;
                case 3:
                    yell("나만 좋아하면 아무 문제 없었잖아. 왜 이렇게까지 만들어? 이제 안 놔줄 거야.");
                    break;
                default:
                    yell("도망가 봐. 하트 찾아서 나한테 던지든가, 아니면 내가 먼저 잡을게♡");
                    break;
            }
        }
    }


    // LAB3-y021: 랜덤 재생성 후보를 여러 번 뽑아 영웅에게서 가장 먼 칸을 고른다.
    private void startChaseAtDistance() {
        if (Dungeon.level == null || Dungeon.hero == null) return;

        int best = -1;
        int bestDist = -1;

        for (int i = 0; i < 32; i++) {
            int cell = Dungeon.level.randomRespawnCell(this);
            if (cell == -1) continue;

            int d = Dungeon.level.distance(cell, Dungeon.hero.pos);
            if (d > bestDist) {
                best = cell;
                bestDist = d;
            }
        }

        if (best != -1 && best != pos) {
            ScrollOfTeleportation.appear(this, best);
        }
    }

    // ---------- chatter ----------

    private void maybeChatter() {
        if (hostileToHero || Dungeon.hero == null) return;

        float now = globalClock();
        if (now < nextChatterClock) return;

        nextChatterClock = now + Random.Float(CHATTER_MIN, CHATTER_MAX);

        int r = rage();

        if (r >= 85) {
            switch (Random.Int(6)) {
                case 0:
                    yell("나 진짜 서러워. 왜 나만 계속 기다리게 해?");
                    break;
                case 1:
                    yell("하트 하나만 줘. 나 좋아한다는 표시 하나만 줘. 그게 그렇게 어려워?");
                    break;
                case 2:
                    yell("나 계속 참고 있잖아. 나 착하게 참고 있잖아. 그러니까 나 좀 봐줘.");
                    break;
                case 3:
                    yell("나 버릴 생각 하지 마. 그 생각만 해도 속이 뒤집혀.");
                    break;
                case 4:
                    yell("너만 보면 좋은데 동시에 너무 화나. 나 왜 이렇게 만들어?");
                    break;
                default:
                    yell("나한테 와. 지금은 다른 거 하지 말고 나부터 챙겨줘.");
                    break;
            }
        } else if (r >= 50) {
            switch (Random.Int(6)) {
                case 0:
                    yell("나 요즘 좀 소홀한 것 같은데? 나도 애정표현 받고 싶어♡");
                    break;
                case 1:
                    yell("나 계속 잘 지켜주고 있잖아. 칭찬 한 번 해주면 안 돼?");
                    break;
                case 2:
                    yell("하트 발견하면 내 거야♡ 다른 데 쓰면 삐질 거야.");
                    break;
                case 3:
                    yell("나만 계속 따라다니는 것 같아서 조금 서운해. 너도 나 좀 봐줘.");
                    break;
                case 4:
                    yell("나 좋아하지? 응? 좋아한다고 해줘♡");
                    break;
                default:
                    yell("오늘은 내가 먼저 안 보채려고 했는데 실패했어. 나 좀 챙겨줘♡");
                    break;
            }
        } else {
            switch (mode) {
                case MODE_RAMPAGE:
                    switch (Random.Int(5)) {
                        case 0: yell("기다려♡ 내가 싹 정리하고 올게!"); break;
                        case 1: yell("너는 편하게 있어♡ 귀찮은 건 내가 전부 잡아줄게."); break;
                        case 2: yell("적 보이면 나한테 맡겨! 네 손 더러워지는 거 싫어♡"); break;
                        case 3: yell("헤헤♡ 나 잘하지? 더 잡아줄까?"); break;
                        default: yell("나한테 맡기면 금방 둘만 남을 거야♡"); break;
                    }
                    break;
                case MODE_PEACE:
                    switch (Random.Int(5)) {
                        case 0: yell("나 진짜 참고 있어♡ 칭찬해줘."); break;
                        case 1: yell("아무도 안 때리고 있지? 네 말 잘 듣고 있지? 헤헤♡"); break;
                        case 2: yell("저거 거슬리는데 참을게. 네가 하지 말랬으니까♡"); break;
                        case 3: yell("손 안 대고 따라가기만 하는 것도 좋아. 네 옆이니까♡"); break;
                        default: yell("나 얌전히 있으면 하트 하나 줄 거야?♡"); break;
                    }
                    break;
                default:
                    switch (Random.Int(6)) {
                        case 0: yell("여기 딱 붙어 있을게♡ 어디 가지 마."); break;
                        case 1: yell("너 옆이 제일 좋아♡ 가까이 오는 건 내가 막을게."); break;
                        case 2: yell("내가 보고 있으니까 안심하고 가♡"); break;
                        case 3: yell("손 잡고 다니면 더 좋을 텐데♡ 아쉽다."); break;
                        case 4: yell("오늘도 같이 내려가자♡ 끝까지 같이 가는 거야."); break;
                        default: yell("나 여기 있어♡ 부르면 바로 갈게."); break;
                    }
                    break;
            }
        }
    }

    // ---------- warnings ----------

    private int stage(int r) {
        if (r >= 100) return 5;
        if (r >= 95) return 4;
        if (r >= 85) return 3;
        if (r >= 70) return 2;
        if (r >= 50) return 1;
        return 0;
    }

    private void updateWarnings() {
        if (hostileToHero) return;

        int r = rage();
        int s = stage(r);

        if (s > warnedStage) {
            switch (s) {
                case 1:
                    yell("나 슬슬 서운해지려고 해. 나도 좀 챙겨줘♡");
                    break;
                case 2:
                    yell("나 계속 기다리고 있어. 나 좋아한다는 표시 좀 해줘.");
                    break;
                case 3:
                    yell("나 지금 기분 진짜 이상해. 화나고 서럽고 무서워. 나 좀 봐줘.");
                    break;
                case 4:
                    yell("나 더는 못 참을 것 같아. 지금 하트 줘. 나한테 지금 줘.");
                    break;
                case 5:
                    yell("나 한계야. 진짜 한계야. 지금도 나 무시하면 나 무슨 짓 할지 몰라.");
                    if (limitReachedClock < 0f) limitReachedClock = globalClock();
                    break;
                default:
                    break;
            }
        }
        warnedStage = s;
    }

    // ---------- debug / persistence ----------

    public void debugSetRage(int value) {
        hostileToHero = false;
        alignment = Alignment.ALLY;
        rageBase = Math.max(0, Math.min(100, value));
        affectionClock = globalClock();
        warnedStage = Math.max(0, stage(rageBase) - 1);
        limitReachedClock = rageBase >= 100 ? globalClock() : -1f;
    }

    public void applyPersistentState(int savedMode, int savedRage, int savedEffectiveHearts,
                                     int savedTotalHearts, boolean savedHostile) {
        float now = globalClock();
        boolean carryTiming = matchesFloorTransfer(savedMode, savedRage, savedEffectiveHearts,
                savedTotalHearts, savedHostile, now);

        float transferDelta = carryTiming ? Math.max(0f, now - transferSnapshotClock) : 0f;
        float carriedAffectionAge = carryTiming ? transferAffectionAge + transferDelta : 0f;
        float carriedLimitAge = carryTiming && transferLimitAge >= 0f
                ? transferLimitAge + transferDelta : -1f;
        int carriedRageBase = carryTiming ? transferRageBase : Math.max(0, Math.min(100, savedRage));
        int carriedWarnedStage = carryTiming ? transferWarnedStage : stage(savedRage);

        clearFloorTransferSnapshot();

        mode = savedMode;
        modeBeforeHostile = savedMode;
        rageBase = Math.max(0, Math.min(100, carriedRageBase));
        effectiveHearts = Math.max(0, savedEffectiveHearts);
        totalHearts = Math.max(effectiveHearts, savedTotalHearts);
        affectionClock = now - Math.max(0f, carriedAffectionAge);
        warnedStage = Math.max(0, carriedWarnedStage);
        limitReachedClock = carriedLimitAge < 0f ? -1f : now - carriedLimitAge;

        hostileToHero = savedHostile;
        if (savedHostile) {
            rageBase = 100;
            alignment = Alignment.ENEMY;
            attacksAutomatically = true;
            limitReachedClock = now - LIMIT_SAFE_TIME - 1f;
            state = HUNTING;
        } else {
            alignment = Alignment.ALLY;
            attacksAutomatically = mode != MODE_PEACE;
        }
    }

    private static final String MODE = "lab3_yandere_mode";
    private static final String MODE_BEFORE_HOSTILE = "lab3_yandere_mode_before_hostile";
    private static final String RAGE_BASE = "lab3_yandere_rage_base";
    private static final String AFFECTION_CLOCK = "lab3_yandere_affection_clock";
    private static final String AFFECTION_AGE = "lab3_yandere_affection_age";
    private static final String LAST_TALK_CLOCK = "lab3_yandere_last_talk_clock";
    private static final String WARNED_STAGE = "lab3_yandere_warned_stage";
    private static final String LIMIT_CLOCK = "lab3_yandere_limit_clock";
    private static final String LIMIT_AGE = "lab3_yandere_limit_age";
    private static final String REVENGE_TARGET = "lab3_yandere_revenge_target";
    private static final String EMERGENCY = "lab3_yandere_emergency";
    private static final String HOSTILE = "lab3_yandere_hostile";
    private static final String EFFECTIVE_HEARTS = "lab3_yandere_effective_hearts";
    private static final String TOTAL_HEARTS = "lab3_yandere_total_hearts";
    private static final String KILLS = "lab3_yandere_kills";
    private static final String NEXT_CHATTER = "lab3_yandere_next_chatter";
    private static final String LAST_PAIN = "lab3_yandere_last_pain";
    private static final String HERO_HP_STAGE = "lab3_yandere_hero_hp_stage";
    private static final String LAST_DEPTH = "lab3_yandere_last_depth";
    private static final String LAST_BRANCH = "lab3_yandere_last_branch";
    private static final String FLOOR_ENTER_CLOCK = "lab3_yandere_floor_enter_clock";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        ensureClocks();

        float now = globalClock();
        float affectionAge = Math.max(0f, now - affectionClock);
        float limitAge = limitReachedClock < 0f ? -1f : Math.max(0f, now - limitReachedClock);

        bundle.put(MODE, mode);
        bundle.put(MODE_BEFORE_HOSTILE, modeBeforeHostile);
        bundle.put(RAGE_BASE, rageBase);
        bundle.put(AFFECTION_CLOCK, affectionClock); // 구버전 호환용
        bundle.put(AFFECTION_AGE, affectionAge);
        bundle.put(LAST_TALK_CLOCK, lastTalkClock);
        bundle.put(WARNED_STAGE, warnedStage);
        bundle.put(LIMIT_CLOCK, limitReachedClock); // 구버전 호환용
        bundle.put(LIMIT_AGE, limitAge);
        bundle.put(REVENGE_TARGET, revengeTargetID);
        bundle.put(EMERGENCY, emergencyProtect);
        bundle.put(HOSTILE, hostileToHero);
        bundle.put(EFFECTIVE_HEARTS, effectiveHearts);
        bundle.put(TOTAL_HEARTS, totalHearts);
        bundle.put(KILLS, kills);
        bundle.put(NEXT_CHATTER, nextChatterClock);
        bundle.put(LAST_PAIN, lastPainDialogueClock);
        bundle.put(HERO_HP_STAGE, heroHpStage);
        bundle.put(LAST_DEPTH, lastDepth);
        bundle.put(LAST_BRANCH, lastBranch);
        bundle.put(FLOOR_ENTER_CLOCK, floorEnterClock);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);

        if (bundle.contains(MODE)) mode = bundle.getInt(MODE);
        if (bundle.contains(MODE_BEFORE_HOSTILE)) modeBeforeHostile = bundle.getInt(MODE_BEFORE_HOSTILE);
        if (bundle.contains(RAGE_BASE)) rageBase = bundle.getInt(RAGE_BASE);

        if (bundle.contains(AFFECTION_AGE)) {
            affectionClock = globalClock() - Math.max(0f, bundle.getFloat(AFFECTION_AGE));
        } else if (bundle.contains(AFFECTION_CLOCK)) {
            affectionClock = bundle.getFloat(AFFECTION_CLOCK);
        }

        if (bundle.contains(LAST_TALK_CLOCK)) lastTalkClock = bundle.getFloat(LAST_TALK_CLOCK);
        if (bundle.contains(WARNED_STAGE)) warnedStage = bundle.getInt(WARNED_STAGE);

        if (bundle.contains(LIMIT_AGE)) {
            float age = bundle.getFloat(LIMIT_AGE);
            limitReachedClock = age < 0f ? -1f : globalClock() - Math.max(0f, age);
        } else if (bundle.contains(LIMIT_CLOCK)) {
            limitReachedClock = bundle.getFloat(LIMIT_CLOCK);
        }

        if (bundle.contains(REVENGE_TARGET)) revengeTargetID = bundle.getInt(REVENGE_TARGET);
        if (bundle.contains(EMERGENCY)) emergencyProtect = bundle.getBoolean(EMERGENCY);
        if (bundle.contains(HOSTILE)) hostileToHero = bundle.getBoolean(HOSTILE);
        if (bundle.contains(EFFECTIVE_HEARTS)) effectiveHearts = bundle.getInt(EFFECTIVE_HEARTS);
        if (bundle.contains(TOTAL_HEARTS)) totalHearts = bundle.getInt(TOTAL_HEARTS);
        if (bundle.contains(KILLS)) kills = bundle.getInt(KILLS);
        if (bundle.contains(NEXT_CHATTER)) nextChatterClock = bundle.getFloat(NEXT_CHATTER);
        if (bundle.contains(LAST_PAIN)) lastPainDialogueClock = bundle.getFloat(LAST_PAIN);
        if (bundle.contains(HERO_HP_STAGE)) heroHpStage = bundle.getInt(HERO_HP_STAGE);
        if (bundle.contains(LAST_DEPTH)) lastDepth = bundle.getInt(LAST_DEPTH);
        if (bundle.contains(LAST_BRANCH)) lastBranch = bundle.getInt(LAST_BRANCH);
        if (bundle.contains(FLOOR_ENTER_CLOCK)) floorEnterClock = bundle.getFloat(FLOOR_ENTER_CLOCK);

        alignment = hostileToHero ? Alignment.ENEMY : Alignment.ALLY;
        attacksAutomatically = hostileToHero || emergencyProtect || mode != MODE_PEACE;
    }
}
