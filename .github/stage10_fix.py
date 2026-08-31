from pathlib import Path
import re

def replace_once(text, old, new, label):
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f'{label}: anchor not found')
    return text.replace(old, new, 1)

# RedRibbon
p = Path('core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/RedRibbon.java')
t = p.read_text()

t = replace_once(t,
"""    private static final float ABANDONMENT_DAMAGE_MULTIPLIER = 0.80f;
    private static final float ABANDONMENT_ATTACK_SPEED_MULTIPLIER = 0.80f;
    private static final float ABANDONMENT_STAGE_1 = 30f;
    private static final float ABANDONMENT_STAGE_2 = 80f;
    private static final float ABANDONMENT_STAGE_3 = 150f;""",
"""    private static final float ABANDONMENT_DAMAGE_MULTIPLIER = 0.80f;
    private static final float ABANDONMENT_ATTACK_SPEED_MULTIPLIER = 0.80f;
    private static final float ABANDONMENT_MOVE_SPEED_MULTIPLIER = 0.80f;
    private static final float ABANDONMENT_STAGE_1 = 30f;
    private static final float ABANDONMENT_STAGE_2 = 80f;
    private static final float ABANDONMENT_STAGE_3 = 150f;
    private static final float ABANDONMENT_STAGE_4 = 220f;""", 'ribbon constants')

t = replace_once(t,
"""    private boolean abandonmentActive = false;
    private float abandonmentStartClock = -1f;
    private int abandonmentStage = -1;""",
"""    private boolean abandonmentActive = false;
    private float abandonmentStartClock = -1f;
    private float abandonmentNextChatterClock = -1f;
    private int abandonmentStage = -1;""", 'ribbon fields')

t = replace_once(t,
"""            abandonmentActive = true;
            abandonmentStartClock = globalCurseClock();
            abandonmentStage = 0;
            speakAbandonmentLine(0);""",
"""            abandonmentActive = true;
            abandonmentStartClock = globalCurseClock();
            abandonmentNextChatterClock = abandonmentStartClock + ABANDONMENT_STAGE_1;
            abandonmentStage = 0;
            speakAbandonmentLine(0);""", 'drop start')

t = replace_once(t,
"""    public static float abandonmentAttackDelayMultiplier(Hero hero) {
        return abandonmentCurseActive(hero) ? 1f / ABANDONMENT_ATTACK_SPEED_MULTIPLIER : 1f;
    }""",
"""    public static float abandonmentAttackDelayMultiplier(Hero hero) {
        return abandonmentCurseActive(hero) ? 1f / ABANDONMENT_ATTACK_SPEED_MULTIPLIER : 1f;
    }

    public static float abandonmentMoveSpeedMultiplier(Hero hero) {
        return abandonmentCurseActive(hero) ? ABANDONMENT_MOVE_SPEED_MULTIPLIER : 1f;
    }""", 'move multiplier method')

pattern = re.compile(r"    private void updateAbandonmentCurse\(\) \{.*?\n    public boolean recordGrowthHeart\(\) \{", re.S)
replacement = """    private float abandonmentRepeatInterval(int stage) {
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

    public boolean recordGrowthHeart() {"""
if 'private float abandonmentRepeatInterval' not in t:
    t, n = pattern.subn(replacement, t, count=1)
    if n != 1:
        raise SystemExit('abandonment methods block not found')

old_ensure = """    public static void ensureYanderePresent() {
        if (Dungeon.hero == null || Dungeon.level == null) return;
        RedRibbon carried = Dungeon.hero.belongings.getItem(RedRibbon.class);
        RedRibbon ribbon = carried;
        if (ribbon == null) {
            RibbonTransit transit = Dungeon.hero.buff(RibbonTransit.class);
            if (transit != null && transit.ribbon != null) {
                ribbon = transit.takeRibbon();
                int cell = ribbonDropCell(Dungeon.hero);
                if (cell >= 0) {
                    Heap heap = Dungeon.level.drop(ribbon, cell);
                    if (heap.sprite != null) heap.sprite.drop(Dungeon.hero.pos);
                    GLog.w("버린 붉은 리본이 계단 근처까지 따라왔다.");
                }
            } else {
                ribbon = findFloorRibbon();
            }
        }
        if (ribbon == null) return;
        if (carried != null && ribbon.abandonmentActive) ribbon.stopAbandonmentCurse(false);
        ribbon.updateAbandonmentCurse();
        if (!ribbon.summoned) return;
        YandereAlly ally = ribbon.findAlly();
        if (ally == null) ribbon.createOnCurrentFloor(Dungeon.hero, false);
        else ribbon.captureFrom(ally);
    }"""
