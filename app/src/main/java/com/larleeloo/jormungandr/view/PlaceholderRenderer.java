package com.larleeloo.jormungandr.view;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/**
 * Draws colored placeholder shapes for items, creatures, doors, and other objects
 * when actual sprite assets are not yet available.
 */
public final class PlaceholderRenderer {
    private static final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    static {
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(2f);
        outlinePaint.setColor(Color.BLACK);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    private PlaceholderRenderer() {}

    public static void drawShape(Canvas canvas, String shape, int color, float x, float y,
                                  float width, float height) {
        if (shape == null) shape = "rectangle";
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);

        switch (shape.toLowerCase()) {
            case "sword":
                drawSword(canvas, color, x, y, width, height);
                break;
            case "axe":
                drawAxe(canvas, color, x, y, width, height);
                break;
            case "bow":
                drawBow(canvas, color, x, y, width, height);
                break;
            case "staff":
                drawStaff(canvas, color, x, y, width, height);
                break;
            case "shield":
                drawShield(canvas, color, x, y, width, height);
                break;
            case "potion":
                drawPotion(canvas, color, x, y, width, height);
                break;
            case "scroll":
                drawScroll(canvas, color, x, y, width, height);
                break;
            case "food":
                drawFood(canvas, color, x, y, width, height);
                break;
            case "key":
                drawKey(canvas, color, x, y, width, height);
                break;
            case "armor":
                drawArmor(canvas, color, x, y, width, height);
                break;
            case "hat":
                drawHat(canvas, color, x, y, width, height);
                break;
            case "boots":
                drawBoots(canvas, color, x, y, width, height);
                break;
            case "ring":
            case "accessory":
                drawRing(canvas, color, x, y, width, height);
                break;
            case "gem":
                drawGem(canvas, color, x, y, width, height);
                break;
            case "chest":
                drawChest(canvas, color, x, y, width, height);
                break;
            case "creature":
                drawCreature(canvas, color, x, y, width, height);
                break;
            case "door":
                drawDoor(canvas, color, x, y, width, height);
                break;
            case "trap":
                drawTrap(canvas, color, x, y, width, height);
                break;
            case "circle":
                canvas.drawOval(new RectF(x, y, x + width, y + height), paint);
                canvas.drawOval(new RectF(x, y, x + width, y + height), outlinePaint);
                break;
            case "material":
                drawMaterial(canvas, color, x, y, width, height);
                break;
            default:
                canvas.drawRect(x, y, x + width, y + height, paint);
                canvas.drawRect(x, y, x + width, y + height, outlinePaint);
                break;
        }
    }

    private static void drawSword(Canvas canvas, int color, float x, float y,
                                   float w, float h) {
        paint.setColor(color);
        // Blade
        float bladeW = w * 0.2f;
        float bladeH = h * 0.7f;
        float cx = x + w / 2;
        canvas.drawRect(cx - bladeW / 2, y, cx + bladeW / 2, y + bladeH, paint);
        // Crossguard
        float guardW = w * 0.7f;
        float guardH = h * 0.08f;
        canvas.drawRect(cx - guardW / 2, y + bladeH, cx + guardW / 2, y + bladeH + guardH, paint);
        // Handle
        float handleW = w * 0.15f;
        float handleH = h * 0.2f;
        paint.setColor(darken(color, 0.6f));
        canvas.drawRect(cx - handleW / 2, y + bladeH + guardH, cx + handleW / 2, y + h, paint);
        // Outline
        paint.setColor(color);
        canvas.drawRect(cx - bladeW / 2, y, cx + bladeW / 2, y + bladeH, outlinePaint);
    }

