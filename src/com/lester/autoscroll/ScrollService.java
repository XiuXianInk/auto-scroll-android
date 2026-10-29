package com.lester.autoscroll;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Path;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

/**
 * 核心：无障碍服务，用匀速手势链模拟手指持续上滑。
 *
 * 顺滑的三个要点：
 * 1) 每段位移固定为约 1/4 屏，段时长由速度反推 —— 速度恒定，不会忽快忽慢；
 * 2) 上一段 onCompleted 里立刻发下一段，中间不等，视觉上不断线；
 * 3) 段长随速度自适应 —— 慢速时接缝的影响（丢失约 1 帧的位移）小到看不出来，
 *    快速时才用更长的段来减少接缝次数。
 */
public class ScrollService extends AccessibilityService {

    private static final String TAG = "AutoScroll";

    public static volatile ScrollService instance;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private volatile boolean running = false;
    private volatile String currentPkg = "";
    private volatile boolean screenOn = true;

    /** 是否有一次手势正在执行，防止重复下发导致速度翻倍 */
    private volatile boolean inFlight = false;
    /** 当前这一段手势，用来识别迟到/过期的回调 */
    private volatile GestureDescription currentGesture;
    private long lastDispatchAt = 0;
    private long lastChunkMs = 0;
    private int retryCount = 0;

    /** 连续滚动上限。滚动期间屏幕常亮，用户睡过去也不会一直烧着（烧屏 + 耗电） */
    private static final long MAX_RUN_MS = 60 * 60 * 1000L;
    private long runStartAt = 0;

    /** 是否处于预览页。预览页是本应用的 Activity，平时会被排除，需要放行 */
    public volatile boolean previewMode = false;

    private FloatPanel panel;
    private BroadcastReceiver screenReceiver;
    /** 滚动期间把屏幕按住不灭，否则长时间看小说会被系统超时熄屏打断 */
    private PowerManager.WakeLock wakeLock;

