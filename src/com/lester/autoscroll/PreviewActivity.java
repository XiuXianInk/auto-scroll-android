package com.lester.autoscroll;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 预览页：内置一段样例文字，模拟小说阅读界面，让用户在真机上直接看滚动效果。
 * 不依赖番茄小说，也不用装别的阅读器。
 */
public class PreviewActivity extends Activity {

    private static final int PAPER = 0xFFF6F2E9;
    private static final int INK = 0xFF2B2B2B;
    private static final int INK_LIGHT = 0xFF6B675F;
    private static final int LINE = 0x14000000;

    private static final String CHAPTER = "第一章　雪线";

    /** 原创样例文字，只为演示滚动，不涉及任何版权内容 */
    private static final String[] PARAS = {
            "山里的第一场雪来得比往年早。",
            "陈默推开木屋的门，看见院子里那棵老松的枝丫已经压弯了一半，雪厚得像棉被。他站在门口看了很久，"
                    + "直到呼出的白气在眉毛上结了薄霜，才转身回屋拿了扫帚。",
            "这里海拔两千七百米，离最近的镇子四十里山路。四年前他申请调来做护林员的时候，老场长盯着他看了半天，"
                    + "说，年轻人，那边没电没网，冬天封山五个月，你想清楚。他说想清楚了。老场长又问了一遍。"
                    + "他还是说想清楚了。",
            "于是他就来了。",
            "扫雪是一件很奇怪的事。你把门前的雪扫开，露出一条窄窄的路，心里会生出一种错觉，好像这条路真能通向什么地方。"
                    + "可顺着它往前看，走二十米就是林子，林子后面是白茫茫的山脊，再往后什么都没有。",
            "他扫到一半停下来，抬头看天。云层压得很低，是那种化不开的铁灰色。今天不会再下了，但明天说不定又来一场。"
                    + "山里的天气从来不跟人商量。",
            "吃过早饭，他背上水壶和责任区的记录本出了门。按规定，每天要走一遍林线，看看有没有倒木、有没有偷猎的套子、"
                    + "有没有火情。其实冬天没什么可看的，雪把一切都盖住了，连声音都盖住了。",
            "走到第三个界桩的时候，他停住了。",
            "雪地上有一行脚印。",
            "不是野物的。野物的脚印他一眼就能认出来——狍子是两瓣蹄，踩下去前窄后宽；野猪更深，边缘毛糙；"
                    + "狐狸走的是直线，一步一步隔得很匀。这一行不是。这一行有鞋底的纹路，有脚跟的重心，有深有浅，"
                    + "深的地方说明踩的人在那儿犹豫过。",
            "他蹲下来看了很久。脚印很新，边缘还没被风磨圆，最多两个小时。方向是从界碑外面进来的，绕过一道山脊，"
                    + "然后径直往林子里去了。",
            "这道界碑外面是另一片林场，早就撤了，三年没人管。",
            "陈默直起腰，把手套往上拽了拽。他没有立刻跟上去，而是先在心里过了一遍：封山前最后一个下山的是老周，"
                    + "走了四十天；上周他下过一次山补给，来回三天，路上没碰见人；那么这行脚印是谁的。",
            "他想不出答案。于是他决定跟上去。",
            "林子里很静。雪把松针压得低低的，人从底下一钻，雪就簌簌地落进衣领，凉得钻心。他走得慢，"
                    + "尽量踩在脚印旁边而不是上面，这样万一对方回头，也看不出有人跟着——虽然这更像是他心里给自己找的理由。",
            "脚印在一处背风的石崖下断了。",
            "石崖前有一小片空地，雪被踩得很实，中间有一堆东西，被雪盖了一半。他走过去，蹲下，用手把雪拨开。",
            "是一个帆布包，很旧，边角磨得起了毛。包口用绳子系着，系得很仔细，打了两个结。他解开第一个结的时候，"
                    + "手指有点抖，不是因为冷。",
            "包里是一件叠好的棉衣，一个搪瓷缸子，半包压缩饼干，还有一本硬壳的笔记本。",
            "笔记本被水浸过，页面皱得像干枯的叶子，但字还能认。他翻开第一页。",
            "上面只有一行字，写得很用力，笔尖把纸戳破了一个小洞：",
            "「如果有人看到这个本子，请把它交给山下镇上的李秀兰。」",
            "陈默盯着那行字看了很久。风从石崖上卷下来，把地上的雪沫吹起来，打在他脸上。他忽然意识到，"
                    + "这行字的墨迹还没有完全干透。",
            "他站起来，环顾四周。雪地上除了自己来时的脚印，再没有别的痕迹。可就在刚才，这里明明还有另一个人。",
            "远处传来一声闷响，像是积雪从高处崩塌。他循声望去，只看见一片白，白得晃眼。",
            "他把笔记本塞进怀里，贴着胸口。棉衣也收进包里，重新系好，然后背在肩上。",
            "下山的路他走了两个钟头。回到木屋的时候天已经擦黑，屋里冷得像冰窖。他生了火，把笔记本摊在桌上，"
                    + "一页一页地翻。",
            "大部分页面被水泡得看不清了，只有零星几个字能辨认：一些数字，一些地名，还有反复出现的一个称呼，"
                    + "像是「娘」。",
            "翻到最后几页，字迹忽然清楚起来，因为那几页被什么东西压着，没怎么进水。",
            "「娘，今年我还是没能回去。等雪化了我就下山，一定回去。」",
            "「今年雪下得早，我怕是要困在这儿了。」",
            "「你要好好的。」",
            "陈默合上本子，火苗在炉膛里噼啪响了一声。他想起自己四年前上山时，也说过类似的话。",
            "也是「等雪化了就回去」。",
            "他起身走到窗边，外面黑透了，什么也看不见。雪又开始下了，很小，很密，落在玻璃上立刻化成一小点水。",
            "第二天一早，他重新背上那个帆布包，往山下走。",
            "四十几里山路，他走了整整一天。到镇上的时候天已经黑透，街上的灯一盏一盏亮起来，照得雪地发黄。"
                    + "他问了三个人，才找到李秀兰家。",
            "开门的是一个头发花白的老太太，个子很矮，穿着一件洗得发白的蓝布棉袄。她看了看他，又看了看他肩上的包，"
                    + "什么也没问，只是往后退了一步，把门让开。",
            "屋里很暖和。桌上摆着两副碗筷，其中一副没人用过。",
            "陈默把包放在桌上，解开那两个结，把笔记本拿出来，双手递过去。",
            "老太太接过去，没有立刻翻开。她先是用手把封面上的雪水擦干净，动作很慢，很仔细，像在擦一件瓷器。",
            "然后她翻开第一页。",
            "屋子里很安静。灶上的水开了，壶盖被顶得轻轻作响。",
            "过了很久，她抬起头，说了一句话。",
            "「他今年又没回来。」",
            "陈默没有说话。他看见老太太把本子抱在怀里，抱得很紧，就像抱着一个很小的孩子。",
            "窗外雪还在下。镇上的灯一盏一盏地灭下去，最后只剩下街口那一盏，把雪地照出一小片昏黄。",
    };

