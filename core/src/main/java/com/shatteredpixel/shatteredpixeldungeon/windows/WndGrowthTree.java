package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.GrowthYandereAlly;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.ui.Component;

public class WndGrowthTree extends Window {

    private static final int WIDTH_P = 136;
    private static final int HEIGHT_P = 180;
    private static final int WIDTH_L = 220;
    private static final int HEIGHT_L = 132;

    private static final int[] HEARTS = {
            3, 6, 9, 12, 15, 18, 21, 24,
            27, 30, 33, 36, 39, 42, 45, 48
    };

    private static final String[] NAMES = {
            "경계 범위 I", "자연회복 I", "하트 감지", "경계 범위 II",
            "위기 각성 I", "피해 가로채기 I", "자연회복 II", "비밀 감지 I",
            "긴급 호출", "위험 회피", "위기 각성 II", "비밀 감지 II",
            "피해 가로채기 II", "전투 순간이동", "비밀 감지 III", "최종 각성"
    };

    private static final String[] DESCS = {
            "수비 태세의 경계 범위가 2칸에서 3칸으로 넓어진다.",
            "10턴마다 최대 HP의 2%를 자연회복한다. 기본 상태는 1%다.",
            "층을 떠나려 할 때 바닥에 하트가 남아 있으면 얀데레가 경고한다.",
            "수비 태세의 경계 범위가 5칸으로 넓어진다. 집착 단계가 ‘소유욕’으로 올라간다.",
            "주인공 HP가 30% 이하일 때 얀데레의 공격력과 피해 경감이 1.2배가 된다.",
            "주인공 근처에 위협이 있을 때 턴마다 30% 확률로 피해 연결 1개를 준비해, 다음 피해를 둘이 절반씩 나눈다.",
            "자연회복이 10턴마다 최대 HP의 3%로 강화된다.",
            "2칸 안의 비밀문을 감지하면 경고한다. 집착 단계가 ‘과의존’으로 올라간다.",
            "리본의 도움 요청을 쓰면 안전한 자리가 있을 때 즉시 주인공 옆으로 순간이동한다.",
            "충전 빔, 리퍼 도약 등 예고된 위험 칸을 피하고 안전한 경로를 우선한다.",
            "주인공 HP가 25% 이하이면 전투 배율 1.5배, 10% 이하이면 1.8배까지 강화된다.",
            "가만히 살피면 근처 2칸의 숨은 함정을 찾아낸다. 집착 단계가 ‘광기’로 올라간다.",
            "피해 가로채기가 3칸 범위·50% 확률로 강화된다. 연결 3개로 다음 피해의 약 75%를 대신 받고 이동속도도 1.2배가 된다.",
            "전투 중 2~4칸 떨어진 적에게 안전한 인접 칸이 있으면 순간이동해 붙는다.",
            "숨은 함정 탐지 범위가 3칸으로 늘고, 비밀방에 들어왔을 때도 알려준다.",
            "완전 각성. 최대 HP·공격력·피해 경감 1.5배, 명중·방어 1.3배. 광란도는 0으로 고정되고 주인공을 광란 추격하지 않는다. 학살 이벤트도 해금된다."
    };

    private final int hearts;
    private final float[] nodeTop = new float[HEARTS.length];
    private final float[] nodeBottom = new float[HEARTS.length];

    public WndGrowthTree(int hearts) {
        this.hearts = Math.max(0, Math.min(GrowthYandereAlly.MAX_GROWTH_HEARTS, hearts));

        final int width = PixelScene.landscape() ? WIDTH_L : WIDTH_P;
        final int height = PixelScene.landscape() ? HEIGHT_L : HEIGHT_P;

        RenderedTextBlock title = PixelScene.renderTextBlock("얀데레 성장 트리", 9);
        title.hardlight(TITLE_COLOR);
        title.maxWidth(width);
        title.setPos(0, 0);
        add(title);

        RenderedTextBlock progress = PixelScene.renderTextBlock(
                "성장 하트 " + this.hearts + "/48  ·  밝은 노드=해금  ·  어두운 노드=미해금", 6);
        progress.maxWidth(width);
        progress.setPos(0, title.bottom() + 2);
        add(progress);

        Component content = new Component();
        float y = 2;

        for (int i = 0; i < HEARTS.length; i++) {
            final boolean unlocked = this.hearts >= HEARTS[i];
            final boolean next = !unlocked && (i == 0 || this.hearts >= HEARTS[i - 1]);
            final int color = unlocked ? TITLE_COLOR : (next ? 0xBBBB88 : 0x666666);

            ColorBlock spine = new ColorBlock(1, 22, unlocked ? 0xFFCC44 : 0x444444);
            spine.x = 3;
            spine.y = y + 2;
            content.add(spine);

            ColorBlock node = new ColorBlock(5, 5, unlocked ? 0xFFFF66 : (next ? 0x999977 : 0x555555));
            node.x = 1;
            node.y = y + 1;
            content.add(node);

            RenderedTextBlock text = PixelScene.renderTextBlock(
                    (unlocked ? "해금  " : "잠김  ") + "♥" + HEARTS[i] + "  " + NAMES[i], 6);
            text.hardlight(color);
            text.maxWidth(width - 11);
            text.setPos(10, y);
            content.add(text);

            nodeTop[i] = y - 2;
            nodeBottom[i] = Math.max(y + 9, text.bottom() + 2);
            y = nodeBottom[i] + 5;
        }

        RenderedTextBlock hint = PixelScene.renderTextBlock("노드를 누르면 자세한 효과를 볼 수 있다.", 6);
        hint.hardlight(0x999999);
        hint.maxWidth(width - 8);
        hint.setPos(8, y + 2);
        content.add(hint);
        y = hint.bottom() + 3;
        content.setSize(width, y);

        ScrollPane pane = new ScrollPane(content) {
            @Override
            public void onClick(float x, float y) {
                for (int i = 0; i < HEARTS.length; i++) {
                    if (y >= nodeTop[i] && y <= nodeBottom[i]) {
                        showNode(i);
                        return;
                    }
                }
            }
        };
        float paneY = progress.bottom() + 3;
        pane.setRect(0, paneY, width, height - paneY);
        add(pane);

        resize(width, height);
    }

    private void showNode(int index) {
        boolean unlocked = hearts >= HEARTS[index];
        String status = unlocked ? "해금됨" : "미해금";
        String extra = "";
        if (!unlocked) extra = "\n\n앞으로 하트 " + (HEARTS[index] - hearts) + "개 더 필요.";
        GameScene.show(new WndOptions(
                "♥" + HEARTS[index] + " · " + NAMES[index],
                status + "\n\n" + DESCS[index] + extra,
                "닫기"
        ));
    }
}
