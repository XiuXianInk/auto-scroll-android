package com.lester.autoscroll;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 参数存本地，服务启动时读。
 */
public class Prefs {

    private static final String NAME = "autoscroll";

    public static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    /**
     * 滚动速度：滚完一屏要多少秒。
     * 默认 15 秒 —— 实测下来这个速度是"能慢慢看"的节奏；
     * 数字越小滚得越快，连续性由服务端匀速手势链保证。
     */
    public static int getScreenSeconds(Context c) {
        return sp(c).getInt("screenSeconds", 15);
    }

    public static void setScreenSeconds(Context c, int v) {
        sp(c).edit().putInt("screenSeconds", v).apply();
    }

    /** 只在指定应用里滚动，留空表示所有应用都滚 */
    public static String getTargetPkg(Context c) {
        return sp(c).getString("pkg", "");
    }

    public static void setTargetPkg(Context c, String v) {
        sp(c).edit().putString("pkg", v == null ? "" : v.trim()).apply();
    }

    /** 悬浮球用法提示是否已经展示过，只提示一次 */
    public static boolean isFloatHintShown(Context c) {
        return sp(c).getBoolean("floatHint", false);
    }

    public static void setFloatHintShown(Context c, boolean v) {
        sp(c).edit().putBoolean("floatHint", v).apply();
    }

    /**
     * 悬浮球位置，存千分比而不是像素——换分辨率或旋转屏幕后依然落在相对位置。
     * 返回 -1 表示用户还没拖过，用默认位置。
     */
    public static int getBallXRatio(Context c) {
        return sp(c).getInt("ballX", -1);
    }

    public static int getBallYRatio(Context c) {
        return sp(c).getInt("ballY", -1);
    }

    public static void setBallPos(Context c, int xRatio, int yRatio) {
        sp(c).edit().putInt("ballX", xRatio).putInt("ballY", yRatio).apply();
    }
}
