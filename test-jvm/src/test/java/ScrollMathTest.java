import com.lester.autoscroll.ScrollMath;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 直接测运行时用的 ScrollMath，不是复制品。
 */
public class ScrollMathTest {

    private static final int H = 2400;
    private static final float DENSITY = 2.75f;

    @Test
    public void 速度在设定区间内恒等于目标值() {
        for (int secs = ScrollMath.SECS_MIN; secs <= ScrollMath.SECS_MAX; secs++) {
            ScrollMath.Chunk c = ScrollMath.compute(secs, H, DENSITY);
            float target = 60f / secs;                 // 屏/分钟
            float actual = c.screensPerMinute();
            float err = Math.abs(actual - target) / target;
            assertTrue("secs=" + secs + " 速度偏差 " + (err * 100) + "%", err < 0.005f);
        }
    }

    @Test
    public void 位移始终大于触摸阈值() {
        float slop = 8f * DENSITY;                     // 系统 touch slop
        for (int secs = ScrollMath.SECS_MIN; secs <= ScrollMath.SECS_MAX; secs++) {
            ScrollMath.Chunk c = ScrollMath.compute(secs, H, DENSITY);
            float travel = c.travelPx(H);
            assertTrue("secs=" + secs + " 位移仅 " + travel + "px，可能被判成点击", travel > slop * 2);
        }
    }

    @Test
    public void 行程始终在屏幕内且向下为正() {
        for (int secs = ScrollMath.SECS_MIN; secs <= ScrollMath.SECS_MAX; secs++) {
            ScrollMath.Chunk c = ScrollMath.compute(secs, H, DENSITY);
            assertTrue("起点越界 secs=" + secs + " startY=" + c.startY, c.startY >= 0 && c.startY <= H);
            assertTrue("终点越界 secs=" + secs + " endY=" + c.endY, c.endY >= 0 && c.endY <= H);
            assertTrue("方向反了 secs=" + secs, c.startY > c.endY);
        }
    }

    @Test
    public void 段时长有上下限() {
        for (int secs = ScrollMath.SECS_MIN; secs <= ScrollMath.SECS_MAX; secs++) {
            ScrollMath.Chunk c = ScrollMath.compute(secs, H, DENSITY);
            assertTrue("段太短 secs=" + secs + " " + c.chunkMs, c.chunkMs >= 400);
            assertTrue("段太长 secs=" + secs + " " + c.chunkMs, c.chunkMs <= ScrollMath.CHUNK_HARD_MAX_MS);
        }
    }

    @Test
    public void 超范围参数自动夹紧() {
        ScrollMath.Chunk low = ScrollMath.compute(-100, H, DENSITY);
        ScrollMath.Chunk lowEdge = ScrollMath.compute(ScrollMath.SECS_MIN, H, DENSITY);
        assertEquals("低于下限应夹到最小值", lowEdge.chunkMs, low.chunkMs);

        ScrollMath.Chunk high = ScrollMath.compute(999999, H, DENSITY);
        ScrollMath.Chunk highEdge = ScrollMath.compute(ScrollMath.SECS_MAX, H, DENSITY);
        assertEquals("高于上限应夹到最大值", highEdge.chunkMs, high.chunkMs);
    }

    @Test
    public void 异常屏幕尺寸不崩() {
        ScrollMath.Chunk c = ScrollMath.compute(20, 0, DENSITY);
        assertTrue("高度为 0 也要给出合法结果", c.chunkMs > 0 && c.endY <= c.startY);

        ScrollMath.Chunk d = ScrollMath.compute(20, H, 0f);
        assertTrue("密度为 0 也要给出合法结果", d.chunkMs > 0);
    }

    @Test
    public void 常见机型分辨率都正常() {
        int[][] screens = {{720, 1280}, {1080, 1920}, {1080, 2400}, {1220, 2712}, {1440, 3200}};
        float[] densities = {2.0f, 2.75f, 3.0f, 3.5f};
        for (int[] s : screens) {
            for (float d : densities) {
                for (int secs : new int[]{3, 10, 20, 60, 120, 200}) {
                    ScrollMath.Chunk c = ScrollMath.compute(secs, s[1], d);
                    assertTrue("异常: " + s[0] + "x" + s[1] + " dpi=" + d + " secs=" + secs,
                            c.startY > c.endY && c.endY >= 0 && c.startY <= s[1]);
                }
            }
        }
    }

    @Test
    public void 慢速档确实比快速档慢() {
        float fast = ScrollMath.compute(10, H, DENSITY).screensPerMinute();
        float slow = ScrollMath.compute(120, H, DENSITY).screensPerMinute();
        assertTrue("10 秒档应明显快于 120 秒档", fast > slow * 5);
    }

