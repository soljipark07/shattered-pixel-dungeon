package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.GrowthYandereAlly;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.YandereAlly;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.YandereJealousy;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

public class YandereHeart extends Item {

    {
        image = ItemSpriteSheet.YANDERE_HEART;
        stackable = true;
        defaultAction = AC_THROW;
    }

    @Override
    public String name() {
        return "애정의 하트";
    }

    @Override
    public String desc() {
        return "얀데레에게 애정을 확실하게 전하는 하트다.\n\n"
                + "얀데레에게 직접 던지면 광란도가 즉시 0으로 초기화되고 애정 타이머도 처음부터 다시 시작한다. "
                + "광란 추격 중에도 통한다.\n\n"
                + "성장형 얀데레에게 직접 건넨 하트는 타이밍과 관계없이 성장 하트로 1개 기록되며, 최대 48개까지 쌓인다. "
                + "광란도 20 이상일 때 건넨 하트만 기록되는 '유효 하트'는 성장 하트와 별개의 관계 기록이다.\n\n"
                + "다른 대상에게 잘못 던진 하트는 소모되지 않고 그 대상의 발밑에 떨어지지만, 성장형 얀데레의 질투를 자극할 수 있다.";
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
    protected void onThrow(int cell) {
        Char ch = Actor.findChar(cell);
        if (ch instanceof YandereAlly) {
            ((YandereAlly)ch).receiveHeart();

            RedRibbon ribbon = Dungeon.hero == null
                ? null : RedRibbon.findRibbonForRun();
            boolean grew = ribbon != null && ribbon.recordGrowthHeart();

            if (grew) {
                if (ch instanceof GrowthYandereAlly) {
                    ((GrowthYandereAlly)ch).syncGrowthHearts(ribbon.growthHearts());
                }
                GLog.p("하트를 건넸다. 광란도가 0으로 돌아갔고 성장 하트가 "
                        + ribbon.growthHearts() + "/" + RedRibbon.MAX_GROWTH_HEARTS + "이 됐다.");
            } else {
                GLog.p("하트를 건넸다. 얀데레의 광란도가 0으로 돌아갔다.");
            }

            if (ch instanceof GrowthYandereAlly) {
                YandereJealousy.onHeartGivenProperly((GrowthYandereAlly)ch);
            }

            Item.updateQuickslot();
            return;
        }

        // Keep normal throwable-item behavior first: the heart lands at the
        // target's feet and is not consumed. Jealousy is only a reaction to it.
        super.onThrow(cell);
        if (ch != null) {
            YandereJealousy.onHeartMisthrown(ch);
        }
    }
}