    private static void drawAxe(Canvas canvas, int color, float x, float y,
                                  float w, float h) {
        paint.setColor(darken(color, 0.6f));
        // Handle
        float cx = x + w / 2;
        canvas.drawRect(cx - w * 0.08f, y + h * 0.3f, cx + w * 0.08f, y + h, paint);
        // Head
        paint.setColor(color);
        Path axeHead = new Path();
        axeHead.moveTo(cx - w * 0.08f, y + h * 0.05f);
        axeHead.lineTo(cx + w * 0.45f, y + h * 0.15f);
        axeHead.lineTo(cx + w * 0.45f, y + h * 0.35f);
        axeHead.lineTo(cx - w * 0.08f, y + h * 0.3f);
        axeHead.close();
        canvas.drawPath(axeHead, paint);
        canvas.drawPath(axeHead, outlinePaint);
    }

    private static void drawBow(Canvas canvas, int color, float x, float y,
                                  float w, float h) {
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(w * 0.1f);
        RectF arcRect = new RectF(x + w * 0.2f, y, x + w * 0.9f, y + h);
        canvas.drawArc(arcRect, 120, 120, false, paint);
        paint.setStyle(Paint.Style.FILL);
        // String
        Paint stringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        stringPaint.setColor(Color.WHITE);
        stringPaint.setStrokeWidth(2f);
        float topY = y + h * 0.13f;
        float botY = y + h * 0.87f;
        float strX = x + w * 0.3f;
        canvas.drawLine(strX, topY, strX, botY, stringPaint);
    }

    private static void drawStaff(Canvas canvas, int color, float x, float y,
                                    float w, float h) {
        paint.setColor(darken(color, 0.5f));
        float cx = x + w / 2;
        canvas.drawRect(cx - w * 0.08f, y + h * 0.2f, cx + w * 0.08f, y + h, paint);
        // Orb on top
        paint.setColor(color);
        canvas.drawCircle(cx, y + h * 0.15f, w * 0.2f, paint);
        canvas.drawCircle(cx, y + h * 0.15f, w * 0.2f, outlinePaint);
    }

    private static void drawShield(Canvas canvas, int color, float x, float y,
                                     float w, float h) {
        paint.setColor(color);
        Path shield = new Path();
        float cx = x + w / 2;
        shield.moveTo(cx, y);
        shield.lineTo(x + w, y + h * 0.15f);
        shield.lineTo(x + w, y + h * 0.55f);
        shield.lineTo(cx, y + h);
        shield.lineTo(x, y + h * 0.55f);
        shield.lineTo(x, y + h * 0.15f);
        shield.close();
        canvas.drawPath(shield, paint);
        canvas.drawPath(shield, outlinePaint);
        // Cross emblem
        paint.setColor(darken(color, 0.7f));
        canvas.drawRect(cx - w * 0.05f, y + h * 0.2f, cx + w * 0.05f, y + h * 0.7f, paint);
        canvas.drawRect(cx - w * 0.2f, y + h * 0.35f, cx + w * 0.2f, y + h * 0.45f, paint);
    }

    private static void drawPotion(Canvas canvas, int color, float x, float y,
                                     float w, float h) {
        // Bottle body
        paint.setColor(color);
        RectF body = new RectF(x + w * 0.15f, y + h * 0.35f, x + w * 0.85f, y + h * 0.9f);
        canvas.drawRoundRect(body, w * 0.15f, h * 0.1f, paint);
        canvas.drawRoundRect(body, w * 0.15f, h * 0.1f, outlinePaint);
        // Neck
        paint.setColor(darken(color, 0.8f));
        canvas.drawRect(x + w * 0.35f, y + h * 0.1f, x + w * 0.65f, y + h * 0.4f, paint);
        // Cork
        paint.setColor(0xFF8B4513);
        canvas.drawRect(x + w * 0.3f, y, x + w * 0.7f, y + h * 0.12f, paint);
    }