new_ensure = """    public static void ensureYanderePresent() {
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
    }"""
t = replace_once(t, old_ensure, new_ensure, 'ensure floor-follow')

t = replace_once(t,
"""    private static final String ABANDONMENT_ACTIVE = "lab3_yandere_abandonment_active";
    private static final String ABANDONMENT_AGE = "lab3_yandere_abandonment_age";
    private static final String ABANDONMENT_STAGE = "lab3_yandere_abandonment_stage";""",
"""    private static final String ABANDONMENT_ACTIVE = "lab3_yandere_abandonment_active";
    private static final String ABANDONMENT_AGE = "lab3_yandere_abandonment_age";
    private static final String ABANDONMENT_NEXT_CHATTER = "lab3_yandere_abandonment_next_chatter";
    private static final String ABANDONMENT_STAGE = "lab3_yandere_abandonment_stage";""", 'bundle const')

t = replace_once(t,
"""        bundle.put(ABANDONMENT_AGE, abandonmentActive && abandonmentStartClock >= 0f
                ? Math.max(0f, globalCurseClock() - abandonmentStartClock) : 0f);
        bundle.put(ABANDONMENT_STAGE, abandonmentStage);""",
"""        bundle.put(ABANDONMENT_AGE, abandonmentActive && abandonmentStartClock >= 0f
                ? Math.max(0f, globalCurseClock() - abandonmentStartClock) : 0f);
        bundle.put(ABANDONMENT_NEXT_CHATTER, abandonmentActive && abandonmentNextChatterClock >= 0f
                ? Math.max(0f, abandonmentNextChatterClock - globalCurseClock()) : 0f);
        bundle.put(ABANDONMENT_STAGE, abandonmentStage);""", 'bundle store')

t = replace_once(t,
"""        float abandonmentAge = bundle.contains(ABANDONMENT_AGE) ? Math.max(0f, bundle.getFloat(ABANDONMENT_AGE)) : 0f;
        abandonmentStartClock = abandonmentActive ? globalCurseClock() - abandonmentAge : -1f;
        abandonmentStage = bundle.contains(ABANDONMENT_STAGE) ? bundle.getInt(ABANDONMENT_STAGE) : -1;""",
"""        float abandonmentAge = bundle.contains(ABANDONMENT_AGE) ? Math.max(0f, bundle.getFloat(ABANDONMENT_AGE)) : 0f;
        abandonmentStartClock = abandonmentActive ? globalCurseClock() - abandonmentAge : -1f;
        float abandonmentNext = bundle.contains(ABANDONMENT_NEXT_CHATTER)
                ? Math.max(0f, bundle.getFloat(ABANDONMENT_NEXT_CHATTER)) : 0f;
        abandonmentStage = bundle.contains(ABANDONMENT_STAGE) ? bundle.getInt(ABANDONMENT_STAGE) : -1;
        abandonmentNextChatterClock = abandonmentActive
                ? globalCurseClock() + (abandonmentNext > 0f ? abandonmentNext : abandonmentRepeatInterval(Math.max(0, abandonmentStage)))
                : -1f;""", 'bundle restore')

p.write_text(t)

# Hero move speed
p = Path('core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/Hero.java')
t = p.read_text()
t = replace_once(t,
"""\t\tspeed = AscensionChallenge.modifyHeroSpeed(speed);
\t\t
\t\treturn speed;""",
"""\t\tspeed = AscensionChallenge.modifyHeroSpeed(speed);
\t\tspeed *= RedRibbon.abandonmentMoveSpeedMultiplier(this);
\t\t
\t\treturn speed;""", 'hero move speed')
p.write_text(t)

