package com.lester.autoscroll;

/**
 * 决定"此刻该不该滚"。纯 Java，方便离线测试。
 */
public final class ScrollPolicy {

    private ScrollPolicy() {
    }

    /**
     * @param currentPkg 当前前台应用的包名，可能为空（还没收到窗口事件）
     * @param selfPkg    本应用包名
     * @param targetPkg  用户设定的限定包名，可为空表示不限
     */
    public static boolean shouldScroll(String currentPkg, String selfPkg, String targetPkg) {
        return shouldScroll(currentPkg, selfPkg, targetPkg, false);
    }

    /**
     * @param previewMode 是否在预览页。预览页属于本应用，平时是被排除的，
     *                    但用户进预览就是为了看效果，必须放行。
     */
    public static boolean shouldScroll(String currentPkg, String selfPkg, String targetPkg,
                                       boolean previewMode) {
        if (previewMode) {
            return true;
        }
        if (currentPkg == null || currentPkg.length() == 0) {
            // 还没拿到前台应用信息，不拦着
            return true;
        }
        if (selfPkg != null && selfPkg.equals(currentPkg)) {
            // 自己的设置界面不滚，免得误触
            return false;
        }
        if (currentPkg.startsWith("com.android.systemui")) {
            // 通知栏、状态栏拉下来的场景不滚
            return false;
        }
        String target = targetPkg == null ? "" : targetPkg.trim();
        if (!target.isEmpty()) {
            return target.equals(currentPkg);
        }
        return true;
    }
}