    private static void drawScroll(Canvas canvas, int color, float x, float y,
                                     float w, float h) {
        paint.setColor(0xFFF5DEB3); // wheat color for parchment
        canvas.drawRect(x + w * 0.1f, y + h * 0.1f, x + w * 0.9f, y + h * 0.9f, paint);
        // Scroll rolls
        paint.setColor(color);
        canvas.drawCircle(x + w * 0.1f, y + h * 0.1f, w * 0.1f, paint);
        canvas.drawCircle(x + w * 0.9f, y + h * 0.1f, w * 0.1f, paint);
        canvas.drawCircle(x + w * 0.1f, y + h * 0.9f, w * 0.1f, paint);
        canvas.drawCircle(x + w * 0.9f, y + h * 0.9f, w * 0.1f, paint);
        // Text lines
        paint.setColor(Color.DKGRAY);
        for (float ly = 0.3f; ly < 0.8f; ly += 0.12f) {
            canvas.drawLine(x + w * 0.2f, y + h * ly, x + w * 0.8f, y + h * ly, paint);
        }
    }

    private static void drawFood(Canvas canvas, int color, float x, float y,
                                   float w, float h) {
        paint.setColor(color);
        canvas.drawOval(new RectF(x + w * 0.1f, y + h * 0.2f, x + w * 0.9f, y + h * 0.8f), paint);
        canvas.drawOval(new RectF(x + w * 0.1f, y + h * 0.2f, x + w * 0.9f, y + h * 0.8f), outlinePaint);
        // Highlight
        paint.setColor(lighten(color, 0.3f));
        canvas.drawOval(new RectF(x + w * 0.3f, y + h * 0.3f, x + w * 0.5f, y + h * 0.45f), paint);
    }

    private static void drawKey(Canvas canvas, int color, float x, float y,
                                  float w, float h) {
        paint.setColor(color);
        // Key ring
        float cx = x + w / 2;
        canvas.drawCircle(cx, y + h * 0.2f, w * 0.2f, paint);
        paint.setColor(darken(color, 0.3f));
        canvas.drawCircle(cx, y + h * 0.2f, w * 0.1f, paint);
        // Shaft
        paint.setColor(color);
        canvas.drawRect(cx - w * 0.06f, y + h * 0.3f, cx + w * 0.06f, y + h * 0.85f, paint);
        // Teeth
        canvas.drawRect(cx + w * 0.06f, y + h * 0.7f, cx + w * 0.25f, y + h * 0.78f, paint);
        canvas.drawRect(cx + w * 0.06f, y + h * 0.8f, cx + w * 0.2f, y + h * 0.88f, paint);
    }

    private static void drawArmor(Canvas canvas, int color, float x, float y,
                                    float w, float h) {
        paint.setColor(color);
        // Body
        Path armor = new Path();
        armor.moveTo(x + w * 0.2f, y + h * 0.1f);
        armor.lineTo(x + w * 0.8f, y + h * 0.1f);
        armor.lineTo(x + w * 0.85f, y + h * 0.3f);
        armor.lineTo(x + w * 0.7f, y + h * 0.3f);
        armor.lineTo(x + w * 0.7f, y + h * 0.9f);
        armor.lineTo(x + w * 0.3f, y + h * 0.9f);
        armor.lineTo(x + w * 0.3f, y + h * 0.3f);
        armor.lineTo(x + w * 0.15f, y + h * 0.3f);
        armor.close();
        canvas.drawPath(armor, paint);
        canvas.drawPath(armor, outlinePaint);
    }

    private static void drawHat(Canvas canvas, int color, float x, float y,
                                  float w, float h) {
        paint.setColor(color);
        // Brim
        canvas.drawOval(new RectF(x, y + h * 0.6f, x + w, y + h), paint);
        // Crown
        canvas.drawOval(new RectF(x + w * 0.2f, y, x + w * 0.8f, y + h * 0.75f), paint);
        canvas.drawOval(new RectF(x + w * 0.2f, y, x + w * 0.8f, y + h * 0.75f), outlinePaint);
    }

