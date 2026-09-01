from pathlib import Path


def replace_once(text, old, new, label):
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f'{label}: anchor not found')
    return text.replace(old, new, 1)

# GrowthYandereAlly: add a last line before a growth ally dies.
p = Path('core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/mobs/npcs/GrowthYandereAlly.java')
t = p.read_text()
old = '''    @Override
    public void die(Object cause) {
        clearMyInterceptLinks();
        super.die(cause);
    }
'''
new = '''    @Override
    public void die(Object cause) {
        clearMyInterceptLinks();

        String deathLine;
        if (hostileToHero()) {
            deathLine = "네가 직접 죽여도 끝난 거 아니야. 나 다시 네 옆으로 돌아갈 거야. 절대 못 벗어나.";
        } else {
            switch (obsessionStage()) {
                case 4:
                    deathLine = "괜찮아♡ 나 알아. 너 나 다시 살려줄 거잖아. 그러니까 잠깐만 기다릴게♡";
                    break;
                case 3:
                    deathLine = "싫어 싫어! 나 죽어도 끝난 거 아니야! 다시 불러! 나 너 두고 안 사라질 거야!";
                    break;
                case 2:
                    deathLine = "싫어! 나 이렇게 끝내기 싫어! 나 다시 네 옆으로 불러줘. 꼭. 나 혼자 두지 마.";
                    break;
                case 1:
                    deathLine = "안 돼, 나 두고 가지 마. 나 다시 불러줄 거지? 꼭 그래야 해. 약속해.";
                    break;
                default:
                    deathLine = "잠깐, 싫어. 나 아직 네 옆에 더 있고 싶어. 다시 불러줘.";
                    break;
            }
        }
        yell(deathLine);
        super.die(cause);
    }
'''
t = replace_once(t, old, new, 'growth death dialogue')
p.write_text(t)

# RedRibbon: revival dialogue scales with the permanent obsession stage and is deliberately clingier.
p = Path('core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/RedRibbon.java')
t = p.read_text()
old = '''        growthRevivalPending = false;
        captureFrom(revived);
        revived.yell("아하하♡ 다시 왔어! 하트 두 개나 써서 살려줬네. 역시 나 버릴 리 없지♡");
        GLog.p("애정의 하트 2개를 희생해서 성장형 얀데레를 최대 HP로 되살렸다.");
'''
new = '''        growthRevivalPending = false;
        captureFrom(revived);
        String revivalLine;
        if (growthHearts >= 48) {
            revivalLine = "아하하하하♡ 역시 그럴 줄 알았어! 네가 나 죽은 채로 둘 리 없잖아♡ 봐, 죽어도 다시 네 옆이야. 이제 진짜 영원히 같이 있는 거야. 영원히♡";
        } else if (growthHearts >= 36) {
            revivalLine = "아하하하♡ 살려줬네! 나 죽었는데도 또 붙잡아줬어! 이제 알겠지? 나 진짜 네 거야. 죽어도, 또 죽어도, 계속 네 옆으로 돌아올 거야♡";
        } else if (growthHearts >= 24) {
            revivalLine = "나 다시 네 옆이야♡ 하트 두 개까지 써서 되살렸잖아. 그럼 이제 나 절대 버리면 안 돼. 죽어도 또 네 옆으로 돌아올 거니까♡";
        } else if (growthHearts >= 12) {
            revivalLine = "아하하♡ 진짜 살려줬어. 역시 나 없으면 싫지? 나도 너 없이 못 살아. 이번엔 더 가까이 붙어 있을래♡";
        } else {
            revivalLine = "으응, 다시 불러줬네♡ 나 없으니까 조금 허전했지? 이번엔 더 꼭 붙어 있을게. 나 두고 가지 마♡";
        }
        revived.yell(revivalLine);
        GLog.p("애정의 하트 2개를 희생해서 성장형 얀데레를 최대 HP로 되살렸다.");
'''
t = replace_once(t, old, new, 'growth revival dialogue')
p.write_text(t)