    private final AccessibilityService.GestureResultCallback chunkCallback =
            new AccessibilityService.GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    // 只有"当前这一段"的回调才作数。停止后立刻重启时，
                    // 上一段的回调可能迟到，若照单全收会清掉新段的 inFlight，
                    // 结果两段同时下发、速度翻倍。
                    if (gestureDescription != currentGesture) {
                        return;
                    }
                    inFlight = false;
                    retryCount = 0;
                    // 不要在这个回调栈里直接注入下一段：手势刚结束的一瞬间资源可能还没释放，
                    // 立刻注入容易被拒。post 到队列末尾，代价是一帧，换来衔接成功率。
                    handler.removeCallbacks(pumpTask);
                    handler.post(pumpTask);
                }

                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    // 被系统或用户打断（来电、切应用、手指按下），稍等一下再续，别硬顶
                    if (gestureDescription != currentGesture) {
                        return;
                    }
                    inFlight = false;
                    handler.removeCallbacks(pumpTask);
                    handler.postDelayed(pumpTask, 150);
                }
            };

    private final Runnable pumpTask = new Runnable() {
        @Override
        public void run() {
            pump();
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.i(TAG, "无障碍服务已连接");

        screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String a = intent.getAction();
                if (Intent.ACTION_SCREEN_OFF.equals(a)) {
                    screenOn = false;
                } else if (Intent.ACTION_SCREEN_ON.equals(a) || Intent.ACTION_USER_PRESENT.equals(a)) {
                    screenOn = true;
                }
            }
        };
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_SCREEN_OFF);
        f.addAction(Intent.ACTION_SCREEN_ON);
        f.addAction(Intent.ACTION_USER_PRESENT);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(screenReceiver, f, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(screenReceiver, f);
        }

        refreshPanel();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return;
        }
        CharSequence p = event.getPackageName();
        if (p != null && p.length() > 0) {
            currentPkg = p.toString();
        }
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public boolean onUnbind(Intent intent) {
        cleanup();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        cleanup();
        super.onDestroy();
    }

    private void cleanup() {
        running = false;
        inFlight = false;
        releaseWake();
        handler.removeCallbacksAndMessages(null);
        if (panel != null) {
            panel.remove();
            panel = null;
        }
        if (screenReceiver != null) {
            try {
                unregisterReceiver(screenReceiver);
            } catch (Exception ignored) {
            }
            screenReceiver = null;
        }
        if (instance == this) {
            instance = null;
        }
    }

    // ---------- 对外接口 ----------

    public boolean isRunning() {
        return running;
    }

    public String getCurrentPkg() {
        return currentPkg;
    }

    public void setPreviewMode(boolean on) {
        previewMode = on;
    }

    public void startScroll() {
        if (running) {
            return;
        }
        running = true;
        retryCount = 0;
        runStartAt = System.currentTimeMillis();
        acquireWake();
        handler.removeCallbacks(pumpTask);
        pump();
        if (panel != null) {
            panel.setRunning(true);
        }
    }

    /**
     * 停止：不再续下一段。正在走的那一段会自然走完（最长 CHUNK_MAX_MS），
     * 这样不会因为硬切而产生跳动。
     */
    public void stopScroll() {
        running = false;
        handler.removeCallbacks(pumpTask);
        releaseWake();
        if (panel != null) {
            panel.setRunning(false);
        }
    }

    public void toggle() {
        if (running) {
            stopScroll();
        } else {
            startScroll();
        }
    }

    /** 试滚：滚一大段，让他看清方向和幅度 */
    public void demoSwipe() {
        if (inFlight) {
            Toast.makeText(this, "滚动进行中，请先暂停", Toast.LENGTH_SHORT).show();
            return;
        }
        int[] size = screenSize();
        if (size == null) {
            return;
        }
        float x = size[0] * 0.5f;
        float y1 = size[1] * ScrollMath.START_RATIO;
        float y2 = size[1] * 0.30f;

        Path path = new Path();
        path.moveTo(x, y1);
        path.lineTo(x, y2);
        GestureDescription gd = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0, 1400))
                .build();
        try {
            dispatchGesture(gd, null, null);
        } catch (Exception e) {
            Log.w(TAG, "试滚失败", e);
        }
    }

    public void refreshPanel() {
        if (!Settings.canDrawOverlays(this)) {
            if (panel != null) {
                panel.remove();
                panel = null;
            }
            return;
        }
        if (panel == null) {
            panel = new FloatPanel(this);
            panel.show(running);
        }
    }

    // ---------- 屏幕常亮 ----------

    /** 常亮最长挂 10 分钟，滚动中自动续期。进程若被系统杀掉，超时到期也会自动释放，不会一直亮着耗电 */
    private static final long WAKE_TIMEOUT_MS = 10 * 60 * 1000L;
    private long lastWakeRefreshAt = 0;

    private void acquireWake() {
        try {
            if (wakeLock == null) {
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "AutoScroll:scroll");
                wakeLock.setReferenceCounted(false);
            }
            wakeLock.acquire(WAKE_TIMEOUT_MS);
            lastWakeRefreshAt = System.currentTimeMillis();
        } catch (Exception e) {
            Log.w(TAG, "请求屏幕常亮失败", e);
        }
    }

    /** 每分钟续期一次就够了 */
    private void refreshWake() {
        if (System.currentTimeMillis() - lastWakeRefreshAt < 60_000L) {
            return;
        }
        acquireWake();
    }

    private void releaseWake() {
        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        } catch (Exception e) {
            Log.w(TAG, "释放屏幕常亮失败", e);
        }
        // 清零刷新时间戳，否则 60 秒节流会让下次重新获取被跳过，屏幕就白灭了
        lastWakeRefreshAt = 0;
    }

    // ---------- 主循环 ----------

    private void pump() {
        if (!running) {
            inFlight = false;
            return;
        }
        // 连续滚动到上限就自己停下，避免用户睡着后长时间亮屏空转
        if (runStartAt > 0 && System.currentTimeMillis() - runStartAt > MAX_RUN_MS) {
            stopScroll();
            try {
                Toast.makeText(this, "已连续滚动 60 分钟，自动停止", Toast.LENGTH_LONG).show();
            } catch (Exception ignored) {
            }
            return;
        }
        if (!screenOn || !canScrollHere()) {
            // 熄屏或不在该滚的界面：停住但保持运行态，回到目标应用自动续上。
            // 同时把屏幕常亮放掉——人在别的应用里，没必要替他亮着屏耗电。
            inFlight = false;
            releaseWake();
            handler.removeCallbacks(pumpTask);
            handler.postDelayed(pumpTask, 500);
            return;
        }
        // 上一段还在跑：等它。但回调万一丢了必须自愈，否则整条链会卡死。
        // 余量给 600ms 而不是更长——手势时长本就是确定的，超出说明回调已经丢了。
        if (inFlight && System.currentTimeMillis() - lastDispatchAt < lastChunkMs + 600) {
            handler.removeCallbacks(pumpTask);
            handler.postDelayed(pumpTask, 40);
            return;
        }
        inFlight = false;
        scrollChunk();
    }

    private void scrollChunk() {
        int[] size = screenSize();
        if (size == null) {
            return;
        }
        int w = size[0];
        int h = size[1];

        int secs = Prefs.getScreenSeconds(this);
        ScrollMath.Chunk chunk = ScrollMath.compute(
                secs, h, getResources().getDisplayMetrics().density);

        float x = w * 0.5f;
        Path path = new Path();
        path.moveTo(x, chunk.startY);
        path.lineTo(x, chunk.endY);

        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0, chunk.chunkMs))
                .build();

        boolean accepted;
        try {
            currentGesture = gesture;
            accepted = dispatchGesture(gesture, chunkCallback, null);
        } catch (Exception e) {
            Log.w(TAG, "发送手势失败", e);
            accepted = false;
        }

        if (accepted) {
            inFlight = true;
            lastDispatchAt = System.currentTimeMillis();
            lastChunkMs = chunk.chunkMs;
            refreshWake();
        } else {
            // 退避重试，用帧级延迟而不是固定 200ms，避免肉眼可见的卡顿
            inFlight = false;
            scheduleRetry();
        }
    }

    private void scheduleRetry() {
        if (!running) {
            return;
        }
        int delay = ScrollMath.retryDelayMs(retryCount);
        // 连续被拒这么多次，基本可以断定用户正按着屏幕手动操作。
        // 这时候要明显让路：一秒试一次足够，既不打断用户，松手后也能很快续上。
        if (retryCount >= ScrollMath.RETRY_YIELD_THRESHOLD) {
            delay = Math.max(delay, 1000);
        }
        retryCount++;
        handler.removeCallbacks(pumpTask);
        handler.postDelayed(pumpTask, delay);
    }

    private int[] screenSize() {
        try {
            WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            Point size = new Point();
            wm.getDefaultDisplay().getRealSize(size);
            if (size.x <= 0 || size.y <= 0) {
                return null;
            }
            return new int[]{size.x, size.y};
        } catch (Exception e) {
            Log.w(TAG, "取屏幕尺寸失败", e);
            return null;
        }
    }

    /** 当前是否该滚：排除自己、通知栏、以及设了限定应用时的其它应用 */
    private boolean canScrollHere() {
        return ScrollPolicy.shouldScroll(currentPkg, getPackageName(),
                Prefs.getTargetPkg(this), previewMode);
    }
}