    private static void drawBoots(Canvas canvas, int color, float x, float y,
                                    float w, float h) {
        paint.setColor(color);
        // Shaft
        canvas.drawRect(x + w * 0.25f, y, x + w * 0.65f, y + h * 0.7f, paint);
        // Foot
        canvas.drawRoundRect(new RectF(x + w * 0.1f, y + h * 0.6f, x + w * 0.9f, y + h),
                w * 0.1f, h * 0.1f, paint);
        canvas.drawRoundRect(new RectF(x + w * 0.1f, y + h * 0.6f, x + w * 0.9f, y + h),
                w * 0.1f, h * 0.1f, outlinePaint);
    }

    private static void drawRing(Canvas canvas, int color, float x, float y,
                                   float w, float h) {
        float cx = x + w / 2;
        float cy = y + h / 2;
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(w * 0.15f);
        canvas.drawCircle(cx, cy, Math.min(w, h) * 0.3f, paint);
        paint.setStyle(Paint.Style.FILL);
        // Gem on top
        paint.setColor(lighten(color, 0.3f));
        canvas.drawCircle(cx, cy - Math.min(w, h) * 0.3f, w * 0.12f, paint);
    }

    private static void drawGem(Canvas canvas, int color, float x, float y,
                                  float w, float h) {
        paint.setColor(color);
        Path gem = new Path();
        float cx = x + w / 2;
        gem.moveTo(cx, y);
        gem.lineTo(x + w, y + h * 0.4f);
        gem.lineTo(cx, y + h);
        gem.lineTo(x, y + h * 0.4f);
        gem.close();
        canvas.drawPath(gem, paint);
        canvas.drawPath(gem, outlinePaint);
        // Facets
        paint.setColor(lighten(color, 0.2f));
        Path facet = new Path();
        facet.moveTo(cx, y);
        facet.lineTo(cx + w * 0.15f, y + h * 0.4f);
        facet.lineTo(cx, y + h * 0.5f);
        facet.lineTo(cx - w * 0.15f, y + h * 0.4f);
        facet.close();
        canvas.drawPath(facet, paint);
    }

    private static void drawChest(Canvas canvas, int color, float x, float y,
                                    float w, float h) {
        // Body
        paint.setColor(0xFF8B4513); // brown
        canvas.drawRect(x, y + h * 0.3f, x + w, y + h, paint);
        // Lid
        paint.setColor(darken(0xFF8B4513, 0.8f));
        RectF lid = new RectF(x, y, x + w, y + h * 0.4f);
        canvas.drawRoundRect(lid, w * 0.05f, h * 0.1f, paint);
        // Lock
        paint.setColor(color);
        canvas.drawCircle(x + w / 2, y + h * 0.35f, w * 0.08f, paint);
        // Metal bands
        paint.setColor(0xFF666666);
        canvas.drawRect(x + w * 0.1f, y + h * 0.28f, x + w * 0.9f, y + h * 0.33f, paint);
        outlinePaint.setColor(Color.BLACK);
        canvas.drawRect(x, y + h * 0.3f, x + w, y + h, outlinePaint);
    }

