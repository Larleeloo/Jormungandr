package com.larleeloo.jormungandr.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class HpBarView extends View {
    private int currentHp = 100;
    private int maxHp = 100;
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public HpBarView(Context context) { super(context); init(); }
    public HpBarView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public HpBarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr); init();
    }

    private void init() {
        bgPaint.setColor(0xFF333333);
        borderPaint.setColor(0xFF666666);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(20f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setShadowLayer(2f, 1f, 1f, Color.BLACK);
    }

    public void setHp(int current, int max) {
        this.currentHp = current;
        this.maxHp = max;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float radius = h / 4;

        // Background
        RectF rect = new RectF(0, 0, w, h);
        canvas.drawRoundRect(rect, radius, radius, bgPaint);

        // Fill
        float ratio = maxHp > 0 ? (float) currentHp / maxHp : 0;
        fillPaint.setColor(getHpColor(ratio));
        RectF fillRect = new RectF(0, 0, w * ratio, h);
        canvas.drawRoundRect(fillRect, radius, radius, fillPaint);

        // Border
        canvas.drawRoundRect(rect, radius, radius, borderPaint);

        // Text
        textPaint.setTextSize(h * 0.6f);
        canvas.drawText(currentHp + "/" + maxHp, w / 2, h * 0.7f, textPaint);
    }

    private int getHpColor(float ratio) {
        if (ratio > 0.5f) return Color.rgb((int)(255 * (1 - ratio) * 2), 255, 0);
        return Color.rgb(255, (int)(255 * ratio * 2), 0);
    }
}
