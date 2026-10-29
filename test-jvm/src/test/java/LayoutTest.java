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

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * 布局几何检查。Robolectric 的测量/布局是纯 Java 实现，能验证尺寸真的算对了，
 * 而不只是"控件存在"。分辨率固定为 393x852dp @ xhdpi（786x1704px）。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, qualifiers = "w393dp-h852dp-xhdpi")
public class LayoutTest {

    private static final int W = 786;
    private static final int H = 1704;
    private static final int PAGE_PAD = 40;   // 页面左右各 20dp，@xhdpi = 40px
    private static final int CARD_W = W - PAGE_PAD * 2;  // 706

    private View decor() {
        ActivityController<MainActivity> controller =
                Robolectric.buildActivity(MainActivity.class).setup();
        View d = controller.get().getWindow().getDecorView();
        d.measure(View.MeasureSpec.makeMeasureSpec(W, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(H, View.MeasureSpec.EXACTLY));
        d.layout(0, 0, W, H);
        return d;
    }

    @Test
    public void 主按钮尺寸符合设计要求() {
        View d = decor();
        // 默认测试环境无障碍未启用，主按钮显示引导文案
        TextView label = findByText(d, "启用无障碍服务");
        assertNotNull("找不到主按钮文字", label);

        View btn = (View) label.getParent();
        assertEquals("主按钮高度应为 56dp", 112, btn.getHeight());
        assertEquals("主按钮宽度应撑满内容区", CARD_W, btn.getWidth());
    }

    @Test
    public void 卡片宽度与内边距一致() {
        View d = decor();
        TextView title = findByText(d, "滚动速度");
        assertNotNull(title);
        View card = (View) title.getParent().getParent();
        assertEquals("卡片宽度应为屏宽减去两侧 20dp", CARD_W, card.getWidth());

        View appTitle = findByText(d, "应用范围");
        View appCard = (View) appTitle.getParent().getParent();
        assertEquals(CARD_W, appCard.getWidth());
    }

    /**
     * 注意：Robolectric 没有真实字体，文本的宽高测量值不可信（宽度基本为 0），
     * 但 top/bottom 这类由布局计算出的坐标是准确的。所以这里用纵向占位来检查排版密度。
     */
    @Test
    public void 内容填满屏幕不留大片空白() {
        View d = decor();
        int maxBottom = 0;
        int minTop = Integer.MAX_VALUE;
        for (TextView t : allTextViews(d)) {
            // isShown 会连带检查所有祖先的可见性，折叠区里的控件不会被算进来
            if (!t.isShown()) {
                continue;
            }
            CharSequence c = t.getText();
            if (c == null || c.length() == 0) {
                continue;
            }
            maxBottom = Math.max(maxBottom, bottomInWindow(t));
            minTop = Math.min(minTop, topInWindow(t));
        }
        assertTrue("内容底部只到 " + maxBottom + "px，屏幕 " + H + "px，底部空白过多",
                maxBottom > H * 0.85);
        // 内容多于屏幕是可滚动的正常情况，只要不过分就行
        assertTrue("内容超出屏幕过多: " + maxBottom, maxBottom <= (int) (H * 1.3));
        assertTrue("顶部起始位置异常: " + minTop, minTop >= 0 && minTop < 160);
    }

    @Test
    public void 纵向没有元素重叠() {
        View d = decor();
        List<TextView> list = allTextViews(d);
        List<String> overlaps = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            TextView a = list.get(i);
            if (!a.isShown()) {
                continue;
            }
            int aTop = topInWindow(a);
            int aBottom = bottomInWindow(a);
            for (int j = i + 1; j < list.size(); j++) {
                TextView b = list.get(j);
                if (!b.isShown()) {
                    continue;
                }
                int bTop = topInWindow(b);
                int bBottom = bottomInWindow(b);
                // 只有同一列（横向也有交集）才算真重叠。必须用窗口坐标，
                // getLeft() 是相对父容器的，不同卡片里都从 0 开始，会误判成同列。
                boolean sameCol = leftInWindow(a) < rightInWindow(b)
                        && leftInWindow(b) < rightInWindow(a);
                if (sameCol && aTop < bBottom - 2 && bTop < aBottom - 2) {
                    overlaps.add("[" + a.getText() + "] 与 [" + b.getText() + "]");
                }
            }
        }
        assertTrue("存在重叠: " + overlaps, overlaps.isEmpty());
    }

    @Test
    public void 打印所有文本控件尺寸() {
        View d = decor();
        List<TextView> all = new ArrayList<>();
        collect(d, all);
        System.out.println("---- 文本控件清单 ----");
        for (TextView t : all) {
            CharSequence c = t.getText();
            System.out.println(String.format(java.util.Locale.US,
                    "[%s] size=%.1f w=%d h=%d x=%d y=%d",
                    c == null ? "" : c.toString(), t.getTextSize(),
                    t.getWidth(), t.getHeight(), leftInWindow(t), topInWindow(t)));
        }
    }

    private int leftInWindow(View v) {
        int x = 0;
        while (v != null) {
            x += v.getLeft();
            if (v.getParent() instanceof View) {
                v = (View) v.getParent();
            } else {
                break;
            }
        }
        return x;
    }

    private int topInWindow(View v) {
        int y = 0;
        while (v != null) {
            y += v.getTop();
            if (v.getParent() instanceof View) {
                v = (View) v.getParent();
            } else {
                break;
            }
        }
        return y;
    }

    @Test
    public void 没有任何控件横向溢出() {
        View d = decor();
        List<String> bad = new ArrayList<>();
        checkOverflow(d, bad);
        assertTrue("存在溢出控件: " + bad, bad.isEmpty());
    }

    @Test
    public void 内容高度在合理范围() {
        View d = decor();
        View content = d.findViewById(android.R.id.content);
        assertNotNull(content);
        assertTrue("内容区高度异常", content.getHeight() > 0 && content.getHeight() <= H);
    }

    /** 子控件的右边界不能超过父容器的可用宽度 */
    private void checkOverflow(View v, List<String> bad) {
        if (!(v instanceof ViewGroup)) {
            return;
        }
        ViewGroup g = (ViewGroup) v;
        int padRight = g.getPaddingRight();
        for (int i = 0; i < g.getChildCount(); i++) {
            View child = g.getChildAt(i);
            if (child.getVisibility() == View.GONE) {
                continue;
            }
            if (child.getRight() > g.getWidth() - padRight + 1) {
                bad.add(describe(child) + " right=" + child.getRight()
                        + " > parent=" + g.getWidth());
            }
            checkOverflow(child, bad);
        }
    }

    private String describe(View v) {
        if (v instanceof TextView) {
            CharSequence t = ((TextView) v).getText();
            return v.getClass().getSimpleName() + "[" + (t == null ? "" : t) + "]";
        }
        return v.getClass().getSimpleName();
    }

    private int bottomInWindow(View v) {
        return topInWindow(v) + v.getHeight();
    }

    private int rightInWindow(View v) {
        return leftInWindow(v) + v.getWidth();
    }

    private List<TextView> allTextViews(View root) {
        List<TextView> out = new ArrayList<>();
        collect(root, out);
        return out;
    }

    private TextView findByText(View root, String key) {
        List<TextView> all = new ArrayList<>();
        collect(root, all);
        for (TextView t : all) {
            CharSequence c = t.getText();
            if (c != null && c.toString().contains(key)) {
                return t;
            }
        }
        return null;
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
