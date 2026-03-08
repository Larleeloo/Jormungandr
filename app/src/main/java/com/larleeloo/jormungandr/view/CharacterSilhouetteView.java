package com.larleeloo.jormungandr.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/**
 * Draws a simple character silhouette outline as a visual guide
 * for dragging items to equip slots positioned around it.
 */
public class CharacterSilhouetteView extends View {

    private final Paint silhouettePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean highlightDropZone = false;

    public CharacterSilhouetteView(Context context) {
        super(context);
        init();
    }

    public CharacterSilhouetteView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CharacterSilhouetteView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        silhouettePaint.setColor(0x33FFD700); // Faint gold fill
        silhouettePaint.setStyle(Paint.Style.FILL);

        outlinePaint.setColor(0x66FFD700); // Semi-transparent gold outline
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(2f);
    }

    public void setHighlight(boolean highlight) {
        this.highlightDropZone = highlight;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;

        if (highlightDropZone) {
            silhouettePaint.setColor(0x55FFD700);
            outlinePaint.setColor(0xAAFFD700);
        } else {
            silhouettePaint.setColor(0x22FFD700);
            outlinePaint.setColor(0x44FFD700);
        }

        // Draw a simple humanoid silhouette
        Path path = new Path();

        // Head (circle)
        float headRadius = Math.min(w, h) * 0.07f;
        float headCy = h * 0.15f;
        canvas.drawCircle(cx, headCy, headRadius, silhouettePaint);
        canvas.drawCircle(cx, headCy, headRadius, outlinePaint);

        // Neck
        float neckTop = headCy + headRadius;
        float neckBottom = h * 0.22f;
        float neckHalfW = headRadius * 0.4f;
        canvas.drawRect(cx - neckHalfW, neckTop, cx + neckHalfW, neckBottom, silhouettePaint);

        // Torso (trapezoid)
        float torsoTop = neckBottom;
        float torsoBottom = h * 0.52f;
        float shoulderHalfW = w * 0.18f;
        float waistHalfW = w * 0.12f;

        path.reset();
        path.moveTo(cx - shoulderHalfW, torsoTop);
        path.lineTo(cx + shoulderHalfW, torsoTop);
        path.lineTo(cx + waistHalfW, torsoBottom);
        path.lineTo(cx - waistHalfW, torsoBottom);
        path.close();
        canvas.drawPath(path, silhouettePaint);
        canvas.drawPath(path, outlinePaint);

        // Arms
        float armWidth = w * 0.04f;
        float armTop = torsoTop + h * 0.02f;
        float armBottom = h * 0.48f;

        // Left arm
        canvas.drawRect(cx - shoulderHalfW - armWidth, armTop,
                cx - shoulderHalfW, armBottom, silhouettePaint);
        canvas.drawRect(cx - shoulderHalfW - armWidth, armTop,
                cx - shoulderHalfW, armBottom, outlinePaint);

        // Right arm
        canvas.drawRect(cx + shoulderHalfW, armTop,
                cx + shoulderHalfW + armWidth, armBottom, silhouettePaint);
        canvas.drawRect(cx + shoulderHalfW, armTop,
                cx + shoulderHalfW + armWidth, armBottom, outlinePaint);

        // Legs
        float legTop = torsoBottom;
        float legBottom = h * 0.85f;
        float legHalfW = waistHalfW * 0.4f;
        float legGap = w * 0.02f;

        // Left leg
        canvas.drawRect(cx - waistHalfW + legGap, legTop,
                cx - legGap, legBottom, silhouettePaint);
        canvas.drawRect(cx - waistHalfW + legGap, legTop,
                cx - legGap, legBottom, outlinePaint);

        // Right leg
        canvas.drawRect(cx + legGap, legTop,
                cx + waistHalfW - legGap, legBottom, silhouettePaint);
        canvas.drawRect(cx + legGap, legTop,
                cx + waistHalfW - legGap, legBottom, outlinePaint);

        // Feet
        float footHeight = h * 0.04f;
        float footExtraW = w * 0.02f;
        canvas.drawRect(cx - waistHalfW + legGap - footExtraW, legBottom,
                cx - legGap + footExtraW, legBottom + footHeight, silhouettePaint);
        canvas.drawRect(cx + legGap - footExtraW, legBottom,
                cx + waistHalfW - legGap + footExtraW, legBottom + footHeight, silhouettePaint);
    }
}
