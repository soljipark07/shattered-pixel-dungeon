package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

public class DeveloperTool extends Item {

    private static final String AC_USE = "USE";
    private static final int PAGE_SIZE = 6;

    {
        image = ItemSpriteSheet.STYLUS;
        unique = true;
        defaultAction = AC_USE;
    }

    @Override
    public String name() {
        return "개발자 도구";
    }

    @Override
    public String desc() {
        return "녹픽던 실험실용 개발자 도구다. 원본 게임의 실제 아이템을 생성하거나 강화의 주문서를 원하는 만큼 받을 수 있다.";
    }

    @Override
    public boolean isUpgradable() {
        return false;
    }

    @Override
    public boolean isIdentified() {
        return true;
    }

    @Override
    public int value() {
        return 0;
    }

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = new ArrayList<>();
        actions.add(AC_USE);
        return actions;
    }

    @Override
    public String actionName(String action, Hero hero) {
        if (AC_USE.equals(action)) return "사용";
        return super.actionName(action, hero);
    }

    @Override
    public void execute(Hero hero, String action) {
        super.execute(hero, action);

        if (AC_USE.equals(action)) {
            showMainMenu(hero);
        }
    }

    private void showMainMenu(final Hero hero) {
        GameScene.show(new WndOptions(
                "개발자 도구",
                "무엇을 실험할까?",
                "강화의 주문서",
                "장비 / 전투 아이템",
                "소모품 / 기타 아이템"
        ) {
            @Override
            protected void onSelect(int index) {
                switch (index) {
                    case 0: showUpgradeMenu(hero); break;
                    case 1: showGearMenu(hero); break;
                    case 2: showConsumableMenu(hero); break;
                }
            }
        });
    }

    private void showUpgradeMenu(final Hero hero) {
        GameScene.show(new WndOptions(
                "강화의 주문서",
                "원하는 수량을 선택해.",
                "1장",
                "10장",
                "100장"
        ) {
            @Override
            protected void onSelect(int index) {
                int amount;
                if (index == 0) amount = 1;
                else if (index == 1) amount = 10;
                else amount = 100;

                ScrollOfUpgrade scroll = new ScrollOfUpgrade();
                scroll.quantity(amount);
                scroll.identify();

                if (!scroll.collect()) {
                    Dungeon.level.drop(scroll, hero.pos);
                }

                Item.updateQuickslot();
                showUpgradeMenu(hero);
            }
        });
    }

    private void showGearMenu(final Hero hero) {
        GameScene.show(new WndOptions(
                "장비 / 전투 아이템",
                "종류를 선택해.",
                "근접 무기",
                "투척 무기",
                "방어구",
                "마법막대",
                "반지",
                "유물",
                "장신구"
        ) {
            @Override
            protected void onSelect(int index) {
                switch (index) {
                    case 0:
                        showItemPage(hero, "근접 무기",
                                concat(
                                        Generator.Category.WEP_T1,
                                        Generator.Category.WEP_T2,
                                        Generator.Category.WEP_T3,
                                        Generator.Category.WEP_T4,
                                        Generator.Category.WEP_T5
                                ), 0);
                        break;
                    case 1:
                        showItemPage(hero, "투척 무기",
                                concat(
                                        Generator.Category.MIS_T1,
                                        Generator.Category.MIS_T2,
                                        Generator.Category.MIS_T3,
                                        Generator.Category.MIS_T4,
                                        Generator.Category.MIS_T5
                                ), 0);
                        break;
                    case 2:
                        showItemPage(hero, "방어구", Generator.Category.ARMOR.classes, 0);
                        break;
                    case 3:
                        showItemPage(hero, "마법막대", Generator.Category.WAND.classes, 0);
                        break;
                    case 4:
                        showItemPage(hero, "반지", Generator.Category.RING.classes, 0);
                        break;
                    case 5:
                        showItemPage(hero, "유물", Generator.Category.ARTIFACT.classes, 0);
                        break;
                    case 6:
                        showItemPage(hero, "장신구", Generator.Category.TRINKET.classes, 0);
                        break;
                }
            }
        });
    }

    private void showConsumableMenu(final Hero hero) {
        GameScene.show(new WndOptions(
                "소모품 / 기타",
                "종류를 선택해.",
                "물약",
                "주문서",
                "씨앗",
                "룬석",
                "음식"
        ) {
            @Override
            protected void onSelect(int index) {
                switch (index) {
                    case 0:
                        showItemPage(hero, "물약", Generator.Category.POTION.classes, 0);
                        break;
                    case 1:
                        showItemPage(hero, "주문서", Generator.Category.SCROLL.classes, 0);
                        break;
                    case 2:
                        showItemPage(hero, "씨앗", Generator.Category.SEED.classes, 0);
                        break;
                    case 3:
                        showItemPage(hero, "룬석", Generator.Category.STONE.classes, 0);
                        break;
                    case 4:
                        showItemPage(hero, "음식", Generator.Category.FOOD.classes, 0);
                        break;
                }
            }
        });
    }

    private Class<?>[] concat(Generator.Category... categories) {
        ArrayList<Class<?>> result = new ArrayList<>();

        for (Generator.Category category : categories) {
            for (Class<?> c : category.classes) {
                result.add(c);
            }
        }

        return result.toArray(new Class<?>[0]);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void showItemPage(final Hero hero, final String title,
                              final Class<?>[] classes, final int page) {

        final int start = page * PAGE_SIZE;
        final int end = Math.min(start + PAGE_SIZE, classes.length);
        final int itemCount = end - start;

        final boolean hasPrev = page > 0;
        final boolean hasNext = end < classes.length;

        int buttonCount = itemCount;
        if (hasPrev) buttonCount++;
        if (hasNext) buttonCount++;

        final String[] options = new String[buttonCount];

        for (int i = 0; i < itemCount; i++) {
            Class<?> cls = classes[start + i];
            Item preview = (Item) Reflection.newInstance((Class) cls);

            if (preview != null) {
                options[i] = preview.name();
            } else {
                options[i] = cls.getSimpleName();
            }
        }

        int pos = itemCount;

        if (hasPrev) {
            options[pos++] = "← 이전";
        }

        if (hasNext) {
            options[pos] = "다음 →";
        }

        GameScene.show(new WndOptions(
                title,
                "원하는 아이템을 선택해.",
                options
        ) {
            @Override
            protected void onSelect(int index) {

                if (index < itemCount) {
                    Class<?> cls = classes[start + index];

                    Item item = (Item) Reflection.newInstance((Class) cls);

                    if (item != null) {
                        item.cursed = false;
                        item.cursedKnown = true;
                        item.identify();

                        if (!item.collect()) {
                            Dungeon.level.drop(item, hero.pos);
                        }

                        Item.updateQuickslot();
                    }

                    showItemPage(hero, title, classes, page);
                    return;
                }

                int navIndex = itemCount;

                if (hasPrev) {
                    if (index == navIndex) {
                        showItemPage(hero, title, classes, page - 1);
                        return;
                    }
                    navIndex++;
                }

                if (hasNext && index == navIndex) {
                    showItemPage(hero, title, classes, page + 1);
                }
            }
        });
    }
}