# GrowthYandereAlly
p = Path('core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/mobs/npcs/GrowthYandereAlly.java')
t = p.read_text()
t = replace_once(t,
"""    private static final float HIGH_LAUGH_COOLDOWN = 70f;""",
"""    private static final float HIGH_LAUGH_COOLDOWN = 70f;
    private static final float KILL_LAUGH_COOLDOWN = 12f;""", 'growth laugh constant')
t = replace_once(t,
"""    private float lastHighLaughClock = -999999f;""",
"""    private float lastHighLaughClock = -999999f;
    private float lastKillLaughClock = -999999f;""", 'growth kill laugh field')
t = replace_once(t,
"""                yell("사랑해사랑해사랑해♡ 이제 됐어. 이제 아무것도 부족하지 않아.");""",
"""                Sample.INSTANCE.play(Assets.Sounds.YANDERE_LAUGH_HIGH);
                lastHighLaughClock = globalGrowthClock();
                yell("사랑해. 사랑해 사랑해 사랑해 사랑해♡ 아하하하하하! 나도 알아! 이제 알아! 너도 나 사랑하는 거잖아!!");""", 'final awakening line')
t = t.replace('"사랑해사랑해사랑해♡ 이제 됐어. 이제 아무것도 부족하지 않아."', '"사랑해. 사랑해 사랑해 사랑해 사랑해♡ 아하하하하하! 나도 알아! 이제 알아! 너도 나 사랑하는 거잖아!!"')

t = replace_once(t,
"""        if (tier >= 4) return pick(
                "누가 너 건드렸어? 말 안 해도 돼♡ 내가 찾아서 없애면 되니까.",
                "아프지 마. 네가 다치는 건 아직도 싫어. 그건 절대 익숙해질 생각 없어.",
                "이리 와♡ 네가 멀쩡해질 때까지 내가 전부 막을게.",
                "너 건드린 건 기억했어♡ 이제 걔가 얼마나 버티는지만 보면 돼.");
        if (tier == 3) return pick(
                "누가 이랬어! 어디 있어! 내가 지금 바로 분질러버릴 거야!",
                "피 봐. 네 피잖아. 헤헤, 나 진짜 화나네. 쟤부터 없앨게.",
                "아프지 마! 너 건드린 새끼는 내가 다시는 못 움직이게 할 거야!",
                "내가 옆에 있는데 감히 너를 이렇게 만들어? 걔 어디 있어!");""",
"""        if (tier >= 4) return pick(
                "아하하하하! 괜찮아♡ 내가 다 죽이면 돼. 너 건드린 것들 하나도 안 남기면 되잖아♡",
                "누가 너 건드렸어? 말 안 해도 돼♡ 내가 찾아서 없애면 되니까.",
                "아프지 마♡ 내가 전부 없애줄게. 너는 그냥 내 옆에 있으면 돼.",
                "너 건드린 건 기억했어♡ 이제 하나도 안 남길 거야.");
        if (tier == 3) return pick(
                "누가 너 이렇게 만들었어?! 말해! 어디 있어?! 내가 지금 다 찢어버릴 거야!",
                "누가 이랬어! 어디 있어! 내가 지금 바로 분질러버릴 거야!",
                "피 봐. 네 피잖아. 헤헤, 나 진짜 화나네. 쟤부터 없앨게.",
                "아프지 마! 너 건드린 새끼는 내가 다시는 못 움직이게 할 거야!");""", 'pain dialogue')

anchor = """    @Override
    public int damageRoll() {"""
kill_method = """    private void maybePlayKillLaugh(Char enemy, int dealtDamage) {
        if (enemy == null || enemy == Dungeon.hero || !enemy.isAlive()) return;
        if (enemy.HP - dealtDamage > 0) return;

        int tier = obsessionStage();
        float chance;
        switch (tier) {
            case 4: chance = 0.85f; break;
            case 3: chance = 0.55f; break;
            case 2: chance = 0.25f; break;
            case 1: chance = 0.10f; break;
            default: chance = 0.05f; break;
        }
        float now = globalGrowthClock();
        if (now - lastKillLaughClock < KILL_LAUGH_COOLDOWN || Random.Float() >= chance) return;

        Sample.INSTANCE.play(tier >= 3 ? Assets.Sounds.YANDERE_LAUGH_HIGH : Assets.Sounds.YANDERE_LAUGH_MILD);
        lastKillLaughClock = now;
    }

    @Override
    public int attackProc(Char enemy, int damage) {
        int result = super.attackProc(enemy, damage);
        maybePlayKillLaugh(enemy, result);
        return result;
    }

"""
if 'private void maybePlayKillLaugh' not in t:
    t = replace_once(t, anchor, kill_method + anchor, 'kill laugh hook')

