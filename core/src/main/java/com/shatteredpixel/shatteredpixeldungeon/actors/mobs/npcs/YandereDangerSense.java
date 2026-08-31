package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Alchemy;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Foliage;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.GooWarn;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Regrowth;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SacrificialFire;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.StormCloud;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.WellWater;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DelayedRockFall;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Eye;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.RipperDemon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Tengu;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogDzewa;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.PitfallTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.watabou.utils.PathFinder;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;

/**
 * 30하트 이후 성장형 얀데레가 사용하는 중앙 위험 판정기.
 *
 * 숨겨진/아직 발동하지 않은 함정을 미리 아는 기능은 아니다. 현재 눈에 보이는
 * 작동 함정, 이미 생성된 위험 효과, 화면에 예고된 공격만 위험으로 취급한다.
 * 이동, 긴급 회피, 전투 순간이동이 모두 이 한 판정을 공유한다.
 */
public final class YandereDangerSense {

    private YandereDangerSense() {}

    private static Field yogTargetedCellsField;
    private static Field gooPumpedUpField;
    private static Field rockPositionsField;
    private static boolean reflectionPrepared = false;

    public static boolean dangerAt(GrowthYandereAlly ally, int cell) {
        if (ally == null || Dungeon.level == null
                || cell < 0 || cell >= Dungeon.level.length()
                || !Dungeon.level.insideMap(cell)) {
            return true;
        }

        // 이미 열린 구덩이.
        if (Dungeon.level.pit[cell]) return true;

        // 숨겨진 함정은 여기서 보지 않는다. 발견되어 지도에 드러난 활성 함정만 회피한다.
        Trap trap = Dungeon.level.traps.get(cell);
        if (trap != null && trap.visible && Dungeon.level.map[cell] == Terrain.TRAP) return true;

        // 이미 작동한 낙하 함정의 붕괴 예고 구역.
        if (delayedPitThreatens(cell)) return true;

        // 이미 생성되어 현재 작동 중인 환경 효과. 해로운 효과를 하나씩 나열하는 대신,
        // 명백히 무해한 blob만 제외하고 나머지 활성 효과는 보수적으로 위험 취급한다.
        if (activeBlobThreatens(ally, cell)) return true;

        // 몬스터가 화면에 이미 예고한 공격들.
        return telegraphedAttackThreatens(ally, cell);
    }

    private static boolean harmlessBlob(Blob blob) {
        return blob instanceof Alchemy
                || blob instanceof Foliage
                || blob instanceof Regrowth
                || blob instanceof GooWarn
                || blob instanceof SacrificialFire
                || blob instanceof StormCloud
                || blob instanceof WellWater;
    }

    private static boolean activeBlobThreatens(GrowthYandereAlly ally, int cell) {
        for (Blob blob : Dungeon.level.blobs.values()) {
            if (blob == null || blob.volume <= 0 || blob.cur == null
                    || cell >= blob.cur.length || blob.cur[cell] <= 0) {
                continue;
            }
            if (harmlessBlob(blob)) continue;
            if (ally.isImmune(blob.getClass())) continue;

            // 전기, 눈보라, 불/지옥불, 냉기, 독/부식/마비/혼란 가스,
            // 거미줄, 텐구/보스 전용 위험 blob 등을 모두 여기서 한 번에 잡는다.
            return true;
        }
        return false;
    }

    private static boolean delayedPitThreatens(int cell) {
        if (Dungeon.hero == null) return false;
        for (PitfallTrap.DelayedPit pit : Dungeon.hero.buffs(PitfallTrap.DelayedPit.class)) {
            if (pit == null || pit.positions == null
                    || pit.depth != Dungeon.depth || pit.branch != Dungeon.branch
                    || pit.ignoreAllies) {
                continue;
            }
            for (int threatened : pit.positions) {
                if (threatened == cell) return true;
            }
        }
        return false;
    }

    private static boolean telegraphedAttackThreatens(GrowthYandereAlly ally, int cell) {
        prepareReflection();

        for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
            if (mob == null || !mob.isAlive() || mob.alignment != GrowthYandereAlly.Alignment.ENEMY) continue;

            if (mob instanceof Eye && ((Eye)mob).chargedBeamThreatens(cell)) return true;
            if (mob instanceof RipperDemon && ((RipperDemon)mob).leapThreatens(cell)) return true;

            if (mob instanceof Tengu && tenguBombThreatens((Tengu)mob, cell)) return true;
            if (mob instanceof Goo && gooPumpThreatens((Goo)mob, ally, cell)) return true;
            if (mob instanceof YogDzewa && yogRayThreatens((YogDzewa)mob, cell)) return true;

            // DM-300뿐 아니라 같은 DelayedRockFall 기반 예고를 쓰는 몬스터를 전부 잡는다.
            for (DelayedRockFall fall : mob.buffs(DelayedRockFall.class)) {
                if (delayedRockThreatens(fall, cell)) return true;
            }
        }

