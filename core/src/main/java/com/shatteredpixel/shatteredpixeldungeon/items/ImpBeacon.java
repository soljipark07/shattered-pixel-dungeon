package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.ImpTravel;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.HashSet;

/** Reusable route to the actual saved Imp floor and back to the saved first floor. */
public class ImpBeacon extends Item {
    public static final String AC_IMP = "IMP";
    public static final String AC_FIRST = "FIRST";
    private int impDepth = -1;
    private int impCell = -1;
    private boolean strengthRelocated = true;
    private final HashSet<Integer> visitedFloors = new HashSet<>();
    private final HashSet<Integer> remainsFloors = new HashSet<>();

    {
        image = ItemSpriteSheet.ARTIFACT_BEACON;
        unique = true;
        levelKnown = cursedKnown = true;
        defaultAction = AC_IMP;
    }

    public void setDestination(int depth, int cell) {
        impDepth = depth;
        impCell = cell;
    }

    public boolean visitFloor(int depth) { return visitedFloors.add(depth); }

    public int destinationCell() { return impCell; }

    public boolean relocatesStrength() { return strengthRelocated; }

    public boolean isPreparedFloor(int depth) { return depth >= 1 && depth <= impDepth; }

    public void recordRemainsFloor(int depth) { remainsFloors.add(depth); }

    public boolean remainsOnFloor(int depth) { return remainsFloors.contains(depth); }

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = new ArrayList<>();
        actions.add(AC_IMP);
        actions.add(AC_FIRST);
        return actions;
    }

    @Override
    public void execute(Hero hero, String action) {
        super.execute(hero, action);
        if (!AC_IMP.equals(action) && !AC_FIRST.equals(action)) return;
        if (Dungeon.branch != 0 || !Dungeon.interfloorTeleportAllowed()) {
            GLog.w(Messages.get(this, "blocked"));
            return;
        }
        if (impDepth < 17 || impDepth > 19 || impCell < 0
                || !Dungeon.levelHasBeenGenerated(impDepth, 0)) {
            GLog.w(Messages.get(this, "unready"));
            return;
        }
        Level.beforeTransition();
        InterlevelScene.mode = InterlevelScene.Mode.RETURN;
        InterlevelScene.returnDepth = AC_IMP.equals(action) ? impDepth : 1;
        InterlevelScene.returnBranch = 0;
        InterlevelScene.returnPos = AC_IMP.equals(action) ? ImpTravel.IMP_ARRIVAL : ImpTravel.FIRST_ARRIVAL;
        Game.switchScene(InterlevelScene.class);
    }

    @Override
    public boolean isUpgradable() { return false; }

    @Override
    public boolean isIdentified() { return true; }

    @Override
    public boolean keptThroughLostInventory() { return true; }

    @Override
    public int value() { return 0; }

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put("imp_depth", impDepth);
        bundle.put("imp_cell", impCell);
        bundle.put("strength_relocated", strengthRelocated);
        int[] visited = new int[visitedFloors.size()];
        int index = 0;
        for (int depth : visitedFloors) visited[index++] = depth;
        bundle.put("visited_floors", visited);
        int[] remains = new int[remainsFloors.size()];
        index = 0;
        for (int depth : remainsFloors) remains[index++] = depth;
        bundle.put("remains_floors", remains);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        impDepth = bundle.contains("imp_depth") ? bundle.getInt("imp_depth") : -1;
        impCell = bundle.contains("imp_cell") ? bundle.getInt("imp_cell") : -1;
        // Previous APKs did not grant strength potions; keep their remaining natural drops.
        strengthRelocated = bundle.getBoolean("strength_relocated");
        visitedFloors.clear();
        if (bundle.contains("visited_floors")) {
            for (int depth : bundle.getIntArray("visited_floors")) visitedFloors.add(depth);
        }
        remainsFloors.clear();
        if (bundle.contains("remains_floors")) {
            for (int depth : bundle.getIntArray("remains_floors")) remainsFloors.add(depth);
        }
    }
}
