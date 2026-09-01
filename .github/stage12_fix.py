from pathlib import Path

p = Path('core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/Hero.java')
t = p.read_text()

duplicate = '''\t@Override
\tpublic void move(int step, boolean travelling) {
\t\tint oldPos = pos;
\t\tsuper.move(step, travelling);
\t\tif (travelling && Dungeon.level != null && oldPos != pos && Dungeon.level.adjacent(oldPos, pos)) {
\t\t\tYandereBloodbath.onHeroStep();
\t\t}
\t}

'''
if duplicate in t:
    t = t.replace(duplicate, '', 1)

old = '''\t@Override
\tpublic void move(int step, boolean travelling) {
\t\tboolean wasHighGrass = Dungeon.level.map[step] == Terrain.HIGH_GRASS;

\t\tsuper.move( step, travelling);
\t\t
\t\tif (!flying && travelling) {'''
new = '''\t@Override
\tpublic void move(int step, boolean travelling) {
\t\tboolean wasHighGrass = Dungeon.level.map[step] == Terrain.HIGH_GRASS;
\t\tint oldPos = pos;

\t\tsuper.move( step, travelling);
\t\tif (travelling && Dungeon.level != null && oldPos != pos && Dungeon.level.adjacent(oldPos, pos)) {
\t\t\tYandereBloodbath.onHeroStep();
\t\t}
\t\t
\t\tif (!flying && travelling) {'''
if new not in t:
    if old not in t:
        raise SystemExit('existing Hero.move anchor not found')
    t = t.replace(old, new, 1)

if t.count('public void move(int step, boolean travelling)') != 1:
    raise SystemExit('Hero.move override count is not exactly one')

p.write_text(t)