t = replace_once(t,
"""    private static final String HIGH_LAUGH_AGE = "lab3_growth_yandere_high_laugh_age";""",
"""    private static final String HIGH_LAUGH_AGE = "lab3_growth_yandere_high_laugh_age";
    private static final String KILL_LAUGH_AGE = "lab3_growth_yandere_kill_laugh_age";""", 'growth bundle const')
t = replace_once(t,
"""        float laughAge = lastHighLaughClock < -900000f ? HIGH_LAUGH_COOLDOWN : Math.max(0f, now - lastHighLaughClock);
        bundle.put(GROWTH_HEARTS, growthHearts);
        bundle.put(REGEN_AGE, regenAge);
        bundle.put(HIGH_LAUGH_AGE, laughAge);""",
"""        float laughAge = lastHighLaughClock < -900000f ? HIGH_LAUGH_COOLDOWN : Math.max(0f, now - lastHighLaughClock);
        float killLaughAge = lastKillLaughClock < -900000f ? KILL_LAUGH_COOLDOWN : Math.max(0f, now - lastKillLaughClock);
        bundle.put(GROWTH_HEARTS, growthHearts);
        bundle.put(REGEN_AGE, regenAge);
        bundle.put(HIGH_LAUGH_AGE, laughAge);
        bundle.put(KILL_LAUGH_AGE, killLaughAge);""", 'growth bundle store')
t = replace_once(t,
"""        float laughAge = bundle.contains(HIGH_LAUGH_AGE) ? Math.max(0f, bundle.getFloat(HIGH_LAUGH_AGE)) : HIGH_LAUGH_COOLDOWN;
        lastRegenClock = globalGrowthClock() - Math.min(REGEN_INTERVAL, regenAge);
        lastHighLaughClock = globalGrowthClock() - Math.min(HIGH_LAUGH_COOLDOWN, laughAge);""",
"""        float laughAge = bundle.contains(HIGH_LAUGH_AGE) ? Math.max(0f, bundle.getFloat(HIGH_LAUGH_AGE)) : HIGH_LAUGH_COOLDOWN;
        float killLaughAge = bundle.contains(KILL_LAUGH_AGE) ? Math.max(0f, bundle.getFloat(KILL_LAUGH_AGE)) : KILL_LAUGH_COOLDOWN;
        lastRegenClock = globalGrowthClock() - Math.min(REGEN_INTERVAL, regenAge);
        lastHighLaughClock = globalGrowthClock() - Math.min(HIGH_LAUGH_COOLDOWN, laughAge);
        lastKillLaughClock = globalGrowthClock() - Math.min(KILL_LAUGH_COOLDOWN, killLaughAge);""", 'growth bundle restore')
p.write_text(t)

