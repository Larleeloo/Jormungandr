package com.larleeloo.jormungandr.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.larleeloo.jormungandr.model.BiomeType;
import com.larleeloo.jormungandr.model.Player;
import com.larleeloo.jormungandr.util.RoomIdHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapCanvasView extends SurfaceView implements SurfaceHolder.Callback {

    private Player player;
    private String currentRoomId;
    private float offsetX = 0, offsetY = 0;
    private float scale = 1.0f;
    private boolean isReady = false;

    private final Paint nodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint currentNodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint waypointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint regionLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;

    public MapCanvasView(Context context) { super(context); init(context); }
    public MapCanvasView(Context context, AttributeSet attrs) { super(context, attrs); init(context); }
    public MapCanvasView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle); init(context);
    }

    private void init(Context context) {
        getHolder().addCallback(this);
        setFocusable(true);

        currentNodePaint.setColor(Color.WHITE);
        currentNodePaint.setStyle(Paint.Style.FILL);
        waypointPaint.setColor(0xFF00FF00);
        linePaint.setColor(0x44FFFFFF);
        linePaint.setStrokeWidth(1.5f);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(16f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        regionLabelPaint.setColor(0xAAFFD700);
        regionLabelPaint.setTextSize(22f);
        regionLabelPaint.setTextAlign(Paint.Align.CENTER);
        regionLabelPaint.setShadowLayer(2f, 1f, 1f, Color.BLACK);

        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                scale *= detector.getScaleFactor();
                scale = Math.max(0.3f, Math.min(3.0f, scale));
                render();
                return true;
            }
        });

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
                offsetX -= dx;
                offsetY -= dy;
                render();
                return true;
            }
        });
    }

    public void setPlayer(Player player, String currentRoomId) {
        this.player = player;
        this.currentRoomId = currentRoomId;
        // Center on current room
        int currentRegion = RoomIdHelper.getRegion(currentRoomId);
        int currentRoom = RoomIdHelper.getRoomNumber(currentRoomId);
        render();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        return true;
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
        if (!isReady || player == null) return;

        Canvas canvas = null;
        try {
            canvas = getHolder().lockCanvas();
            if (canvas == null) return;

            int w = canvas.getWidth();
            int h = canvas.getHeight();

            // Dark background
            canvas.drawColor(0xFF0D0520);

            canvas.save();
            canvas.translate(w / 2f + offsetX, h / 2f + offsetY);
            canvas.scale(scale, scale);

            // Draw hub at center
            nodePaint.setColor(BiomeType.HUB.getColor());
            canvas.drawCircle(0, 0, 20, nodePaint);
            regionLabelPaint.setTextSize(14f);
            canvas.drawText("HUB", 0, -28, regionLabelPaint);

            // Draw each region as a sector around the hub
            Map<String, List<String>> discovered = player.getDiscoveredRooms();
            float regionRadius = 120f;
            float angleStep = 360f / 8f;

            for (int region = 1; region <= 8; region++) {
                float angle = (region - 1) * angleStep - 90; // Start from top
                float radians = (float) Math.toRadians(angle);
                float centerX = (float) Math.cos(radians) * regionRadius;
                float centerY = (float) Math.sin(radians) * regionRadius;

                // Region label
                BiomeType biome = BiomeType.fromRegion(region);
                regionLabelPaint.setTextSize(12f);
                regionLabelPaint.setColor(biome.getColor());
                canvas.drawText(biome.getDisplayName(), centerX, centerY - 50, regionLabelPaint);

                // Draw line from hub to region
                linePaint.setColor(biome.getColor());
                linePaint.setAlpha(80);
                canvas.drawLine(0, 0, centerX, centerY, linePaint);

                // Draw discovered rooms as small dots
                String regionKey = String.valueOf(region);
                List<String> rooms = discovered.get(regionKey);
                if (rooms != null) {
                    for (int i = 0; i < rooms.size(); i++) {
                        String roomId = rooms.get(i);
                        int roomNum = RoomIdHelper.getRoomNumber(roomId);
                        int zone = RoomIdHelper.getZone(roomNum);

                        // Position room relative to region center, farther for higher zones
                        float roomAngle = radians + (float)(i * 0.3 - rooms.size() * 0.15);
                        float roomDist = zone * 20f;
                        float rx = centerX + (float) Math.cos(roomAngle) * roomDist;
                        float ry = centerY + (float) Math.sin(roomAngle) * roomDist;

                        // Draw node
                        float nodeSize = 4f;
                        if (roomId.equals(currentRoomId)) {
                            canvas.drawCircle(rx, ry, nodeSize + 3, currentNodePaint);
                        }

                        if (RoomIdHelper.isWaypoint(roomId)) {
                            canvas.drawCircle(rx, ry, nodeSize + 1, waypointPaint);
                        }

                        nodePaint.setColor(biome.getColor());
                        canvas.drawCircle(rx, ry, nodeSize, nodePaint);
                    }
                }
            }

            canvas.restore();

        } finally {
            if (canvas != null) {
                getHolder().unlockCanvasAndPost(canvas);
            }
        }
    }
}
