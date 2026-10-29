import com.lester.autoscroll.ScrollService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Method;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * 无障碍服务的初始化与状态机。dispatchGesture 在 Robolectric 里没有真实实现，
 * 代码里已用 try-catch 兜住，这里验证的是"不会因为异常把服务带崩"。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class ScrollServiceTest {

    private ScrollService buildAndConnect() throws Exception {
        ScrollService s = Robolectric.buildService(ScrollService.class).create().get();
        Method m = ScrollService.class.getDeclaredMethod("onServiceConnected");
        m.setAccessible(true);
        m.invoke(s);
        return s;
    }

    @Test
    public void 服务连接过程不抛异常() throws Exception {
        ScrollService s = buildAndConnect();
        assertNotNull(s);
        assertFalse("刚连上时不应处于运行状态", s.isRunning());
    }

    @Test
    public void 开始后状态变为运行中() throws Exception {
        ScrollService s = buildAndConnect();
        s.startScroll();
        assertTrue("点开始后应处于运行状态", s.isRunning());
    }

    @Test
    public void 停止后状态归位() throws Exception {
        ScrollService s = buildAndConnect();
        s.startScroll();
        s.stopScroll();
        assertFalse("停止后不应还在运行", s.isRunning());
    }

    @Test
    public void 反复开关不会卡在错误的运行态() throws Exception {
        ScrollService s = buildAndConnect();
        for (int i = 0; i < 5; i++) {
            s.toggle();
            assertTrue("第 " + (i + 1) + " 次切换后应为运行", s.isRunning());
            s.toggle();
            assertFalse("第 " + (i + 1) + " 次切换后应为停止", s.isRunning());
        }
    }

    @Test
    public void 重复调用开始不会重复启动() throws Exception {
        ScrollService s = buildAndConnect();
        s.startScroll();
        s.startScroll();
        s.startScroll();
        assertTrue(s.isRunning());
    }

    @Test
    public void 未连接时读取前台包名不崩() {
        ScrollService s = Robolectric.buildService(ScrollService.class).create().get();
        assertNotNull(s.getCurrentPkg());
    }
}
