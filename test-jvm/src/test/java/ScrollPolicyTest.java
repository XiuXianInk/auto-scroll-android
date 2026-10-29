import com.lester.autoscroll.ScrollPolicy;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ScrollPolicyTest {

    private static final String SELF = "com.lester.autoscroll";
    private static final String FANQIE = "com.dragon.read";

    @Test
    public void 番茄小说前台且未限定时应滚() {
        assertTrue(ScrollPolicy.shouldScroll(FANQIE, SELF, ""));
    }

    @Test
    public void 自己的界面永不滚() {
        assertFalse(ScrollPolicy.shouldScroll(SELF, SELF, ""));
        assertFalse(ScrollPolicy.shouldScroll(SELF, SELF, SELF));
    }

    @Test
    public void 通知栏和状态栏不滚() {
        assertFalse(ScrollPolicy.shouldScroll("com.android.systemui", SELF, ""));
        assertFalse(ScrollPolicy.shouldScroll("com.android.systemui.recents", SELF, ""));
    }

    @Test
    public void 未拿到前台应用时不拦着() {
        assertTrue(ScrollPolicy.shouldScroll(null, SELF, ""));
        assertTrue(ScrollPolicy.shouldScroll("", SELF, ""));
    }

    @Test
    public void 限定了应用时只在目标内滚() {
        assertTrue(ScrollPolicy.shouldScroll(FANQIE, SELF, FANQIE));
        assertFalse(ScrollPolicy.shouldScroll("com.tencent.mm", SELF, FANQIE));
        assertFalse(ScrollPolicy.shouldScroll("com.miui.home", SELF, FANQIE));
    }

    @Test
    public void 限定包名带空格也能匹配() {
        assertTrue(ScrollPolicy.shouldScroll(FANQIE, SELF, "  " + FANQIE + "  "));
    }

    @Test
    public void 限定包名为空等于不限() {
        assertTrue(ScrollPolicy.shouldScroll("com.tencent.mm", SELF, null));
        assertTrue(ScrollPolicy.shouldScroll("com.tencent.mm", SELF, "   "));
    }

    @Test
    public void 空白限定不误伤自己界面判断() {
        // 自己界面判断优先于限定判断
        assertFalse(ScrollPolicy.shouldScroll(SELF, SELF, FANQIE));
    }

    @Test
    public void 预览模式下本应用也允许滚动() {
        // 预览页是本应用的 Activity，平时会被排除，但用户进预览就是为了看效果
        assertTrue(ScrollPolicy.shouldScroll(SELF, SELF, "", true));
        // 即便设了限定应用，预览页也要能滚
        assertTrue(ScrollPolicy.shouldScroll(SELF, SELF, FANQIE, true));
        assertTrue(ScrollPolicy.shouldScroll(FANQIE, SELF, "", true));
    }

    @Test
    public void 退出预览后恢复原有判断() {
        assertFalse(ScrollPolicy.shouldScroll(SELF, SELF, "", false));
        assertTrue(ScrollPolicy.shouldScroll(FANQIE, SELF, "", false));
    }
}