        return false;
    }

    private static boolean tenguBombThreatens(Tengu tengu, int cell) {
        for (Tengu.BombAbility bomb : tengu.buffs(Tengu.BombAbility.class)) {
            if (bomb != null && bomb.bombPos >= 0 && openRadiusThreatens(bomb.bombPos, cell, 2)) {
                return true;
            }
        }
        return false;
    }

    // 텐구 폭탄의 실제 판정과 같이 단단한 벽을 통과하지 않는 2칸 범위만 계산한다.
    private static boolean openRadiusThreatens(int source, int target, int radius) {
        if (source == target) return true;
        boolean[] seen = new boolean[Dungeon.level.length()];
        int[] distance = new int[Dungeon.level.length()];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        seen[source] = true;
        queue.add(source);

        while (!queue.isEmpty()) {
            int cur = queue.removeFirst();
            if (distance[cur] >= radius) continue;

            for (int off : PathFinder.NEIGHBOURS8) {
                int next = cur + off;
                if (next < 0 || next >= seen.length || seen[next] || !Dungeon.level.insideMap(next)) continue;
                if (Dungeon.level.solid[next]) continue;
                seen[next] = true;
                distance[next] = distance[cur] + 1;
                if (next == target) return true;
                queue.addLast(next);
            }
        }
        return false;
    }

    private static boolean gooPumpThreatens(Goo goo, GrowthYandereAlly ally, int cell) {
        // 구의 충전타는 현재 표적 한 명을 치므로, 얀데레 자신이 표적일 때만 회피한다.
        if (!goo.isTargeting(ally) || readInt(gooPumpedUpField, goo, 0) <= 0) return false;
        if (Dungeon.level.distance(goo.pos, cell) > 2) return false;

        int flags = Ballistica.STOP_TARGET | Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID;
        return new Ballistica(goo.pos, cell, flags).collisionPos == cell
                && new Ballistica(cell, goo.pos, flags).collisionPos == goo.pos;
    }

    @SuppressWarnings("unchecked")
    private static boolean yogRayThreatens(YogDzewa yog, int cell) {
        if (yogTargetedCellsField == null) return false;
        try {
            ArrayList<Integer> targets = (ArrayList<Integer>)yogTargetedCellsField.get(yog);
            if (targets == null || targets.isEmpty()) return false;
            for (int target : targets) {
                Ballistica beam = new Ballistica(yog.pos, target, Ballistica.WONT_STOP);
                for (int pathCell : beam.path) {
                    if (pathCell == cell) return true;
                }
            }
        } catch (IllegalAccessException ignored) {
            // 고정된 3.3.8 필드 접근이 실패하면 해당 예고만 건너뛰고 게임은 계속 진행한다.
        }
        return false;
    }

    private static boolean delayedRockThreatens(DelayedRockFall fall, int cell) {
        if (rockPositionsField == null) return false;
        try {
            int[] positions = (int[])rockPositionsField.get(fall);
            if (positions != null) {
                for (int threatened : positions) {
                    if (threatened == cell) return true;
                }
            }
        } catch (IllegalAccessException ignored) {
        }
        return false;
    }

    private static int readInt(Field field, Object target, int fallback) {
        if (field == null) return fallback;
        try {
            return field.getInt(target);
        } catch (IllegalAccessException ignored) {
            return fallback;
        }
    }

    private static void prepareReflection() {
        if (reflectionPrepared) return;
        reflectionPrepared = true;
        try {
            yogTargetedCellsField = YogDzewa.class.getDeclaredField("targetedCells");
            yogTargetedCellsField.setAccessible(true);
        } catch (Exception ignored) {
            yogTargetedCellsField = null;
        }
        try {
            gooPumpedUpField = Goo.class.getDeclaredField("pumpedUp");
            gooPumpedUpField.setAccessible(true);
        } catch (Exception ignored) {
            gooPumpedUpField = null;
        }
        try {
            rockPositionsField = DelayedRockFall.class.getDeclaredField("rockPositions");
            rockPositionsField.setAccessible(true);
        } catch (Exception ignored) {
            rockPositionsField = null;
        }
    }
}
