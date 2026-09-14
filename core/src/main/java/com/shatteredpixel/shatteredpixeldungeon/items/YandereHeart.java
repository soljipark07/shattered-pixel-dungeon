package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.YandereAlly;
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
                + "광란도 20 이상일 때 건넨 하트만 '유효 하트'로 기록된다. "
                + "한꺼번에 몰아먹여 성장치를 쌓는 것은 막아둔 상태다.";
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
            GLog.p("하트를 건넸다. 얀데레의 광란도가 0으로 돌아갔다.");
            Item.updateQuickslot();
            return;
        }

        super.onThrow(cell);
    }
}