# Jealousy wording by obsession stage. Thresholds are untouched.
p = Path('core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/mobs/npcs/YandereJealousy.java')
t = p.read_text()
start = t.index('    private static boolean reactNpcInteraction')
end = t.index('    private static void executeJealousKill', start)
new_methods = """    private static boolean reactNpcInteraction(GrowthYandereAlly ally, Char target, int count) {
        int tier = ally.obsessionStage();
        if (count >= GENERAL_KILL) {
            ally.yell(tier >= 4
                    ? "얘가 날 사랑하는 건 이미 알아♡ 근데 네가 왜 자꾸 옆에 붙어 있어? 이제 그만 없어져."
                    : tier >= 3 ? "말했지! 나 다 세고 있다고! 이제 쟤 없어!"
                    : "말했지. 다음은 없다고. 이제 쟤 없어.");
            executeJealousKill(ally, target);
            return true;
        }
        if (count >= GENERAL_WARNING) {
            ally.yell(tier >= 4
                    ? "나 불안한 건 아니야♡ 그냥 내 사람한테 계속 붙는 네가 거슬리는 거야. 한 번만 더 해 봐."
                    : tier >= 3 ? "아까부터 계속 쟤한테 말 걸고 있잖아. 나 다 세고 있어. 한 번만 더 해봐."
                    : "한 번만 더 쟤한테 가 봐. 그땐 나 진짜 안 참아.");
        } else if (count >= GENERAL_STRONG) {
            ally.yell(tier >= 4
                    ? "얘가 누구를 사랑하는지는 이미 정해졌어♡ 그런데 넌 왜 아직도 옆에 있어?"
                    : tier >= 3 ? "또 쟤야? 나 지금 몇 번째인지 다 기억하고 있어. 계속 해 봐."
                    : "그만 좀 쳐다봐. 계속 그러면 쟤가 없어지는 게 더 편하겠네.");
        } else {
            ally.yell(tier >= 3
                    ? "또 쟤한테 가? 나 보고 있는데도? 진짜 기분 더럽네."
                    : count == 1 ? "왜 또 쟤랑 얘기해? 나 여기 있는데."
                    : "또 쟤야? 나 보고도 굳이 쟤한테 가야 해?");
        }
        return false;
    }

    private static boolean reactShopInteraction(GrowthYandereAlly ally, Shopkeeper target, int count) {
        int tier = ally.obsessionStage();
        if (count >= GENERAL_KILL) {
            ally.yell(tier >= 4
                    ? "물건은 없어도 돼♡ 네 옆에 저 사람은 더 필요 없고."
                    : tier >= 3 ? "됐어! 가게째 없어져. 이제 저 사람한테 갈 일도 없겠네."
                    : "됐어. 이제 가게도, 저 사람도 필요 없어.");
            executeJealousKill(ally, target);
            return true;
        }
        if (count >= GENERAL_WARNING) {
            ally.yell(tier >= 3
                    ? "또 저 사람한테 가 봐. 몇 번째인지 다 세고 있어. 가게째 없애버릴 거야."
                    : "한 번만 더 저 사람한테 가 봐. 그땐 가게째 없애버릴 거야.");
        } else if (count >= GENERAL_STRONG) {
            ally.yell(tier >= 4
                    ? "필요한 건 내가 다 해줄 수 있어♡ 저 사람한테 그렇게 자주 갈 이유 없잖아."
                    : tier >= 3 ? "필요한 것만 사고 당장 와. 왜 자꾸 저 사람 앞에 서 있는데?"
                    : "필요한 것만 사고 와. 계속 저 사람이랑 붙어 있으면 나 진짜 화나.");
        } else {
            ally.yell(count == 1
                    ? "물건만 사고 와. 왜 자꾸 저 사람이랑 말 섞어?"
                    : "또 저 사람이야? 빨리 끝내고 나한테 와.");
        }
        return false;
    }

    private static boolean reactRoseInteraction(GrowthYandereAlly ally, Char target, int count) {
        int tier = ally.obsessionStage();
        if (count >= ROSE_KILL) {
            ally.yell(tier >= 4
                    ? "걔가 네 옆에 있을 이유 없잖아♡ 이제 진짜 나만 보면 돼."
                    : "됐어! 걔 보기 싫다고 했잖아! 이제 없어. 나만 봐!");
            executeJealousKill(ally, target);
            return true;
        }
        if (count >= ROSE_WARNING) {
            ally.yell(tier >= 3
                    ? "또 걔부터 찾으면 내가 직접 없앨 거야. 장난 아니야. 진짜 죽여버릴 거야."
                    : "다음에도 걔부터 찾으면, 내가 직접 없앨 거야.");
        } else {
            ally.yell(tier >= 4
                    ? "얘가 날 사랑하는 건 이미 알아♡ 근데 쟤가 왜 네 옆에 붙어 있어?"
                    : tier >= 3 ? "또 걔야?! 걔 보기 싫다고! 지금 당장 떨어져!"
                    : "또 걔야? 하지 마. 진짜로. 걔 보기 싫어.");
        }
        return false;
    }

"""
t = t[:start] + new_methods + t[end:]
p.write_text(t)
