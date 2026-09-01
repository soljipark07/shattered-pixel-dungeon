from pathlib import Path

p = Path('core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/Hero.java')
t = p.read_text()

old_import = 'import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.YandereAlly;\nimport com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.YandereJealousy;'
new_import = 'import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.YandereAlly;\nimport com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.YandereBloodbath;\nimport com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.YandereJealousy;'
if new_import not in t:
    if old_import not in t:
        raise SystemExit('yandere import anchor not found')
    t = t.replace(old_import, new_import, 1)

anchor = '''\t@Override
\tpublic float speed() {
'''
hook = '''\t@Override
\tpublic void move(int step, boolean travelling) {
\t\tint oldPos = pos;
\t\tsuper.move(step, travelling);
\t\tif (travelling && Dungeon.level != null && oldPos != pos && Dungeon.level.adjacent(oldPos, pos)) {
\t\t\tYandereBloodbath.onHeroStep();
\t\t}
\t}
\n'''
if 'YandereBloodbath.onHeroStep();' not in t:
    if anchor not in t:
        raise SystemExit('hero speed anchor not found')
    t = t.replace(anchor, hook + anchor, 1)

p.write_text(t)