    private LinearLayout headerSpacer;
    private TextView toggleTv;
    private TextView speedTv;
    private ReadingScrollView scrollView;
    private boolean started;
    /** 用户手动暂停过：切后台回来不该自动重新开始 */
    private boolean userPaused;
    /** 已滚到底：停下来，按钮变成"重读" */
    private boolean bottomReached;

    private final android.os.Handler ui = new android.os.Handler(android.os.Looper.getMainLooper());
    /** 悬浮球也能开关滚动，顶栏按钮必须跟着它走，否则两个控制源显示的状态打架 */
    private final Runnable syncTask = new Runnable() {
        @Override
        public void run() {
            ScrollService s = ScrollService.instance;
            if (s != null && !bottomReached) {
                boolean now = s.isRunning();
                if (now != started) {
                    started = now;
                    updateToggle();
                }
            }
            ui.postDelayed(this, 600);
        }
    };

    /** 能感知"滚到底"的滚动容器，避免到底之后手势还在空转 */
    @SuppressLint("ViewConstructor")
    private class ReadingScrollView extends ScrollView {
        ReadingScrollView(android.content.Context c) {
            super(c);
        }

        @Override
        protected void onScrollChanged(int l, int t, int oldL, int oldT) {
            super.onScrollChanged(l, t, oldL, oldT);
            View child = getChildAt(0);
            if (child == null) {
                return;
            }
            boolean atBottom = ScrollMath.isScrolledToBottom(
                    child.getBottom(), getHeight(), getScrollY(),
                    UiKit.dp(PreviewActivity.this, 4));
            if (atBottom) {
                if (!bottomReached) {
                    onReachBottom();
                }
            } else if (bottomReached) {
                // 用户自己往回滚了，恢复可滚动状态
                bottomReached = false;
                updateToggle();
            }
        }
    }

