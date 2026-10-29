import com.lester.autoscroll.Prefs;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import android.content.Context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * 数据层。之前一直没测过，参数存错会直接导致"设了没生效"这类最烦人的问题。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class PrefsTest {

    private Context ctx;

    @Before
    public void setUp() {
        ctx = RuntimeEnvironment.getApplication();
        Prefs.sp(ctx).edit().clear().commit();
    }

    @Test
    public void 没有存过时用默认十五秒() {
        assertEquals(15, Prefs.getScreenSeconds(ctx));
    }

    @Test
    public void 速度读写往返一致() {
        for (int v : new int[]{3, 10, 15, 20, 60}) {
            Prefs.setScreenSeconds(ctx, v);
            assertEquals("写入 " + v + " 后读回不一致", v, Prefs.getScreenSeconds(ctx));
        }
    }

    @Test
    public void 应用范围默认是空() {
        assertEquals("", Prefs.getTargetPkg(ctx));
    }

    @Test
    public void 应用范围会去掉首尾空格() {
        Prefs.setTargetPkg(ctx, "  com.dragon.read  ");
        assertEquals("com.dragon.read", Prefs.getTargetPkg(ctx));
    }

    @Test
    public void 应用范围传空值不会变成null() {
        Prefs.setTargetPkg(ctx, null);
        assertEquals("", Prefs.getTargetPkg(ctx));
        // 空串要和 null 一样被当成"不限定"，不能抛异常
        assertTrue(Prefs.getTargetPkg(ctx) != null);
    }

    @Test
    public void 同一份参数会被后续读取拿到() {
        Prefs.setScreenSeconds(ctx, 25);
        Prefs.setTargetPkg(ctx, "com.dragon.read");
        // 换一个 ctx 重新读（模拟 Activity 重建）
        Context again = RuntimeEnvironment.getApplication();
        assertEquals(25, Prefs.getScreenSeconds(again));
        assertEquals("com.dragon.read", Prefs.getTargetPkg(again));
    }

    @Test
    public void 悬浮球位置默认为未记录() {
        assertEquals("没拖过时 X 应返回 -1", -1, Prefs.getBallXRatio(ctx));
        assertEquals("没拖过时 Y 应返回 -1", -1, Prefs.getBallYRatio(ctx));
    }

    @Test
    public void 悬浮球位置读写往返() {
        Prefs.setBallPos(ctx, 320, 780);
        assertEquals(320, Prefs.getBallXRatio(ctx));
        assertEquals(780, Prefs.getBallYRatio(ctx));
    }

    @Test
    public void 悬浮球提示只弹一次的标志() {
        assertEquals("默认应还没提示过", false, Prefs.isFloatHintShown(ctx));
        Prefs.setFloatHintShown(ctx, true);
        assertEquals("标记后不应再提示", true, Prefs.isFloatHintShown(ctx));
    }
}
