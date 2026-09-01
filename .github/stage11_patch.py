from pathlib import Path


def replace_once(text, old, new, label):
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f'{label}: anchor not found')
    return text.replace(old, new, 1)

# --- YandereHeart: add zero-energy alchemy recipe and document it. ---
p = Path('core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/YandereHeart.java')
t = p.read_text()

t = replace_once(t,
'''import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.YandereJealousy;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;''',
'''import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.YandereJealousy;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;''',
'heart imports')

t = replace_once(t,
'''                + "다른 대상에게 잘못 던진 하트는 소모되지 않고 그 대상의 발밑에 떨어지지만, 성장형 얀데레의 질투를 자극할 수 있다.";''',
'''                + "다른 대상에게 잘못 던진 하트는 소모되지 않고 그 대상의 발밑에 떨어지지만, 성장형 얀데레의 질투를 자극할 수 있다.\\n\\n"
                + "연금술 솥에서 강화의 주문서 1장과 치유 물약 1개를 합치면 연금술 에너지 없이 이 하트 1개를 만들 수 있다.";''',
'heart description')

recipe_block = '''
    public static class Recipe extends com.shatteredpixel.shatteredpixeldungeon.items.Recipe.SimpleRecipe {
        {
            inputs = new Class[]{ScrollOfUpgrade.class, PotionOfHealing.class};
            inQuantity = new int[]{1, 1};
            cost = 0;
            output = YandereHeart.class;
            outQuantity = 1;
        }
    }
'''
if 'public static class Recipe extends com.shatteredpixel.shatteredpixeldungeon.items.Recipe.SimpleRecipe' not in t:
    idx = t.rfind('\n}')
    if idx < 0:
        raise SystemExit('heart class end not found')
    t = t[:idx] + recipe_block + t[idx:]

p.write_text(t)

# --- Register the two-ingredient recipe. ---
p = Path('core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/Recipe.java')
t = p.read_text()
t = replace_once(t,
'''\tprivate static Recipe[] twoIngredientRecipes = new Recipe[]{
\t\tnew Blandfruit.CookFruit(),''',
'''\tprivate static Recipe[] twoIngredientRecipes = new Recipe[]{
\t\tnew YandereHeart.Recipe(),
\t\tnew Blandfruit.CookFruit(),''',
'recipe registration')
p.write_text(t)

# --- RedRibbon: growth deaths now require two inventory hearts to revive. ---
p = Path('core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/RedRibbon.java')
t = p.read_text()

t = replace_once(t,
'''    public static final int MAX_GROWTH_HEARTS = 48;

    private static final float ABANDONMENT_DAMAGE_MULTIPLIER = 0.80f;''',
'''    public static final int MAX_GROWTH_HEARTS = 48;
    public static final int GROWTH_REVIVE_HEART_COST = 2;

    private static final float ABANDONMENT_DAMAGE_MULTIPLIER = 0.80f;''',
'revive cost constant')

t = replace_once(t,
'''    private int profile = PROFILE_CHEAT;
    private boolean profileLocked = false;
    private int growthHearts = 0;

    private boolean abandonmentActive = false;''',
'''    private int profile = PROFILE_CHEAT;
    private boolean profileLocked = false;
    private int growthHearts = 0;
    private boolean growthRevivalPending = false;

    private boolean abandonmentActive = false;''',
'revival field')

# Clear stale death state when profile is explicitly selected before first summon.
t = t.replace('''                    growthHearts = 0;
                    savedGrowthHP = -1;''', '''                    growthHearts = 0;
                    growthRevivalPending = false;
                    savedGrowthHP = -1;''')

# Label dead growth companion distinctly in the ribbon menu.
t = replace_once(t,
'''    private String primaryCommandLabel(YandereAlly ally) {
        if (ally == null) return "소환";''',
'''    private String primaryCommandLabel(YandereAlly ally) {
        if (ally == null) {
            if (isGrowthProfile() && growthRevivalPending) return "하트 2개로 되살리기";
            return "소환";
        }''',
'primary command label')

# Add inventory heart helpers and explicit revival path before spawnOrRecall.
anchor = '''    private void spawnOrRecall(Hero hero, boolean manual) {'''
helpers = '''    private int inventoryHeartCount(Hero hero) {
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
        revived.yell("아하하♡ 다시 왔어! 하트 두 개나 써서 살려줬네. 역시 나 버릴 리 없지♡");
        GLog.p("애정의 하트 2개를 희생해서 성장형 얀데레를 최대 HP로 되살렸다.");
    }

'''
if 'private int inventoryHeartCount(Hero hero)' not in t:
    t = replace_once(t, anchor, helpers + anchor, 'revival helpers')

# Block old free respawn while a growth death is pending.
t = replace_once(t,
'''        summoned = true;
        createOnCurrentFloor(hero, manual);
    }

    private void createOnCurrentFloor(Hero hero, boolean manual) {
        if (Dungeon.level == null || hero == null || findAlly() != null) return;''',
'''        if (isGrowthProfile() && growthRevivalPending) {
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
        if (Dungeon.level == null || hero == null || findAlly() != null) return;''',
'free respawn gate')