    // ---------- 滑块映射 ----------

    @Test
    public void 滑块映射端点正确() {
        assertEquals(ScrollMath.UI_MIN_SECS, ScrollMath.progressToSecs(0));
        assertEquals(ScrollMath.UI_MAX_SECS, ScrollMath.progressToSecs(ScrollMath.UI_STEPS));
        assertEquals(0, ScrollMath.secsToProgress(ScrollMath.UI_MIN_SECS));
        assertEquals(ScrollMath.UI_STEPS, ScrollMath.secsToProgress(ScrollMath.UI_MAX_SECS));
    }

    @Test
    public void 滑块映射单调递增() {
        int prev = -1;
        for (int p = 0; p <= ScrollMath.UI_STEPS; p++) {
            int s = ScrollMath.progressToSecs(p);
            assertTrue("进度 " + p + " 处不单调: " + s + " < " + prev, s >= prev);
            prev = s;
        }
    }

    @Test
    public void 往返映射误差不超过一秒() {
        for (int secs = ScrollMath.UI_MIN_SECS; secs <= ScrollMath.UI_MAX_SECS; secs++) {
            int p = ScrollMath.secsToProgress(secs);
            int back = ScrollMath.progressToSecs(p);
            assertTrue("secs=" + secs + " → p=" + p + " → " + back,
                    Math.abs(back - secs) <= 1);
        }
    }

    /** 三个预设必须能精确命中，否则档位高亮不会亮 */
    @Test
    public void 预设档位可以精确命中() {
        for (int preset : new int[]{10, 15, 20}) {
            int p = ScrollMath.secsToProgress(preset);
            assertEquals("预设 " + preset + " 秒无法精确命中", preset,
                    ScrollMath.progressToSecs(p));
        }
    }

    /** 对数映射的意义：常用低速段要比线性映射占更多行程 */
    @Test
    public void 常用速度段占用行程优于线性映射() {
        int p10 = ScrollMath.secsToProgress(10);
        int p20 = ScrollMath.secsToProgress(20);
        int span = p20 - p10;
        int linear = Math.round(10f / (ScrollMath.UI_MAX_SECS - ScrollMath.UI_MIN_SECS)
                * ScrollMath.UI_STEPS);
        assertTrue("10~20 秒占用行程 " + span + " 应优于线性映射的 " + linear,
                span > linear);
    }

    // ---------- 重试退避 ----------

    @Test
    public void 首次重试走帧级延迟() {
        assertTrue("第一次重试必须是帧级，衔接才不会顿", ScrollMath.retryDelayMs(0) <= 16);
        assertTrue(ScrollMath.retryDelayMs(1) <= 32);
    }

    @Test
    public void 退避单调递增且封顶() {
        int prev = 0;
        for (int i = 0; i < 12; i++) {
            int d = ScrollMath.retryDelayMs(i);
            assertTrue("第 " + i + " 次退避 " + d + " 不应小于上一次 " + prev, d >= prev);
            assertTrue("退避不应超过 512ms 上限: " + d, d <= 512);
            prev = d;
        }
        assertEquals("多次失败后应稳定在 512ms", 512, ScrollMath.retryDelayMs(20));
    }

    @Test
    public void 让路阈值存在且合理() {
        assertTrue("让路阈值应大于帧级重试次数", ScrollMath.RETRY_YIELD_THRESHOLD > 3);
        assertTrue("让路阈值不应太晚", ScrollMath.RETRY_YIELD_THRESHOLD <= 10);
    }

    @Test
    public void 到底判定覆盖各种边界() {
        // 内容 1000、容器 800、滚 200 → 正好到底
        assertTrue("正好到底应判定为 true", ScrollMath.isScrolledToBottom(1000, 800, 200, 4));
        // 还差 3px，在容差内
        assertTrue("容差内的 3px 应算到底", ScrollMath.isScrolledToBottom(1000, 800, 197, 4));
        // 还差 10px，超出容差
        assertFalse("差 10px 不应算到底", ScrollMath.isScrolledToBottom(1000, 800, 190, 4));
        // 内容不满一屏，本来就不需要滚，视为到底
        assertTrue("内容短于容器应视为到底", ScrollMath.isScrolledToBottom(500, 800, 0, 4));
        // 容器高度非法
        assertFalse("容器高度为 0 不应判到底", ScrollMath.isScrolledToBottom(1000, 0, 0, 4));
    }
}
