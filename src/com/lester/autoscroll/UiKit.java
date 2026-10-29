package com.lester.autoscroll;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.widget.TextView;

/**
 * 界面的一套设计常量与形状工具。
 * 尺寸单位与设计稿 1:1 对应（设计稿按 393dp 宽出图）。
 */
public final class UiKit {

    // ---- 色板 ----
    public static final int BG = 0xFF08080A;
    public static final int CARD = 0xFF141417;
    public static final int CARD_LINE = 0x14FFFFFF;
    public static final int ACCENT = 0xFFFF4B3E;
    public static final int ACCENT_HI = 0xFFFF7A5C;
    public static final int ACCENT_DEEP = 0xFFEE3227;
    public static final int TEXT = 0xFFF2F2F4;
    public static final int MUTED = 0xFF85858E;
    public static final int DIM = 0xFF5E5E66;
    public static final int FAINT = 0xFF48484F;
    public static final int GREEN = 0xFF34D399;
    public static final int TRACK = 0xFF232328;
    public static final int FIELD = 0xFF1B1B20;

    // ---- 速度区间见 ScrollMath.UI_MIN_SECS / UI_MAX_SECS ----

    private UiKit() {
    }

    public static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                c.getResources().getDisplayMetrics()));
    }

    /** 常规字重 */
    public static TextView text(Context c, CharSequence s, float sp, int color) {
        return text(c, s, sp, color, false);
    }

    /** medium 走系统 sans-serif-medium，中文也能吃到稍重的字重 */
    public static TextView text(Context c, CharSequence s, float sp, int color, boolean medium) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (medium) {
            t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        }
        return t;
    }

    public static GradientDrawable roundRect(Context c, int color, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    public static GradientDrawable roundRectStroke(Context c, int color, int strokeColor,
                                                   float radiusDp, float strokeDp) {
        GradientDrawable g = roundRect(c, color, radiusDp);
        g.setStroke(Math.max(1, dp(c, strokeDp)), strokeColor);
        return g;
    }

    /** 卡片底色：深灰 + 极细亮边，深色背景下才有"浮起来"的感觉 */
    public static GradientDrawable card(Context c) {
        return roundRectStroke(c, CARD, CARD_LINE, 20f, 0.6f);
    }

    public static GradientDrawable oval(Context c, int color, float sizeDp) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(color);
        int px = dp(c, sizeDp);
        g.setSize(px, px);
        return g;
    }

    public static GradientDrawable gradient(Context c, int[] colors, float radiusDp) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors);
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    /** 顶部氛围光：径向渐变，中心偏上，模拟屏幕被暖光打亮 */
    public static GradientDrawable topGlow(Context c) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.RECTANGLE);
        g.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        g.setGradientRadius(dp(c, 260));
        g.setGradientCenter(0.5f, 0.02f);
        g.setColors(new int[]{0x30FF4B3E, 0x0CFF4B3E, 0x00000000});
        return g;
    }
}
