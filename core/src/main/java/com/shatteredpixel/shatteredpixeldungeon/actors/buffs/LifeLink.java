/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

public class LifeLink extends FlavourBuff {

	public int object = 0;

	// LAB3 growth-yandere: internal damage-sharing links should not appear as
	// the cleric Life Link buff in the HUD or announce themselves to the hero.
	// The base game's Life Link behavior remains unchanged when this is false.
	public boolean yandereGuardLink = false;

	private static final String OBJECT    = "object";
	private static final String YANDERE_GUARD_LINK = "lab3_yandere_guard_link";

	{
		type = buffType.POSITIVE;
		announced = true;
	}

	public static LifeLink attachYandereGuardLink(Char target, int object, float duration) {
		LifeLink link = new LifeLink();
		link.object = object;
		link.yandereGuardLink = true;
		link.announced = false;

		if (link.attachTo(target)) {
			link.spend(duration * target.resist(LifeLink.class));
			return link;
		}

		return null;
	}

	@Override
	public void detach() {
		super.detach();
		Char ch = (Char)Actor.findById(object);
		if (!target.isActive() && ch != null){
			for (LifeLink l : ch.buffs(LifeLink.class)){
				if (l.object == target.id()){
					l.detach();
				}
			}
		}
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( OBJECT, object );
		bundle.put( YANDERE_GUARD_LINK, yandereGuardLink );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		object = bundle.getInt( OBJECT );
		yandereGuardLink = bundle.getBoolean( YANDERE_GUARD_LINK );
		if (yandereGuardLink) announced = false;
	}

	@Override
	public int icon() {
		return yandereGuardLink ? BuffIndicator.NONE : BuffIndicator.HERB_HEALING;
	}

	@Override
	public void tintIcon(Image icon) {
		if (!yandereGuardLink) icon.hardlight(1, 0, 1);
	}

	@Override
	public float iconFadePercent() {
		if (yandereGuardLink) return 0f;
		int duration = Math.round(6.67f + 3.33f*Dungeon.hero.pointsInTalent(Talent.LIFE_LINK));
		return Math.max(0, (duration - visualcooldown()) / duration);
	}

}