# Avoid normal floor-arrival chatter on a true resurrection.
t = replace_once(t,
'''        if (manual && !savedHostile) ally.yell("찾았다♡ 이제 나 두고 혼자 가지 마. 끝까지 같이 가는 거야.");
        else if (!manual && !savedHostile) ally.queueFloorArrivalDialogue(false);
        else if (savedHostile) ally.queueFloorArrivalDialogue(true);''',
'''        if (revival) {
            // reviveGrowth emits its own line after the two hearts are actually consumed.
        } else if (manual && !savedHostile) ally.yell("찾았다♡ 이제 나 두고 혼자 가지 마. 끝까지 같이 가는 거야.");
        else if (!manual && !savedHostile) ally.queueFloorArrivalDialogue(false);
        else if (savedHostile) ally.queueFloorArrivalDialogue(true);''',
'revival chatter suppression')

# Any living captured ally means there is no outstanding death to pay for.
t = replace_once(t,
'''        allyID = ally.id();
        summoned = true;
        profileLocked = true;''',
'''        allyID = ally.id();
        summoned = true;
        growthRevivalPending = false;
        profileLocked = true;''',
'capture clears pending')

# Mark only growth-profile deaths as requiring the 2-heart revival.
t = replace_once(t,
'''        ribbon.summoned = false;
        ribbon.allyID = 0;
        ribbon.savedHostile = false;
        ribbon.savedRage = 0;
        ribbon.savedGrowthHP = -1;
        ribbon.profileLocked = true;''',
'''        ribbon.summoned = false;
        ribbon.allyID = 0;
        ribbon.savedHostile = false;
        ribbon.savedRage = 0;
        ribbon.savedGrowthHP = -1;
        ribbon.growthRevivalPending = ribbon.isGrowthProfile() && ally instanceof GrowthYandereAlly;
        ribbon.profileLocked = true;''',
'death pending flag')

# Status wording when dead.
t = replace_once(t,
'''                    + "\\n\\n" + (summoned ? "층 이동 직후라면 영웅이 한 번 행동하면 자동으로 따라온다." : "현재 소환된 얀데레가 없어.");''',
'''                    + "\\n\\n" + (growthRevivalPending && isGrowthProfile()
                        ? "성장형 얀데레가 죽었다. 애정의 하트 2개를 희생하면 최대 HP로 되살릴 수 있다."
                        : summoned ? "층 이동 직후라면 영웅이 한 번 행동하면 자동으로 따라온다." : "현재 소환된 얀데레가 없어.");''',
'dead status')

# Persist pending-death state.
t = replace_once(t,
'''    private static final String GROWTH_HEARTS = "lab3_yandere_growth_hearts";
    private static final String HEART_WARNING_DEPTH = "lab3_yandere_heart_warning_depth";''',
'''    private static final String GROWTH_HEARTS = "lab3_yandere_growth_hearts";
    private static final String GROWTH_REVIVAL_PENDING = "lab3_yandere_growth_revival_pending";
    private static final String HEART_WARNING_DEPTH = "lab3_yandere_heart_warning_depth";''',
'revival bundle key')

t = replace_once(t,
'''        bundle.put(GROWTH_HEARTS, growthHearts);
        bundle.put(HEART_WARNING_DEPTH, lastHeartWarningDepth);''',
'''        bundle.put(GROWTH_HEARTS, growthHearts);
        bundle.put(GROWTH_REVIVAL_PENDING, growthRevivalPending);
        bundle.put(HEART_WARNING_DEPTH, lastHeartWarningDepth);''',
'revival store')

t = replace_once(t,
'''        if (bundle.contains(GROWTH_HEARTS)) growthHearts = Math.max(0, Math.min(MAX_GROWTH_HEARTS, bundle.getInt(GROWTH_HEARTS)));
        if (bundle.contains(HEART_WARNING_DEPTH)) lastHeartWarningDepth = bundle.getInt(HEART_WARNING_DEPTH);''',
'''        if (bundle.contains(GROWTH_HEARTS)) growthHearts = Math.max(0, Math.min(MAX_GROWTH_HEARTS, bundle.getInt(GROWTH_HEARTS)));
        growthRevivalPending = bundle.contains(GROWTH_REVIVAL_PENDING) && bundle.getBoolean(GROWTH_REVIVAL_PENDING);
        if (bundle.contains(HEART_WARNING_DEPTH)) lastHeartWarningDepth = bundle.getInt(HEART_WARNING_DEPTH);''',
'revival restore')

# Old saves cannot have a pending paid revival state.
t = replace_once(t,
'''            growthHearts = 0;
            savedGrowthHP = -1;''',
'''            growthHearts = 0;
            growthRevivalPending = false;
            savedGrowthHP = -1;''',
'old save fallback')

p.write_text(t)
