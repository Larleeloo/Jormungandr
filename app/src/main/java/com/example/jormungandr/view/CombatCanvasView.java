package com.example.jormungandr.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.jormungandr.model.BiomeType;
import com.example.jormungandr.model.CombatCreature;
import com.example.jormungandr.model.Player;

/**
 * Pokemon-style battle screen rendered on a Canvas.
 * Player sprite bottom-left, creature sprite top-right, HP bars.
 */
public class CombatCanvasView extends SurfaceView implements SurfaceHolder.Callback {

    private Player player;
    private CombatCreature creature;
    private int regionColor = Color.DKGRAY;
    private String lastMessage = "";
    private boolean isReady = false;

    private final Paint bgPaint = new Paint();
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint messagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hpBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hpFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hpBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint namePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public CombatCanvasView(Context context) { super(context); init(); }
    public CombatCanvasView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public CombatCanvasView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle); init();
    }

    private void init() {
        getHolder().addCallback(this);
        setFocusable(true);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(32f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setShadowLayer(3f, 2f, 2f, Color.BLACK);

        messagePaint.setColor(Color.WHITE);
        messagePaint.setTextSize(24f);
        messagePaint.setTextAlign(Paint.Align.CENTER);
        messagePaint.setShadowLayer(2f, 1f, 1f, Color.BLACK);

        hpBgPaint.setColor(0xFF333333);
        hpBorderPaint.setColor(0xFF666666);
        hpBorderPaint.setStyle(Paint.Style.STROKE);
        hpBorderPaint.setStrokeWidth(2f);

        namePaint.setColor(Color.WHITE);
        namePaint.setTextSize(22f);
        namePaint.setShadowLayer(2f, 1f, 1f, Color.BLACK);
    }

    public void setCombatants(Player player, CombatCreature creature, int region) {
        this.player = player;
        this.creature = creature;
        this.regionColor = BiomeType.fromRegion(region).getColor();
        render();
    }

    public void setMessage(String message) {
        this.lastMessage = message;
        render();
    }

    public void updateState() {
        render();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        isReady = true;
        render();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        render();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isReady = false;
    }

    public void render() {
        if (!isReady || player == null || creature == null) return;

        Canvas canvas = null;
        try {
            canvas = getHolder().lockCanvas();
            if (canvas == null) return;

            int w = canvas.getWidth();
            int h = canvas.getHeight();

            // Background - biome colored gradient
            bgPaint.setColor(darken(regionColor, 0.3f));
            canvas.drawRect(0, 0, w, h, bgPaint);

            // Ground line
            float groundY = h * 0.65f;
            Paint groundPaint = new Paint();
            groundPaint.setColor(darken(regionColor, 0.2f));
            canvas.drawRect(0, groundY, w, h, groundPaint);

            // Draw creature (top-right, larger)
            float creatureX = w * 0.55f;
            float creatureY = h * 0.12f;
            float creatureW = w * 0.35f;
            float creatureH = h * 0.3f;

            int creatureColor = creature.getDef() != null ?
                    creature.getDef().getPlaceholderColorInt() : 0xFFCC0000;
            PlaceholderRenderer.drawCreature(canvas, creatureColor,
                    creatureX, creatureY, creatureW, creatureH);

            // Creature name and HP bar
            String creatureName = creature.getDef() != null ?
                    creature.getDef().getDisplayName() : "Enemy";
            namePaint.setColor(Color.WHITE);
            canvas.drawText(creatureName + " Lv." + creature.getLevel(),
                    creatureX, creatureY - 40, namePaint);

            drawHpBar(canvas, creatureX, creatureY - 30, creatureW,
                    creature.getCurrentHp(), creature.getMaxHp(), Color.RED);

            // Draw player (bottom-left, medium)
            float playerX = w * 0.05f;
            float playerY = groundY - h * 0.25f;
            float playerW = w * 0.25f;
            float playerH = h * 0.25f;

            // Simple player shape (knight silhouette)
            Paint playerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            playerPaint.setColor(0xFF4488FF);
            // Body
            canvas.drawRect(playerX + playerW * 0.3f, playerY + playerH * 0.3f,
                    playerX + playerW * 0.7f, playerY + playerH * 0.9f, playerPaint);
            // Head
            canvas.drawCircle(playerX + playerW * 0.5f, playerY + playerH * 0.2f,
                    playerW * 0.15f, playerPaint);
            // Sword
            Paint swordPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            swordPaint.setColor(0xFFC0C0C0);
            canvas.drawRect(playerX + playerW * 0.7f, playerY + playerH * 0.2f,
                    playerX + playerW * 0.75f, playerY + playerH * 0.7f, swordPaint);

            // Player name and HP
            namePaint.setColor(0xFF4488FF);
            canvas.drawText(player.getName() + " Lv." + player.getLevel(),
                    playerX, playerY - 40, namePaint);

            drawHpBar(canvas, playerX, playerY - 30, playerW * 1.5f,
                    player.getHp(), player.getMaxHp(), 0xFF00CC00);

            // Message box at bottom
            if (lastMessage != null && !lastMessage.isEmpty()) {
                Paint msgBg = new Paint();
                msgBg.setColor(0xDD000000);
                float msgY = h * 0.72f;
                canvas.drawRect(0, msgY, w, msgY + h * 0.12f, msgBg);

                // Word wrap the message
                float textY = msgY + 30;
                messagePaint.setTextSize(20f);
                String[] words = lastMessage.split(" ");
                StringBuilder line = new StringBuilder();
                for (String word : words) {
                    if (messagePaint.measureText(line + " " + word) > w * 0.9f) {
                        canvas.drawText(line.toString(), w / 2f, textY, messagePaint);
                        textY += 24;
                        line = new StringBuilder(word);
                    } else {
                        if (line.length() > 0) line.append(" ");
                        line.append(word);
                    }
                }
                canvas.drawText(line.toString(), w / 2f, textY, messagePaint);
            }

        } finally {
            if (canvas != null) {
                getHolder().unlockCanvasAndPost(canvas);
            }
        }
    }

    private void drawHpBar(Canvas canvas, float x, float y, float width,
                            int current, int max, int fillColor) {
        float barHeight = 18f;
        RectF rect = new RectF(x, y, x + width, y + barHeight);
        canvas.drawRoundRect(rect, 4, 4, hpBgPaint);

        float ratio = max > 0 ? (float) current / max : 0;
        hpFillPaint.setColor(fillColor);
        RectF fillRect = new RectF(x, y, x + width * ratio, y + barHeight);
        canvas.drawRoundRect(fillRect, 4, 4, hpFillPaint);

        canvas.drawRoundRect(rect, 4, 4, hpBorderPaint);

        // HP text
        Paint hpText = new Paint(Paint.ANTI_ALIAS_FLAG);
        hpText.setColor(Color.WHITE);
        hpText.setTextSize(14f);
        hpText.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(current + "/" + max, x + width / 2, y + barHeight - 3, hpText);
    }

    private int darken(int color, float factor) {
        return Color.argb(Color.alpha(color),
                (int)(Color.red(color) * factor),
                (int)(Color.green(color) * factor),
                (int)(Color.blue(color) * factor));
    }
}
