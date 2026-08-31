package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/**
 * 정식 플레이용 성장형 얀데레.
 *
 * AI/광란/대화 구조는 YandereAlly를 그대로 공유하고, 기본 전투 능력과
 * 자연회복만 성장형 규칙으로 덮어쓴다. 특수능력 해금은 다음 단계에서 추가한다.
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

    private int growthHearts = 0;
    private float lastRegenClock = -1f;

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

        int healPerPulse = Math.max(1, Math.round(HT * BASE_REGEN_PERCENT));
        HP = Math.min(HT, HP + healPerPulse * pulses);
        lastRegenClock += pulses * REGEN_INTERVAL;
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

        return result;
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
        return Random.NormalIntRange(growthAttackMin(), growthAttackMax());
    }

    @Override
    public int drRoll() {
        return Random.NormalIntRange(growthDrMin(), growthDrMax());
    }

    @Override
    public float speed() {
        // 성장형 기본 이동속도와 광란 추격 모두 1배속.
        return 1f;
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
        return growthAttackMin();
    }

    @Override
    public int statAttackMax() {
        return hostileToHero() ? statAttackMin() : growthAttackMax();
    }

    @Override
    public int statDrMin() {
        return growthDrMin();
    }

    @Override
    public int statDrMax() {
        return growthDrMax();
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
        return 1f;
    }

    @Override
    public float statAttackSpeed() {
        return 1f;
    }

    public float statRegenPercent() {
        return BASE_REGEN_PERCENT;
    }

    public float statRegenInterval() {
        return REGEN_INTERVAL;
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
    }
}
