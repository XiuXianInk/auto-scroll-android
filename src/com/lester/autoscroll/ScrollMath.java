package com.lester.autoscroll;

/**
 * 滚动几何计算。纯 Java，不碰 Android API，方便离线跑测试。
 *
 * 设计目标：
 * 1) 速度恒定 —— 每段位移严格等于「段时长 ÷ 一屏用时」，不因拆段而漂移；
 * 2) 段长自适应 —— 慢速时缩短段长（接缝丢失的位移小到看不见），快速时拉长段长（减少接缝次数）；
 * 3) 位移有下限 —— 低于 touch slop 会被系统判成点击，反而触发目标 App 的点击行为。
 */
public final class ScrollMath {

    /** 每段的目标位移：屏高的四分之一 */
    public static final float CHUNK_SPAN = 0.25f;

    /**
     * 段时长上下限。
     *
     * 这里踩过一次坑，记下来：曾为了"点暂停更快生效"把上限压到 1.2 秒，
     * 结果段数翻倍，接缝处"停一下"的次数也跟着翻倍——手感反而更差。
     * 实测那个停顿是注入手势本身的间隙（20~80ms 系统级准备时间），
     * 与段长短无关，缩短段长只会让它更频繁。
     * 因此维持较长段长，牺牲一点暂停响应，换少一半的接缝。
     */
    public static final int CHUNK_MIN_MS = 1500;
    public static final int CHUNK_MAX_MS = 6000;

    /** 极慢时为保住位移下限，允许把段拉长到这个上限 */
    public static final int CHUNK_HARD_MAX_MS = 10000;

    /** 单段最多滑 70% 屏高，上下留边 */
    public static final float MAX_SPAN = 0.70f;

    /** 手指落点与行程下限 */
    public static final float START_RATIO = 0.80f;
    public static final float MIN_Y_RATIO = 0.10f;

    /** 位移下限（dp），必须明显大于 touch slop（约 8dp） */
    public static final float MIN_TRAVEL_DP = 32f;

    /** 一屏用时的合法区间（秒），算法层兜底用 */
    public static final int SECS_MIN = 3;
    public static final int SECS_MAX = 200;

    // ---- 界面滑块用的取值范围与映射 ----

    /** 滑块区间：3~60 秒。180 秒那种基本用不到，占着行程反而害得常用段没法微调 */
    public static final int UI_MIN_SECS = 3;
    public static final int UI_MAX_SECS = 60;
    public static final int UI_STEPS = 100;

    private ScrollMath() {
    }

    /**
     * 滑块进度 → 秒数，对数映射。
     * 线性映射下 10~20 秒只占行程的十分之一，手指一动就跨好几秒；
     * 对数映射把慢速段拉开，每秒占用的行程更均匀，好微调。
     */
    public static int progressToSecs(int progress) {
        if (progress <= 0) {
            return UI_MIN_SECS;
        }
        if (progress >= UI_STEPS) {
            return UI_MAX_SECS;
        }
        double ratio = (double) UI_MAX_SECS / UI_MIN_SECS;
        double v = UI_MIN_SECS * Math.pow(ratio, progress / (double) UI_STEPS);
        long r = Math.round(v);
        if (r < UI_MIN_SECS) {
            return UI_MIN_SECS;
        }
        return (int) Math.min(r, UI_MAX_SECS);
    }

    /** 秒数 → 滑块进度，是 progressToSecs 的逆映射 */
    public static int secsToProgress(int secs) {
        if (secs <= UI_MIN_SECS) {
            return 0;
        }
        if (secs >= UI_MAX_SECS) {
            return UI_STEPS;
        }
        double ratio = (double) UI_MAX_SECS / UI_MIN_SECS;
        double p = Math.log((double) secs / UI_MIN_SECS) / Math.log(ratio) * UI_STEPS;
        long r = Math.round(p);
        if (r < 0) {
            return 0;
        }
        return (int) Math.min(r, UI_STEPS);
    }

