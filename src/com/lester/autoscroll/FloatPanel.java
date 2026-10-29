package com.lester.autoscroll;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 屏幕上的小圆球：只负责开始/暂停，按住可拖动位置。
 * 触摸事件由本类全权处理，点击走 View 的 performClick 通路，无障碍语义不丢。
 */
public class FloatPanel {

    /** 滚动中闲置多久开始淡出。太短会显得球在闪，太长又挡字 */
    private static final long FADE_DELAY_MS = 3500;
    /** 淡出后的透明度：还能看见，但不抢视线 */
    private static final float FADE_ALPHA = 0.30f;

    private final Context ctx;
    private final WindowManager wm;
    private final TextView ball;
    private WindowManager.LayoutParams lp;
    private final float density;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final Runnable fadeTask = new Runnable() {
        @Override
        public void run() {
            if (running && ball != null) {
                ball.animate().cancel();
                ball.animate().alpha(FADE_ALPHA).setDuration(450).start();
            }
        }
    };

    private int startX;
    private int startY;
    private int ballSize;
    private int screenW;
    private int screenH;
    private float downRawX;
    private float downRawY;
    private boolean moved;
    private boolean running;

    // 触摸由本类全权处理（拖动 + 点击），点击路径已手动调 performClick，符合无障碍语义
    @SuppressLint("ClickableViewAccessibility")
    public FloatPanel(Context ctx) {
        this.ctx = ctx;
        this.wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        this.density = ctx.getResources().getDisplayMetrics().density;

        int size = Math.round(54 * density);
        int touchSlop = Math.round(12 * density);

        ball = new TextView(ctx);
        ball.setText("▶");
        ball.setTextColor(Color.WHITE);
        ball.setTextSize(16);
        ball.setGravity(Gravity.CENTER);
        ball.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        ball.setBackground(makeBg(false));

        // minSdk 26，直接可用悬浮窗类型
        int type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

        lp = new WindowManager.LayoutParams(
                size, size, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;

        // 放在右侧偏下，不挡正文
        Point screen = new Point();
        try {
            wm.getDefaultDisplay().getRealSize(screen);
        } catch (Exception ignored) {
        }
        this.ballSize = size;
        this.screenW = screen.x;
        this.screenH = screen.y;

        int savedX = Prefs.getBallXRatio(ctx);
        int savedY = Prefs.getBallYRatio(ctx);
        if (savedX >= 0 && savedY >= 0 && screen.x > size && screen.y > size) {
            // 用户拖过就沿用他的位置，否则每次重启又挡回去，白拖了
            lp.x = clamp((int) (screen.x * savedX / 1000f), 0, screen.x - size);
            lp.y = clamp((int) (screen.y * savedY / 1000f), 0, screen.y - size);
        } else {
            lp.x = screen.x > 0 ? screen.x - size - Math.round(6 * density) : Math.round(10 * density);
            lp.y = screen.y > 0 ? (int) (screen.y * 0.62f) : 400;
        }

        final int slop = touchSlop;
        ball.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        // 一碰就把球唤回不透明，别让用户对着一团半透明的东西犹豫
                        wakeUp();
                        startX = lp.x;
                        startY = lp.y;
                        downRawX = e.getRawX();
                        downRawY = e.getRawY();
                        moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE: {
                        float dx = e.getRawX() - downRawX;
                        float dy = e.getRawY() - downRawY;
                        if (!moved && (Math.abs(dx) > slop || Math.abs(dy) > slop)) {
                            moved = true;
                        }
                        if (moved) {
                            lp.x = clamp(startX + (int) dx, 0, screenW - ballSize);
                            lp.y = clamp(startY + (int) dy, 0, screenH - ballSize);
                            safeUpdate();
                        }
                        return true;
                    }
                    case MotionEvent.ACTION_UP:
                        if (!moved) {
                            // 走 View 的标准点击通路，TalkBack 也能激活
                            ball.performClick();
                        } else {
                            // 拖完就记住位置
                            savePosition();
                        }
                        // 松手后重新开始计时淡出
                        scheduleFade();
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        scheduleFade();
                        return true;
                    default:
                        return false;
                }
            }
        });

        ball.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performToggle();
            }
        });
    }

    public void show(boolean running) {
        setRunning(running);
        try {
            wm.addView(ball, lp);
            // 用法提示只在第一次出现，之后每次滚动都弹就成了打扰
            if (!Prefs.isFloatHintShown(ctx)) {
                Prefs.setFloatHintShown(ctx, true);
                ball.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Toast.makeText(ctx, "单击开始或暂停，按住可拖动位置",
                                    Toast.LENGTH_SHORT).show();
                        } catch (Exception ignored) {
                        }
                    }
                }, 900);
            }
        } catch (Exception ignored) {
        }
    }

    private GradientDrawable makeBg(boolean running) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(running ? 0xFFFF4B3E : 0xE6323232);
        if (!running) {
            g.setStroke(Math.round(1.5f * density), 0x55FFFFFF);
        }
        return g;
    }

    private void performToggle() {
        ScrollService s = ScrollService.instance;
        if (s == null) {
            try {
                Toast.makeText(ctx, "服务未连接，请重新打开应用", Toast.LENGTH_SHORT).show();
            } catch (Exception ignored) {
            }
            return;
        }
        s.toggle();
        setRunning(s.isRunning());
    }

    public void setRunning(boolean r) {
        running = r;
        ball.setBackground(makeBg(r));
        // 用字体一定支持的字符，避免部分机型显示成方块
        ball.setText(r ? "II" : "▶");
        if (r) {
            scheduleFade();
        } else {
            cancelFade();
        }
    }

    /** 拖完把位置存成相对比例，换屏幕也能落回原来的相对位置 */
    private void savePosition() {
        if (screenW <= ballSize || screenH <= ballSize) {
            return;
        }
        int xr = clamp((int) (lp.x * 1000L / screenW), 0, 1000);
        int yr = clamp((int) (lp.y * 1000L / screenH), 0, 1000);
        Prefs.setBallPos(ctx, xr, yr);
    }

    /** 触摸时立刻恢复不透明，并停掉正在进行的淡出动画 */
    private void wakeUp() {
        ui.removeCallbacks(fadeTask);
        ball.animate().cancel();
        ball.setAlpha(1.0f);
    }

    private void scheduleFade() {
        ui.removeCallbacks(fadeTask);
        if (running) {
            ui.postDelayed(fadeTask, FADE_DELAY_MS);
        }
    }

    /** 停止滚动时球要保持清晰——这时候用户可能要来点它 */
    private void cancelFade() {
        ui.removeCallbacks(fadeTask);
        ball.animate().cancel();
        ball.setAlpha(1.0f);
    }


    private void safeUpdate() {
        try {
            wm.updateViewLayout(ball, lp);
        } catch (Exception ignored) {
        }
    }

    private int clamp(int v, int lo, int hi) {
        if (hi < lo) {
            return lo;
        }
        return v < lo ? lo : (v > hi ? hi : v);
    }

    public void remove() {
        try {
            ui.removeCallbacks(fadeTask);
            wm.removeView(ball);
        } catch (Exception ignored) {
        }
    }
}
