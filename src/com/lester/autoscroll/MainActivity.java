package com.lester.autoscroll;

import android.app.Activity;
import android.content.Intent;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity {

    /** 权限卡的三个可变部件 */
    private static class PermCard {
        LinearLayout root;
        View dot;
        TextView state;
        TextView cta;
    }

    /** 速度预设胶囊 */
    private static class Chip {
        LinearLayout root;
        TextView title;
        TextView sub;
        int secs;
    }

    private PermCard accPerm;
    private PermCard ovPerm;

    private TextView bigNumTv;
    private TextView metaTv;
    private SeekBar speedBar;
    private Chip[] chips;
    private PlayIconView playIcon;
    private LinearLayout mainBtn;
    private TextView mainLabel;
    private EditText pkgInput;
    private TextView pkgNowTv;
    private TextView appHint;
    private LinearLayout appContent;
    private TextView restrictTip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(UiKit.BG);
        getWindow().setNavigationBarColor(UiKit.BG);
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 手输的包名要落盘，否则切页面就丢
        if (pkgInput != null) {
            Prefs.setTargetPkg(this, pkgInput.getText().toString());
        }
    }

    // ==================== 界面 ====================

    private void buildUi() {
        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(UiKit.BG);

        // 顶部氛围光
        View glow = new View(this);
        glow.setBackground(UiKit.topGlow(this));
        FrameLayout.LayoutParams gLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, UiKit.dp(this, 260));
        gLp.gravity = Gravity.TOP;
        gLp.topMargin = -UiKit.dp(this, 70);
        frame.addView(glow, gLp);

        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(UiKit.dp(this, 20), UiKit.dp(this, 40),
                UiKit.dp(this, 20), UiKit.dp(this, 26));
        sv.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        frame.addView(sv, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(frame);

        buildHeader(root);
        buildPerms(root);
        buildSpeedCard(root);
        buildAppCard(root);
        buildMainButton(root);
        buildFooter(root);

        int secs = Prefs.getScreenSeconds(this);
        updateSpeedUi(secs);
        if (speedBar != null) {
            speedBar.setProgress(ScrollMath.secsToProgress(clampSecs(secs)));
        }
    }

    private void buildHeader(LinearLayout root) {
        TextView eyebrow = UiKit.text(this, "AUTO SCROLL", 9.5f, UiKit.ACCENT, true);
        eyebrow.setLetterSpacing(0.24f);
        root.addView(eyebrow);

        TextView title = UiKit.text(this, "自动滚动", 27f, UiKit.TEXT, true);
        title.setLetterSpacing(-0.01f);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tLp.topMargin = UiKit.dp(this, 11);
        root.addView(title, tLp);

        TextView sub = UiKit.text(this, "按设定速度匀速滚动，适合长文阅读", 12.5f, 0xFF7C7C85);
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sLp.topMargin = UiKit.dp(this, 8);
        root.addView(sub, sLp);
    }

    private void buildPerms(LinearLayout root) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rLp.topMargin = UiKit.dp(this, 30);
        root.addView(row, rLp);

        accPerm = buildPermCard("无障碍服务");
        accPerm.root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                    toast("请在列表中找到「自动滚动」并启用");
                } catch (Exception e) {
                    toast("无法打开无障碍设置，请手动前往系统设置");
                }
            }
        });
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(accPerm.root, p1);

        ovPerm = buildPermCard("悬浮窗权限");
        ovPerm.root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName())));
                } catch (Exception e) {
                    toast("无法打开授权页面，请在系统设置中授予悬浮窗权限");
                }
            }
        });
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        p2.leftMargin = UiKit.dp(this, 10);
        row.addView(ovPerm.root, p2);

        // Android 13 起侧载应用启用无障碍会被拦，这里给出具体路径（仅在未启用时出现）
        restrictTip = UiKit.text(this,
                "若提示「出于安全考虑，此设置当前不可用」，请前往「应用信息 → 右上角菜单 → 允许受限设置」后重试。",
                10f, 0xFF6E6E76);
        restrictTip.setLineSpacing(UiKit.dp(this, 3), 1f);
        restrictTip.setVisibility(View.GONE);
        LinearLayout.LayoutParams tipLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tipLp.topMargin = UiKit.dp(this, 10);
        root.addView(restrictTip, tipLp);
    }

    private PermCard buildPermCard(String name) {
        PermCard p = new PermCard();

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(UiKit.card(this));
        card.setPadding(UiKit.dp(this, 14), UiKit.dp(this, 14),
                UiKit.dp(this, 14), UiKit.dp(this, 13));

        TextView n = UiKit.text(this, name, 11.5f, UiKit.MUTED);
        card.addView(n);

        LinearLayout stateRow = new LinearLayout(this);
        stateRow.setOrientation(LinearLayout.HORIZONTAL);
        stateRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams srLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        srLp.topMargin = UiKit.dp(this, 10);
        card.addView(stateRow, srLp);

        p.dot = new View(this);
        p.dot.setBackground(UiKit.oval(this, UiKit.FAINT, 6f));
        stateRow.addView(p.dot, new LinearLayout.LayoutParams(
                UiKit.dp(this, 6), UiKit.dp(this, 6)));

        p.state = UiKit.text(this, "检测中", 14f, UiKit.TEXT, true);
        LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        stLp.leftMargin = UiKit.dp(this, 6);
        stateRow.addView(p.state, stLp);

        p.cta = UiKit.text(this, "点此设置", 10f, UiKit.FAINT);
        LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cLp.topMargin = UiKit.dp(this, 9);
        card.addView(p.cta, cLp);

        p.root = card;
        return p;
    }

    private void buildSpeedCard(LinearLayout root) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(UiKit.card(this));
        card.setPadding(UiKit.dp(this, 18), UiKit.dp(this, 20),
                UiKit.dp(this, 18), UiKit.dp(this, 18));
        LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cLp.topMargin = UiKit.dp(this, 18);
        root.addView(card, cLp);

        // 标题行
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(titleRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView t = UiKit.text(this, "滚动速度", 12f, UiKit.MUTED);
        titleRow.addView(t, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        titleRow.addView(UiKit.text(this, "调整后立即生效", 10.5f, UiKit.DIM));

        // 大数字 + 折算
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.HORIZONTAL);
        hero.setGravity(Gravity.BOTTOM);
        LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hLp.topMargin = UiKit.dp(this, 14);
        card.addView(hero, hLp);

        LinearLayout numBox = new LinearLayout(this);
        numBox.setOrientation(LinearLayout.HORIZONTAL);
        // 靠基线对齐（LinearLayout 默认行为），比手动 margin 更稳，字号变了也不会歪
        numBox.setBaselineAligned(true);
        hero.addView(numBox, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        bigNumTv = UiKit.text(this, "40", 56f, UiKit.TEXT, true);
        bigNumTv.setIncludeFontPadding(false);
        bigNumTv.setLetterSpacing(-0.045f);
        numBox.addView(bigNumTv);

        TextView unit = UiKit.text(this, "秒", 14f, 0xFFFF6B5C, true);
        LinearLayout.LayoutParams uLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        uLp.leftMargin = UiKit.dp(this, 6);
        numBox.addView(unit, uLp);

        TextView per = UiKit.text(this, "/屏", 11f, UiKit.DIM);
        LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pLp.leftMargin = UiKit.dp(this, 3);
        numBox.addView(per, pLp);

        metaTv = UiKit.text(this, "", 11f, UiKit.DIM);
        LinearLayout.LayoutParams mLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mLp.bottomMargin = UiKit.dp(this, 6);
        hero.addView(metaTv, mLp);

        // 滑块
        speedBar = buildSpeedBar();
        LinearLayout.LayoutParams sbLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sbLp.topMargin = UiKit.dp(this, 18);
        card.addView(speedBar, sbLp);

        // 两端刻度
        LinearLayout scale = new LinearLayout(this);
        scale.setOrientation(LinearLayout.HORIZONTAL);
        scale.setPadding(UiKit.dp(this, 13), 0, UiKit.dp(this, 13), 0);
        LinearLayout.LayoutParams scLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        scLp.topMargin = UiKit.dp(this, 4);
        card.addView(scale, scLp);
        scale.addView(UiKit.text(this, ScrollMath.UI_MIN_SECS + " 秒", 9.5f, UiKit.FAINT),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView maxLabel = UiKit.text(this, ScrollMath.UI_MAX_SECS + " 秒", 9.5f, UiKit.FAINT);
        maxLabel.setGravity(Gravity.END);
        scale.addView(maxLabel, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        // 预设
        LinearLayout presetRow = new LinearLayout(this);
        presetRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams prLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        prLp.topMargin = UiKit.dp(this, 18);
        card.addView(presetRow, prLp);

        chips = new Chip[]{
                buildChip(presetRow, "慢速", "20 秒", 20, 0),
                buildChip(presetRow, "标准", "15 秒", 15, 1),
                buildChip(presetRow, "快速", "10 秒", 10, 2)
        };

        // 滑块联动
        speedBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int secs = ScrollMath.progressToSecs(progress);
                Prefs.setScreenSeconds(MainActivity.this, secs);
                updateSpeedUi(secs);
                notifyService();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    /** 进度条固定从物理左端开始填充，不随语言镜像（本应用只有中文界面） */
    @android.annotation.SuppressLint("RtlHardcoded")
    private SeekBar buildSpeedBar() {
        SeekBar sb = new SeekBar(this);
        sb.setMax(ScrollMath.UI_STEPS);

        GradientDrawable track = UiKit.roundRect(this, UiKit.TRACK, 3f);
        track.setSize(1, UiKit.dp(this, 6));

        GradientDrawable progress = UiKit.gradient(this,
                new int[]{0xFFFF3B2F, UiKit.ACCENT_HI}, 3f);
        progress.setSize(1, UiKit.dp(this, 6));
        ClipDrawable clip = new ClipDrawable(progress, Gravity.LEFT, ClipDrawable.HORIZONTAL);

        LayerDrawable layers = new LayerDrawable(new Drawable[]{track, clip});
        layers.setId(0, android.R.id.background);
        layers.setId(1, android.R.id.progress);
        sb.setProgressDrawable(layers);

        // 拇指：外圈淡红光 + 白色实心圆
        int thumbPx = UiKit.dp(this, 26);
        GradientDrawable halo = UiKit.oval(this, 0x22FF4B3E, 26f);
        GradientDrawable core = UiKit.oval(this, 0xFFFFFFFF, 18f);
        LayerDrawable thumb = new LayerDrawable(new Drawable[]{halo, core});
        int inset = (thumbPx - UiKit.dp(this, 18)) / 2;
        thumb.setLayerInset(1, inset, inset, inset, inset);
        sb.setThumb(thumb);

        // 两端留出拇指半径，滑动时拇指不会被裁掉
        int pad = thumbPx / 2;
        sb.setPadding(pad, 0, pad, 0);
        sb.setSplitTrack(false);
        sb.setMinimumHeight(UiKit.dp(this, 20));
        sb.setBackground(null);
        return sb;
    }

    private Chip buildChip(LinearLayout parent, String title, String sub, final int secs, int index) {
        Chip c = new Chip();
        c.secs = secs;

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(0, UiKit.dp(this, 10), 0, UiKit.dp(this, 9));

        c.title = UiKit.text(this, title, 11.5f, 0xFFA0A0A8);
        c.title.setGravity(Gravity.CENTER);
        c.sub = UiKit.text(this, sub, 9f, UiKit.DIM);
        c.sub.setGravity(Gravity.CENTER);

        box.addView(c.title);
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sLp.topMargin = UiKit.dp(this, 3);
        box.addView(c.sub, sLp);

        c.root = box;

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        if (index > 0) {
            lp.leftMargin = UiKit.dp(this, 8);
        }
        parent.addView(box, lp);

        box.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speedBar.setProgress(ScrollMath.secsToProgress(clampSecs(secs)));
            }
        });

        applyChipStyle(c, false);
        return c;
    }

    private void applyChipStyle(Chip c, boolean on) {
        if (on) {
            c.root.setBackground(UiKit.roundRectStroke(this, 0x21FF4B3E, 0x6BFF4B3E, 12f, 1f));
            c.title.setTextColor(0xFFFF8A7E);
            c.title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            c.sub.setTextColor(0x99FF8A7E);
        } else {
            c.root.setBackground(UiKit.roundRectStroke(this, 0xFF1B1B20, 0x0DFFFFFF, 12f, 1f));
            c.title.setTextColor(0xFFA0A0A8);
            c.title.setTypeface(Typeface.DEFAULT);
            c.sub.setTextColor(UiKit.DIM);
        }
    }

    /**
     * 应用范围默认收起：绝大多数人用不上，展开才占地方。
     */
    private void buildAppCard(LinearLayout root) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(UiKit.card(this));
        card.setPadding(UiKit.dp(this, 18), UiKit.dp(this, 16),
                UiKit.dp(this, 18), UiKit.dp(this, 16));
        LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cLp.topMargin = UiKit.dp(this, 18);
        root.addView(card, cLp);

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setMinimumHeight(UiKit.dp(this, 30));
        card.addView(titleRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        titleRow.addView(UiKit.text(this, "应用范围", 12f, UiKit.MUTED),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        appHint = UiKit.text(this, "未限定", 10.5f, UiKit.DIM);
        titleRow.addView(appHint);

        final LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setVisibility(View.GONE);
        card.addView(content, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        appContent = content;

        LinearLayout appRow = new LinearLayout(this);
        appRow.setOrientation(LinearLayout.HORIZONTAL);
        appRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams aLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        aLp.topMargin = UiKit.dp(this, 14);
        content.addView(appRow, aLp);

        pkgInput = new EditText(this);
        pkgInput.setText(Prefs.getTargetPkg(this));
        pkgInput.setHint("输入应用包名，留空表示不限");
        pkgInput.setHintTextColor(UiKit.DIM);
        pkgInput.setTextColor(UiKit.TEXT);
        pkgInput.setTextSize(12f);
        pkgInput.setSingleLine(true);
        pkgInput.setInputType(InputType.TYPE_CLASS_TEXT);
        pkgInput.setBackground(UiKit.roundRectStroke(this, UiKit.FIELD, 0x0DFFFFFF, 12f, 1f));
        pkgInput.setPadding(UiKit.dp(this, 13), UiKit.dp(this, 12),
                UiKit.dp(this, 13), UiKit.dp(this, 12));
        pkgInput.setMinHeight(0);
        pkgInput.setMinimumHeight(0);
        appRow.addView(pkgInput, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView pick = UiKit.text(this, "读取当前应用", 11.5f, 0xFFD8D8DE);
        pick.setGravity(Gravity.CENTER);
        pick.setBackground(UiKit.roundRectStroke(this, 0xFF232329, 0x0FFFFFFF, 12f, 1f));
        pick.setPadding(UiKit.dp(this, 14), UiKit.dp(this, 12),
                UiKit.dp(this, 14), UiKit.dp(this, 12));
        LinearLayout.LayoutParams pkLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pkLp.leftMargin = UiKit.dp(this, 9);
        appRow.addView(pick, pkLp);

        pick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ScrollService s = ScrollService.instance;
                String pkg = s == null ? "" : s.getCurrentPkg();
                if (pkg == null || pkg.length() == 0) {
                    toast("未获取到前台应用，请先打开目标应用后重试");
                    return;
                }
                pkgInput.setText(pkg);
                Prefs.setTargetPkg(MainActivity.this, pkg);
                notifyService();
                updateAppHint();
                toast("已限定为 " + pkg);
            }
        });

        // 诊断用：不生效时能一眼看出前台到底是谁
        pkgNowTv = UiKit.text(this, "", 10f, UiKit.DIM);
        LinearLayout.LayoutParams nLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nLp.topMargin = UiKit.dp(this, 10);
        content.addView(pkgNowTv, nLp);

        titleRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean expand = content.getVisibility() != View.VISIBLE;
                content.setVisibility(expand ? View.VISIBLE : View.GONE);
                updateAppHint();
            }
        });
    }

    private void updateAppHint() {
        if (appHint == null) {
            return;
        }
        boolean expanded = appContent != null && appContent.getVisibility() == View.VISIBLE;
        if (expanded) {
            appHint.setText("收起");
            appHint.setTextColor(UiKit.ACCENT);
            return;
        }
        String pkg = Prefs.getTargetPkg(this);
        boolean has = pkg != null && pkg.length() > 0;
        appHint.setText(has ? "已限定" : "未限定");
        appHint.setTextColor(has ? 0xFFB4763A : UiKit.DIM);
    }

    /**
     * 主按钮同时承担引导职责：权限没就绪时，它就是下一步要做的动作，
     * 而不是让用户点了才被告知缺权限。悬浮窗是「能随时暂停」的前提，所以同样强制。
     */
    private void buildMainButton(LinearLayout root) {
        mainBtn = new LinearLayout(this);
        mainBtn.setOrientation(LinearLayout.HORIZONTAL);
        mainBtn.setGravity(Gravity.CENTER);
        mainBtn.setBackground(UiKit.gradient(this,
                new int[]{0xFFFF5245, UiKit.ACCENT_DEEP}, 18f));
        mainBtn.setElevation(UiKit.dp(this, 6));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, UiKit.dp(this, 56));
        lp.topMargin = UiKit.dp(this, 28);
        root.addView(mainBtn, lp);

        playIcon = new PlayIconView(this, 9f, 11f, 0xFFFFFFFF);
        mainBtn.addView(playIcon);

        mainLabel = UiKit.text(this, "开始滚动", 15.5f, 0xFFFFFFFF, true);
        mainLabel.setLetterSpacing(0.03f);
        LinearLayout.LayoutParams lLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lLp.leftMargin = UiKit.dp(this, 9);
        mainBtn.addView(mainLabel, lLp);

        mainBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isAccEnabled()) {
                    toast("请先启用无障碍服务");
                    try {
                        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                    } catch (Exception ignored) {
                    }
                    return;
                }
                ScrollService s = ScrollService.instance;
                if (s == null) {
                    toast("服务正在连接，请稍候");
                    mainBtn.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            syncState();
                        }
                    }, 1200);
                    return;
                }
                s.refreshPanel();
                s.toggle();
                // 没悬浮窗不拦着，但要告诉用户去哪儿停
                if (s.isRunning() && !Settings.canDrawOverlays(MainActivity.this)) {
                    toast("已开始，暂停请回到本页操作");
                }
                syncState();
            }
        });
    }

    private void buildFooter(LinearLayout root) {
        TextView secondary = UiKit.text(this, "点此进入预览界面 ›", 11.5f, 0xFFFF6B5C);
        secondary.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sLp.topMargin = UiKit.dp(this, 20);
        root.addView(secondary, sLp);
        secondary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(MainActivity.this, PreviewActivity.class));
                } catch (Exception e) {
                    toast("无法打开预览界面");
                }
            }
        });

        TextView foot = UiKit.text(this, "悬浮球单击可开始或暂停，按住可拖动位置", 10f, 0xFF3A3A41);
        foot.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams fLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fLp.topMargin = UiKit.dp(this, 18);
        root.addView(foot, fLp);

        TextView version = UiKit.text(this, "v2.2", 9.5f, 0xFF2E2E34);
        version.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams vLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        vLp.topMargin = UiKit.dp(this, 14);
        root.addView(version, vLp);
    }

    // ==================== 状态 ====================

    // 界面文案全部为中文硬编码，不走多语言资源，拼接字符串是预期行为
    @android.annotation.SuppressLint("SetTextI18n")
    private void syncState() {
        boolean acc = isAccEnabled();
        boolean ov = Settings.canDrawOverlays(this);

        updatePerm(accPerm, acc, "已启用", "未启用", "管理", "去启用 ›");
        // 悬浮窗只是让暂停更顺手，不给也能正常用
        updatePerm(ovPerm, ov, "已授权", "未授权", "管理", "可选 · 点此授权 ›");

        if (restrictTip != null) {
            boolean show = Build.VERSION.SDK_INT >= 33 && !acc;
            restrictTip.setVisibility(show ? View.VISIBLE : View.GONE);
        }

        ScrollService s = ScrollService.instance;
        boolean running = s != null && s.isRunning();

        if (pkgNowTv != null) {
            String cur = s == null ? "" : s.getCurrentPkg();
            String target = Prefs.getTargetPkg(this);
            String now = (cur == null || cur.length() == 0) ? "未获取到" : cur;
            boolean matched = target == null || target.length() == 0
                    || target.equals(cur);
            pkgNowTv.setText("当前前台：" + now + (matched ? "" : "（与设定不符，不会滚动）"));
            pkgNowTv.setTextColor(matched ? UiKit.DIM : 0xFFB4763A);
        }
        updateAppHint();

        if (mainLabel != null) {
            // 只有无障碍是硬性前提。悬浮窗是可选的：不给也能用，只是暂停得回到本页。
            if (!acc) {
                mainLabel.setText("启用无障碍服务");
            } else {
                mainLabel.setText(running ? "停止滚动" : "开始滚动");
            }
            mainLabel.setTextColor(running ? 0xFFC9C9D0 : 0xFFFFFFFF);
        }
        if (playIcon != null) {
            playIcon.setVisibility(running ? View.INVISIBLE : View.VISIBLE);
        }
        if (mainBtn != null) {
            mainBtn.setBackground(UiKit.gradient(this,
                    running ? new int[]{0xFF2C2C31, 0xFF232328}
                            : new int[]{0xFFFF5245, UiKit.ACCENT_DEEP}, 18f));
        }

        int secs = Prefs.getScreenSeconds(this);
        int clamped = clampSecs(secs);
        if (clamped != secs) {
            // 旧版本可能存了超出新区间（3~60）的值，顺手迁移过来
            Prefs.setScreenSeconds(this, clamped);
        }
        updateSpeedUi(clamped);
        if (speedBar != null && speedBar.getProgress() != ScrollMath.secsToProgress(clamped)) {
            speedBar.setProgress(ScrollMath.secsToProgress(clamped));
        }
    }

    private void updatePerm(PermCard p, boolean granted, String ok, String bad,
                            String okCta, String badCta) {
        if (p == null) {
            return;
        }
        p.state.setText(granted ? ok : bad);
        p.dot.setBackground(UiKit.oval(this, granted ? UiKit.GREEN : 0xFFFF6B5B, 6f));
        p.cta.setText(granted ? okCta : badCta);
        p.cta.setTextColor(granted ? UiKit.FAINT : UiKit.ACCENT);
    }

    private void updateSpeedUi(int secs) {
        int s = clampSecs(secs);
        if (bigNumTv != null) {
            bigNumTv.setText(String.valueOf(s));
            applyNumShader();
        }
        if (metaTv != null) {
            metaTv.setText(buildMeta(s));
        }
        if (chips != null) {
            for (Chip c : chips) {
                applyChipStyle(c, c.secs == s);
            }
        }
    }

    /** 大数字用一层很浅的竖向渐变，纯白会显得发灰 */
    private void applyNumShader() {
        if (bigNumTv == null) {
            return;
        }
        int h = bigNumTv.getHeight();
        if (h <= 0) {
            h = UiKit.dp(this, 56);
        }
        Paint p = bigNumTv.getPaint();
        p.setShader(new LinearGradient(0f, 0f, 0f, h,
                0xFFFFFFFF, 0xFFFFC4BA, Shader.TileMode.CLAMP));
        bigNumTv.invalidate();
    }

    private CharSequence buildMeta(int secs) {
        float perMin = 60f / secs;
        SpannableStringBuilder sb = new SpannableStringBuilder("约 ");
        int start = sb.length();
        sb.append(String.format(Locale.US, "%.1f", perMin));
        int end = sb.length();
        sb.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new ForegroundColorSpan(0xFFC9C9D0), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new AbsoluteSizeSpan(13, true), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.append(" 屏/分钟");
        return sb;
    }

    private int clampSecs(int s) {
        if (s < ScrollMath.UI_MIN_SECS) {
            return ScrollMath.UI_MIN_SECS;
        }
        return Math.min(s, ScrollMath.UI_MAX_SECS);
    }

    private boolean isAccEnabled() {
        try {
            String full = getPackageName() + "/" + ScrollService.class.getName();
            String shortName = getPackageName() + "/." + ScrollService.class.getSimpleName();
            String enabled = Settings.Secure.getString(getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (enabled == null) {
                return false;
            }
            for (String s : enabled.split(":")) {
                if (s.equalsIgnoreCase(full) || s.equalsIgnoreCase(shortName)) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private void notifyService() {
        ScrollService s = ScrollService.instance;
        if (s != null) {
            s.refreshPanel();
        }
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
