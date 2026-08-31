package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.YandereAlly;
import com.watabou.noosa.MovieClip;
import com.watabou.noosa.TextureFilm;

public class YandereSprite extends MobSprite {

    private static final int FRAME_WIDTH = 12;
    private static final int FRAME_HEIGHT = 15;
    private static final int FRAMES_PER_STATE = 21;

    private static final int NORMAL = 0;
    private static final int RED_I = 1;
    private static final int RED_II = 2;
    private static final int HOSTILE = 3;

    private final Animation[] idleByState = new Animation[4];
    private final Animation[] runByState = new Animation[4];
    private final Animation[] dieByState = new Animation[4];
    private final Animation[] attackByState = new Animation[4];
    private final Animation[] operateByState = new Animation[4];
    private final Animation[] heartByState = new Animation[4];

    private final MovieClip hostileAura;
    private int visualState = -1;

    public YandereSprite() {
        super();

        texture(Assets.Sprites.YANDERE);
        TextureFilm body = new TextureFilm(texture, FRAME_WIDTH, FRAME_HEIGHT);

        for (int state = NORMAL; state <= HOSTILE; state++) {
            int first = state * FRAMES_PER_STATE;

            idleByState[state] = new Animation(2, true);
            idleByState[state].frames(body, first, first + 1);

            runByState[state] = new Animation(20, true);
            runByState[state].frames(body,
                    first + 2, first + 3, first + 4,
                    first + 5, first + 6, first + 7);

            dieByState[state] = new Animation(10, false);
            dieByState[state].frames(body,
                    first + 8, first + 9, first + 10,
                    first + 11, first + 12);

            attackByState[state] = new Animation(15, false);
            attackByState[state].frames(body,
                    first + 13, first + 14, first + 15, first);

            operateByState[state] = new Animation(8, false);
            operateByState[state].frames(body,
                    first + 16, first + 17, first + 16, first + 17);

            heartByState[state] = new Animation(6, false);
            heartByState[state].frames(body,
                    first + 18, first + 18,
                    first + 19, first + 19,
                    first + 20, first + 20, first + 20, first + 20,
                    first + 19, first + 19,
                    first + 18);
        }

        hostileAura = new MovieClip(Assets.Sprites.YANDERE_AURA);
        TextureFilm auraFrames = new TextureFilm(hostileAura.texture, FRAME_WIDTH, FRAME_HEIGHT);
        MovieClip.Animation auraAnimation = new MovieClip.Animation(2, true);
        auraAnimation.frames(auraFrames, 0, 1);
        hostileAura.play(auraAnimation);

        switchVisualState(NORMAL);
    }

    public void playHeartReaction() {
        // Receiving a heart resets rage (and ends hostility) before this is
        // called, so the approved NORMAL reaction is always appropriate.
        switchVisualState(NORMAL);
        play(heartByState[NORMAL], true);
    }

    private int desiredVisualState() {
        if (!(ch instanceof YandereAlly)) return NORMAL;

        YandereAlly ally = (YandereAlly) ch;
        if (ally.hostileToHero()) return HOSTILE;

        float heroHp = Dungeon.hero == null || Dungeon.hero.HT <= 0
                ? 1f : (float) Dungeon.hero.HP / Dungeon.hero.HT;
        int rage = ally.rage();

        if (rage >= 85 || heroHp <= 0.25f) return RED_II;
        if (rage >= 70 || heroHp <= 0.50f) return RED_I;
        return NORMAL;
    }

    private void switchVisualState(int next) {
        if (next == visualState) return;

        // On first setup curAnim and every inherited animation field are null.
        // Comparing null == null made wasDie true, so a newly summoned ally
        // immediately played her death animation and then disappeared.
        boolean wasRun = curAnim != null && curAnim == run;
        boolean wasAttack = curAnim != null && (curAnim == attack || curAnim == zap);
        boolean wasOperate = curAnim != null && curAnim == operate;
        boolean wasDie = curAnim != null && curAnim == die;

        visualState = next;
        idle = idleByState[next];
        run = runByState[next];
        die = dieByState[next];
        attack = attackByState[next];
        zap = attack.clone();
        operate = operateByState[next];

        if (wasDie) {
            play(die, true);
        } else if (wasAttack) {
            play(attack, true);
        } else if (wasOperate) {
            play(operate, true);
        } else if (wasRun) {
            play(run, true);
        } else {
            play(idle, true);
        }
    }

    @Override
    public void update() {
        switchVisualState(desiredVisualState());
        hostileAura.update();
        super.update();
    }

    @Override
    public void onComplete(Animation anim) {
        super.onComplete(anim);
        if (anim == heartByState[NORMAL]) {
            idle();
        }
    }

    @Override
    public void draw() {
        if (visualState == HOSTILE && visible) {
            hostileAura.x = x;
            hostileAura.y = y;
            hostileAura.scale.set(scale);
            hostileAura.origin.set(origin);
            hostileAura.angle = angle;
            hostileAura.flipHorizontal = flipHorizontal;
            hostileAura.camera = camera();

            hostileAura.rm = rm;
            hostileAura.gm = gm;
            hostileAura.bm = bm;
            hostileAura.am = am;
            hostileAura.ra = ra;
            hostileAura.ga = ga;
            hostileAura.ba = ba;
            hostileAura.aa = aa;
            hostileAura.draw();
        }

        super.draw();
    }

    @Override
    public void destroy() {
        hostileAura.destroy();
        super.destroy();
    }
}
