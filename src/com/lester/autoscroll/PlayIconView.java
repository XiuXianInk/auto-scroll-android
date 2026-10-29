package com.lester.autoscroll;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/**
 * 主按钮里的播放三角。用代码画而不是打字符，
 * 因为 ▶ 这类符号在不同厂商字体下大小和基线都不一样。
 */
@SuppressLint("ViewConstructor")
public class PlayIconView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final int boxW;
    private final int boxH;

    public PlayIconView(Context c, float wDp, float hDp, int color) {
        super(c);
        this.boxW = Math.max(1, UiKit.dp(c, wDp));
        this.boxH = Math.max(1, UiKit.dp(c, hDp));
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(boxW, boxH);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        path.reset();
        path.moveTo(0f, 0f);
        path.lineTo(w, h / 2f);
        path.lineTo(0f, h);
        path.close();
        canvas.drawPath(path, paint);
    }
}
