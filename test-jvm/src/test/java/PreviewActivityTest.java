import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.lester.autoscroll.Prefs;
import com.lester.autoscroll.PreviewActivity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * 预览页：能不能打开、有没有可滚动的正文、滚动容器是否就位。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, qualifiers = "w393dp-h852dp-xhdpi")
public class PreviewActivityTest {

    private ActivityController<PreviewActivity> open() {
        return Robolectric.buildActivity(PreviewActivity.class).setup();
    }

    @Test
    public void 预览页能正常打开() {
        PreviewActivity a = open().get();
        assertNotNull(a);
        View root = a.findViewById(android.R.id.content);
        assertNotNull("根视图为空", root);
        assertTrue("没有任何子视图", ((ViewGroup) root).getChildCount() > 0);
    }

    @Test
    public void 正文内容足够长可以滚动() {
        PreviewActivity a = open().get();
        List<TextView> texts = new ArrayList<>();
        collect(a.findViewById(android.R.id.content), texts);

        int longest = 0;
        for (TextView t : texts) {
            CharSequence c = t.getText();
            if (c != null) {
                longest = Math.max(longest, c.length());
            }
        }
        // 样例文字要足够撑出多屏，否则预览看不出效果
        assertTrue("正文长度只有 " + longest + " 字，不足以演示滚动", longest > 1500);
    }

    @Test
    public void 存在可滚动的容器() {
        PreviewActivity a = open().get();
        List<ScrollView> list = new ArrayList<>();
        findScroll(a.findViewById(android.R.id.content), list);
        assertTrue("没有找到 ScrollView", !list.isEmpty());

        ScrollView sv = list.get(0);
        View child = sv.getChildAt(0);
        assertNotNull(child);
        // 内容高度应远超容器，否则滚不动
        child.measure(View.MeasureSpec.makeMeasureSpec(786, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        assertTrue("正文高度只有 " + child.getMeasuredHeight() + "px，撑不出滚动",
                child.getMeasuredHeight() > 1704);
    }

    @Test
    public void 有返回入口且可点击() {
        PreviewActivity a = open().get();
        List<TextView> texts = new ArrayList<>();
        collect(a.findViewById(android.R.id.content), texts);
        boolean hasBack = false;
        for (TextView t : texts) {
            CharSequence c = t.getText();
            if (c != null && c.toString().contains("返回")) {
                hasBack = true;
                assertTrue("返回按钮应可点击", t.isClickable());
            }
        }
        assertTrue("找不到返回入口", hasBack);
    }

    /**
     * Robolectric 的 scrollTo 只改字段、不触发 onScrollChanged，
     * 所以这里直接触发"到底"回调，验证 UI 联动；到底的判定逻辑由 ScrollMath 的单测覆盖。
     */
    @Test
    public void 到底后按钮联动为重读() throws Exception {
        PreviewActivity a = laidOut();
        java.lang.reflect.Method m = PreviewActivity.class.getDeclaredMethod("onReachBottom");
        m.setAccessible(true);
        m.invoke(a);
        assertNotNull("到底后应出现「重读」", findText(a, "重读"));
    }

    @Test
    public void 点击速度可以就地切换() {
        PreviewActivity a = laidOut();
        TextView speed = null;
        List<TextView> all = new ArrayList<>();
        collect(a.findViewById(android.R.id.content), all);
        for (TextView t : all) {
            if (t.getText() != null && t.getText().toString().contains("秒/屏")) {
                speed = t;
                break;
            }
        }
        assertNotNull("顶栏应有速度显示", speed);

        speed.performClick();
        android.app.AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull("点击速度应弹出选择框", dialog);

        // 第一项是慢速 20 秒
        Shadows.shadowOf(dialog).clickOnItem(0);
        assertEquals("选完之后参数应更新", 20, Prefs.getScreenSeconds(a));
        assertTrue("顶栏文字应同步", containsText(a, "20 秒/屏"));
    }

    /** 打开并把布局真正跑一遍，否则子 View 没有高度，scrollTo 无从算起 */
    private PreviewActivity laidOut() {
        PreviewActivity a = open().get();
        View decor = a.getWindow().getDecorView();
        decor.measure(View.MeasureSpec.makeMeasureSpec(786, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1704, View.MeasureSpec.EXACTLY));
        decor.layout(0, 0, 786, 1704);
        return a;
    }

    private ScrollView findScrollView(PreviewActivity a) {
        List<ScrollView> list = new ArrayList<>();
        findScroll(a.findViewById(android.R.id.content), list);
        assertTrue("没有 ScrollView", !list.isEmpty());
        return list.get(0);
    }

    private boolean containsText(PreviewActivity a, String key) {
        List<TextView> texts = new ArrayList<>();
        collect(a.findViewById(android.R.id.content), texts);
        for (TextView t : texts) {
            if (t.getText() != null && t.getText().toString().contains(key)) {
                return true;
            }
        }
        return false;
    }

    private TextView findText(PreviewActivity a, String key) {
        List<TextView> texts = new ArrayList<>();
        collect(a.findViewById(android.R.id.content), texts);
        for (TextView t : texts) {
            if (t.getText() != null && t.getText().toString().equals(key)) {
                return t;
            }
        }
        return null;
    }

    private String allText(PreviewActivity a) {
        List<TextView> texts = new ArrayList<>();
        collect(a.findViewById(android.R.id.content), texts);
        StringBuilder sb = new StringBuilder();
        for (TextView t : texts) {
            if (t.getText() != null) {
                sb.append(t.getText()).append(" | ");
            }
        }
        return sb.toString();
    }

    private void findScroll(View v, List<ScrollView> out) {
        if (v instanceof ScrollView) {
            out.add((ScrollView) v);
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                findScroll(g.getChildAt(i), out);
            }
        }
    }

    private void collect(View v, List<TextView> out) {
        if (v instanceof TextView) {
            out.add((TextView) v);
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                collect(g.getChildAt(i), out);
            }
        }
    }
}