    /**
     * 纵向是否已经滚到底。
     *
     * @param childBottom 内容底边相对容器的位置
     * @param viewHeight  容器高度
     * @param scrollY     当前滚动量
     * @param tolerance   容差（像素），避免浮点误差导致永远差一点点
     */
    public static boolean isScrolledToBottom(int childBottom, int viewHeight, int scrollY,
                                             int tolerance) {
        if (viewHeight <= 0) {
            return false;
        }
        int remain = childBottom - (viewHeight + scrollY);
        return remain <= tolerance;
    }

    /**
     * 手势下发失败后的退避。
     *
     * 开始用帧级延迟（上一段刚结束时资源可能还没释放，属于正常拒绝，等一下就好）；
     * 连续失败说明屏幕正被用户操作，这时必须让路——越退越久，避免和用户的手指抢手势。
     *
     * @param attempt 第几次重试，从 0 开始
     * @return 建议延迟，毫秒
     */
    public static int retryDelayMs(int attempt) {
        if (attempt <= 0) {
            return 16;
        }
        int[] steps = {16, 32, 64, 128, 256, 512};
        if (attempt < steps.length) {
            return steps[attempt];
        }
        return 512;
    }

    /** 连续失败到这个次数，基本可以判定用户在手动操作屏幕，此时放慢节奏而不是硬顶 */
    public static final int RETRY_YIELD_THRESHOLD = 6;

    public static final class Chunk {
        public final int chunkMs;
        public final float startY;
        public final float endY;
        /** 本段滑过的屏高比例，用于校验 */
        public final float span;

        Chunk(int chunkMs, float startY, float endY, float span) {
            this.chunkMs = chunkMs;
            this.startY = startY;
            this.endY = endY;
            this.span = span;
        }

        /** 实际速度：屏/分钟 */
        public float screensPerMinute() {
            return span / (chunkMs / 1000f) * 60f;
        }

        /** 每段位移（像素） */
        public float travelPx(float screenH) {
            return span * screenH;
        }
    }

    /**
     * @param secs      一屏用时（秒），会被夹到 [3, 200]
     * @param screenH   屏幕高度（像素）
     * @param density   屏幕密度
     */
    public static Chunk compute(int secs, int screenH, float density) {
        if (secs < SECS_MIN) {
            secs = SECS_MIN;
        }
        if (secs > SECS_MAX) {
            secs = SECS_MAX;
        }
        if (screenH <= 0) {
            screenH = 1;
        }
        if (density <= 0) {
            density = 1f;
        }

        int chunkMs = Math.round(secs * 1000f * CHUNK_SPAN);
        if (chunkMs < CHUNK_MIN_MS) {
            chunkMs = CHUNK_MIN_MS;
        }
        if (chunkMs > CHUNK_MAX_MS) {
            chunkMs = CHUNK_MAX_MS;
        }

        float span = (float) chunkMs / (secs * 1000f);
        if (span > MAX_SPAN) {
            span = MAX_SPAN;
            chunkMs = Math.round(MAX_SPAN * secs * 1000f);
        }
        if (chunkMs < 400) {
            chunkMs = 400;
        }
        if (span < 0.002f) {
            span = 0.002f;
        }

        // 位移太小时系统会把手势判成点击（touch slop 约 8dp），
        // 这里兜住下限，并按比例拉长段时长，保证速度不被改变。
        float minPx = MIN_TRAVEL_DP * density;
        if (screenH * span < minPx) {
            span = minPx / screenH;
            chunkMs = Math.round(span * secs * 1000f);
            if (chunkMs > CHUNK_HARD_MAX_MS) {
                chunkMs = CHUNK_HARD_MAX_MS;
            }
        }

        float startY = screenH * START_RATIO;
        float endY = startY - screenH * span;
        float minY = screenH * MIN_Y_RATIO;
        if (endY < minY) {
            // 行程太长，起点往上抬，保证终点不出屏
            startY = minY + screenH * span;
            endY = minY;
        }

        return new Chunk(chunkMs, startY, endY, span);
    }
}
