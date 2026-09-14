package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.YandereHeart;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.Random;

import java.util.Arrays;

/** A fixed, cache-safe sandbox generated before the level becomes active. */
public class LabLevel extends Level {

    private static final int WIDTH = 38;
    private static final int HEIGHT = 24;

    {
        color1 = 0x4b3145;
        color2 = 0x9b667e;
        viewDistance = 12;
        feeling = Feeling.NONE;
    }

    @Override
    public String tilesTex() {
        return Assets.Environment.TILES_SEWERS;
    }

    @Override
    public String waterTex() {
        return Assets.Environment.WATER_SEWERS;
    }

    @Override
    public void playLevelMusic() {
        Music.INSTANCE.play(Assets.Music.SEWERS_1, true);
    }

    @Override
    protected boolean build() {
        setSize(WIDTH, HEIGHT);
        Arrays.fill(map, Terrain.WALL);

        // Three broad zones: entrance/items, chase space, and monster testing.
        Painter.fill(this, 1, 1, WIDTH - 2, HEIGHT - 2, Terrain.EMPTY);
        Painter.fill(this, 12, 2, 1, HEIGHT - 4, Terrain.WALL);
        Painter.fill(this, 26, 2, 1, HEIGHT - 4, Terrain.WALL);
        Painter.fill(this, 12, 10, 1, 4, Terrain.EMPTY);
        Painter.fill(this, 26, 10, 1, 4, Terrain.EMPTY);

        // Item display pedestals in the left-hand test zone.
        int[][] pedestals = {
                {4, 5}, {7, 5}, {10, 5},
                {4, 8}, {7, 8}, {10, 8},
                {4, 15}, {7, 15}, {10, 15}
        };
        for (int[] p : pedestals) {
            map[p[0] + p[1] * width()] = Terrain.PEDESTAL;
        }

        // A small water strip makes movement/flying tests possible without blocking the hall.
        Painter.fill(this, 16, 18, 7, 2, Terrain.WATER);

        int entranceCell = 3 + 12 * width();
        int exitCell = 34 + 12 * width();
        map[entranceCell] = Terrain.ENTRANCE;
        map[exitCell] = Terrain.EXIT;
        entrance = entranceCell;
        exit = exitCell;

        transitions.add(new LevelTransition(this, entranceCell,
                LevelTransition.Type.REGULAR_ENTRANCE));
        transitions.add(new LevelTransition(this, exitCell,
                LevelTransition.Type.REGULAR_EXIT));

        return true;
    }

    @Override
    public Mob createMob() {
        return null;
    }

    @Override
    protected void createMobs() {
        // Monsters are deliberately spawned through the developer tool.
    }

    @Override
    public Actor addRespawner() {
        return null;
    }

    @Override
    protected void createItems() {
        int[] displayCells = {
                4 + 5 * width(), 7 + 5 * width(), 10 + 5 * width(),
                4 + 8 * width(), 7 + 8 * width(), 10 + 8 * width(),
                4 + 15 * width(), 7 + 15 * width(), 10 + 15 * width()
        };

        int index = 0;
        for (Item item : itemsToSpawn) {
            drop(item, displayCells[index++ % displayCells.length]).type = Heap.Type.HEAP;
        }

        // Lab-only stock. Normal floors keep their existing two-heart balance.
        drop(new YandereHeart().quantity(6), displayCells[index % displayCells.length]).type = Heap.Type.HEAP;
    }

    @Override
    public int randomRespawnCell(Char ch) {
        // Unlike the normal implementation, visible cells are allowed. This is essential in a
        // bright sandbox and lets hostile yandere placement still select a distant valid cell.
        for (int tries = 0; tries < 200; tries++) {
            int x = Random.IntRange(2, width() - 3);
            int y = Random.IntRange(2, height() - 3);
            int cell = x + y * width();
            if (passable[cell]
                    && map[cell] != Terrain.ENTRANCE
                    && map[cell] != Terrain.EXIT
                    && Actor.findChar(cell) == null) {
                return cell;
            }
        }
        return -1;
    }
}