    public static void drawCreature(Canvas canvas, int color, float x, float y,
                                     float w, float h) {
        // Body (oval)
        paint.setColor(color);
        RectF body = new RectF(x + w * 0.1f, y + h * 0.2f, x + w * 0.9f, y + h * 0.9f);
        canvas.drawOval(body, paint);
        canvas.drawOval(body, outlinePaint);
        // Eyes
        paint.setColor(Color.WHITE);
        canvas.drawCircle(x + w * 0.35f, y + h * 0.35f, w * 0.1f, paint);
        canvas.drawCircle(x + w * 0.65f, y + h * 0.35f, w * 0.1f, paint);
        // Pupils
        paint.setColor(Color.RED);
        canvas.drawCircle(x + w * 0.37f, y + h * 0.36f, w * 0.05f, paint);
        canvas.drawCircle(x + w * 0.67f, y + h * 0.36f, w * 0.05f, paint);
        // Mouth
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        RectF mouthRect = new RectF(x + w * 0.3f, y + h * 0.5f, x + w * 0.7f, y + h * 0.7f);
        canvas.drawArc(mouthRect, 0, 180, false, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    public static void drawDoor(Canvas canvas, int color, float x, float y,
                                  float w, float h) {
        // Door frame
        paint.setColor(darken(color, 0.5f));
        canvas.drawRect(x, y, x + w, y + h, paint);
        // Door body
        paint.setColor(color);
        canvas.drawRect(x + w * 0.08f, y + h * 0.05f, x + w * 0.92f, y + h, paint);
        // Arch at top
        paint.setColor(darken(color, 0.3f));
        RectF arch = new RectF(x + w * 0.08f, y - h * 0.15f, x + w * 0.92f, y + h * 0.3f);
        canvas.drawArc(arch, 180, 180, true, paint);
        // Handle
        paint.setColor(0xFFFFD700);
        canvas.drawCircle(x + w * 0.75f, y + h * 0.55f, w * 0.06f, paint);
        // Outline
        canvas.drawRect(x, y, x + w, y + h, outlinePaint);
    }

    private static void drawTrap(Canvas canvas, int color, float x, float y,
                                   float w, float h) {
        // Warning triangle
        paint.setColor(0xFFFF4444);
        Path triangle = new Path();
        triangle.moveTo(x + w / 2, y);
        triangle.lineTo(x + w, y + h);
        triangle.lineTo(x, y + h);
        triangle.close();
        canvas.drawPath(triangle, paint);
        canvas.drawPath(triangle, outlinePaint);
        // Exclamation mark
        paint.setColor(Color.BLACK);
        canvas.drawRect(x + w * 0.44f, y + h * 0.25f, x + w * 0.56f, y + h * 0.65f, paint);
        canvas.drawCircle(x + w / 2, y + h * 0.8f, w * 0.06f, paint);
    }

    private static void drawMaterial(Canvas canvas, int color, float x, float y,
                                      float w, float h) {
        paint.setColor(color);
        // Rough block shape
        Path block = new Path();
        block.moveTo(x + w * 0.15f, y + h * 0.1f);
        block.lineTo(x + w * 0.85f, y + h * 0.05f);
        block.lineTo(x + w * 0.9f, y + h * 0.9f);
        block.lineTo(x + w * 0.1f, y + h * 0.95f);
        block.close();
        canvas.drawPath(block, paint);
        canvas.drawPath(block, outlinePaint);
    }

    public static void drawRarityGlow(Canvas canvas, int glowColor, float x, float y,
                                       float w, float h, float glowRadius) {
        Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(glowRadius);
        glowPaint.setColor(glowColor);
        glowPaint.setAlpha(100);
        canvas.drawRoundRect(new RectF(x - glowRadius / 2, y - glowRadius / 2,
                x + w + glowRadius / 2, y + h + glowRadius / 2), glowRadius, glowRadius, glowPaint);
    }

    public static void drawLabel(Canvas canvas, String text, float x, float y,
                                  float maxWidth, float textSize) {
        textPaint.setTextSize(textSize);
        textPaint.setShadowLayer(2f, 1f, 1f, Color.BLACK);
        canvas.drawText(text, x + maxWidth / 2, y, textPaint);
    }

    private static int darken(int color, float factor) {
        int r = (int)(Color.red(color) * factor);
        int g = (int)(Color.green(color) * factor);
        int b = (int)(Color.blue(color) * factor);
        return Color.argb(Color.alpha(color), r, g, b);
    }

    private static int lighten(int color, float amount) {
        int r = Math.min(255, Color.red(color) + (int)(255 * amount));
        int g = Math.min(255, Color.green(color) + (int)(255 * amount));
        int b = Math.min(255, Color.blue(color) + (int)(255 * amount));
        return Color.argb(Color.alpha(color), r, g, b);
    }
}