    private void onReachBottom() {
        bottomReached = true;
        ScrollService s = ScrollService.instance;
        if (s != null) {
            s.stopScroll();
        }
        started = false;
        updateToggle();
        try {
            Toast.makeText(this, "已滚动到底部", Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(PAPER);
        getWindow().setNavigationBarColor(PAPER);
        // 浅色背景要配深色状态栏图标，否则看不见
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ScrollService s = ScrollService.instance;
        if (s == null) {
            Toast.makeText(this, "无障碍服务未连接，可手动滑动体验", Toast.LENGTH_LONG).show();
            return;
        }
        s.setPreviewMode(true);
        // 用户主动暂停过、或者已经滚到底，就不自作主张重新开始
        if (!userPaused && !bottomReached) {
            s.startScroll();
        }
        started = s.isRunning();
        updateToggle();
        // 开始跟踪悬浮球那边的状态变化
        ui.removeCallbacks(syncTask);
        ui.post(syncTask);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ui.removeCallbacks(syncTask);
        ScrollService s = ScrollService.instance;
        if (s != null) {
            s.stopScroll();
            s.setPreviewMode(false);
        }
        started = false;
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(PAPER);

        // 顶栏：返回 + 当前速度 + 暂停
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setBackgroundColor(PAPER);
        header.setPadding(UiKit.dp(this, 16), UiKit.dp(this, 12),
                UiKit.dp(this, 16), UiKit.dp(this, 10));
        root.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView back = UiKit.text(this, "‹ 返回", 13f, INK_LIGHT);
        header.addView(back, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        View spacer = new View(this);
        header.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));

        int secs = Prefs.getScreenSeconds(this);
        speedTv = UiKit.text(this, secs + " 秒/屏 ▾", 11.5f, INK_LIGHT);
        header.addView(speedTv);
        // 点速度就地切换，省得为了试一档速度退出去再进来
        speedTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSpeedPicker();
            }
        });

        toggleTv = UiKit.text(this, "暂停", 11.5f, 0xFFC0392B);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tLp.leftMargin = UiKit.dp(this, 16);
        header.addView(toggleTv, tLp);
        toggleTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ScrollService s = ScrollService.instance;
                if (s == null) {
                    Toast.makeText(PreviewActivity.this, "无障碍服务未连接", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (bottomReached) {
                    // 已经到底了，这一下是"重读"：回到顶部重新开始
                    bottomReached = false;
                    if (scrollView != null) {
                        scrollView.scrollTo(0, 0);
                    }
                    userPaused = false;
                    s.startScroll();
                    started = s.isRunning();
                } else {
                    s.toggle();
                    started = s.isRunning();
                    userPaused = !started;
                }
                updateToggle();
            }
        });

        // 分隔线
        View line = new View(this);
        line.setBackgroundColor(LINE);
        root.addView(line, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1));

        // 正文
        scrollView = new ReadingScrollView(this);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setFillViewport(true);
        root.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(UiKit.dp(this, 22), UiKit.dp(this, 24),
                UiKit.dp(this, 22), UiKit.dp(this, 80));
        scrollView.addView(page, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView chapter = UiKit.text(this, CHAPTER, 17f, INK, true);
        chapter.setGravity(Gravity.CENTER);
        page.addView(chapter, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView body = new TextView(this);
        body.setTextSize(16.5f);
        body.setTextColor(INK);
        body.setLineSpacing(UiKit.dp(this, 9), 1f);
        body.setTypeface(Typeface.create("serif", Typeface.NORMAL));
        StringBuilder sb = new StringBuilder();
        for (String p : PARAS) {
            sb.append("\u3000\u3000").append(p).append("\n\n");
        }
        body.setText(sb.toString());
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bLp.topMargin = UiKit.dp(this, 20);
        page.addView(body, bLp);

        TextView end = UiKit.text(this, "—— 样例文字，仅用于演示滚动 ——", 11f, 0xFFA8A29A);
        end.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams eLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        eLp.topMargin = UiKit.dp(this, 30);
        page.addView(end, eLp);

        setContentView(root);
    }

    /** 就地切速度：改完下一段手势立即按新速度走，不用退出重进 */
    @SuppressLint("SetTextI18n")
    private void showSpeedPicker() {
        final int[] presets = {20, 15, 10};
        final String[] labels = {"慢速 · 20 秒/屏", "标准 · 15 秒/屏", "快速 · 10 秒/屏"};
        int current = Prefs.getScreenSeconds(this);
        int checked = 1;
        for (int i = 0; i < presets.length; i++) {
            if (presets[i] == current) {
                checked = i;
                break;
            }
        }
        new AlertDialog.Builder(this)
                .setTitle("滚动速度")
                .setSingleChoiceItems(labels, checked, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Prefs.setScreenSeconds(PreviewActivity.this, presets[which]);
                        if (speedTv != null) {
                            speedTv.setText(presets[which] + " 秒/屏 ▾");
                        }
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void updateToggle() {
        if (toggleTv == null) {
            return;
        }
        if (bottomReached) {
            toggleTv.setText("重读");
            toggleTv.setTextColor(0xFF2F6B4F);
            return;
        }
        toggleTv.setText(started ? "暂停" : "继续");
        toggleTv.setTextColor(0xFFC0392B);
    }
}
