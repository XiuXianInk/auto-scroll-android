import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.lester.autoscroll.MainActivity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowSettings;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * 在 JVM 里真实实例化 Activity，验证那段纯代码构建的界面能否跑起来。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class MainActivityTest {

    @Test
    public void 界面能创建且不抛异常() {
        ActivityController<MainActivity> controller =
                Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = controller.get();
        assertNotNull(activity);

        View root = activity.findViewById(android.R.id.content);
        assertNotNull("根视图为空", root);
        assertTrue("界面里没有挂任何子视图", ((ViewGroup) root).getChildCount() > 0);
    }

    @Test
    public void 关键控件与文案齐全() {
        ActivityController<MainActivity> controller =
                Robolectric.buildActivity(MainActivity.class).setup();
        List<String> texts = collectTexts(controller.get().findViewById(android.R.id.content));

        assertTrue("缺少主按钮引导文案，实际文本: " + texts, contains(texts, "启用无障碍服务"));
        assertTrue("缺少速度标题，实际文本: " + texts, contains(texts, "滚动速度"));
        assertTrue("缺少速率换算，实际文本: " + texts, contains(texts, "屏/分钟"));
        assertTrue("缺少速度预设，实际文本: " + texts, contains(texts, "慢速"));
        assertTrue("缺少应用范围，实际文本: " + texts, contains(texts, "应用范围"));
        assertTrue("缺少无障碍入口，实际文本: " + texts, contains(texts, "无障碍服务"));
        assertTrue("缺少悬浮窗入口，实际文本: " + texts, contains(texts, "悬浮窗"));
        assertTrue("缺少预览入口，实际文本: " + texts, contains(texts, "预览"));
    }

    @Test
    public void 界面至少包含一个可交互控件() {
        ActivityController<MainActivity> controller =
                Robolectric.buildActivity(MainActivity.class).setup();
        assertTrue("开始按钮应可点击",
                hasClickable(controller.get().findViewById(android.R.id.content)));
    }

    @Test
    public void 完整生命周期来回切换不崩() {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class);
        controller.create().start().resume();
        controller.pause().stop();
        controller.restart().resume();
        controller.pause().stop().destroy();
    }

    /**
     * 主按钮只由无障碍决定。悬浮窗是可选的——软件一直挂后台，
     * 想调设置切回本页就行，没理由因为它拦着不让开始。
     */
    @Test
    public void 主按钮只受无障碍影响() {
        ActivityController<MainActivity> controller =
                Robolectric.buildActivity(MainActivity.class);
        controller.create().start().resume();
        MainActivity a = controller.get();
        View root = a.findViewById(android.R.id.content);

        List<String> t1 = collectTexts(root);
        assertTrue("无障碍未启用时应引导去启用，实际: " + t1,
                contains(t1, "启用无障碍服务"));

        // 打开无障碍，悬浮窗仍未授权
        Settings.Secure.putString(a.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                "com.lester.autoscroll/.ScrollService");
        ShadowSettings.setCanDrawOverlays(false);
        controller.pause().resume();
        List<String> t2 = collectTexts(root);
        assertTrue("无障碍就绪就该能开始，不该被悬浮窗拦住，实际: " + t2,
                contains(t2, "开始滚动"));
    }

    private static boolean hasClickable(View v) {
        if (v.isClickable() && v instanceof TextView) {
            return true;
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                if (hasClickable(g.getChildAt(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean contains(List<String> list, String key) {
        for (String s : list) {
            if (s != null && s.contains(key)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> collectTexts(View v) {
        List<String> out = new ArrayList<>();
        walk(v, out);
        return out;
    }

    private static void walk(View v, List<String> out) {
        if (v instanceof TextView) {
            CharSequence t = ((TextView) v).getText();
            if (t != null) {
                out.add(t.toString());
            }
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                walk(g.getChildAt(i), out);
            }
        }
    }
}
