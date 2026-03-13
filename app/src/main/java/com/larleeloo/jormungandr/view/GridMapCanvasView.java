package com.larleeloo.jormungandr.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.larleeloo.jormungandr.engine.WorldMesh;
import com.larleeloo.jormungandr.model.BiomeType;
import com.larleeloo.jormungandr.model.Direction;
import com.larleeloo.jormungandr.model.Player;
import com.larleeloo.jormungandr.util.Constants;
import com.larleeloo.jormungandr.util.RoomIdHelper;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Grid-based map view that shows a single region's 100×100 maze layout.
 * Discovered rooms are drawn as colored cells; connections between rooms
 * are drawn as passages. Portal doors to other regions are shown as
 * small colored dots representing the target region.
 */
public class GridMapCanvasView extends SurfaceView implements SurfaceHolder.Callback {

    private Player player;
    private String currentRoomId;
    private int displayRegion = 1;
    private float offsetX = 0, offsetY = 0;
    private float scale = 1.0f;
    private boolean isReady = false;

    private final Set<String> discoveredSet = new HashSet<>();

    private final Paint cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint wallPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint passagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint currentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint waypointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint portalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Layout constants
    private static final float CELL_SIZE = 10f;
    private static final float WALL_THICKNESS = 1.5f;
    private static final float PASSAGE_WIDTH = 4f;
    private static final float PORTAL_RADIUS = 2.5f;

    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;

    public GridMapCanvasView(Context context) { super(context); init(context); }
    public GridMapCanvasView(Context context, AttributeSet attrs) { super(context, attrs); init(context); }
    public GridMapCanvasView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle); init(context);
    }

    private void init(Context context) {
        getHolder().addCallback(this);
        setFocusable(true);

        currentPaint.setColor(Color.WHITE);
        currentPaint.setStyle(Paint.Style.STROKE);
        currentPaint.setStrokeWidth(2f);

        waypointPaint.setColor(0xFF00FF00);
        waypointPaint.setStyle(Paint.Style.STROKE);
        waypointPaint.setStrokeWidth(1.5f);

        wallPaint.setColor(0xFF1A0A2E);
        wallPaint.setStyle(Paint.Style.FILL);

        gridLinePaint.setColor(0x15FFFFFF);
        gridLinePaint.setStrokeWidth(0.5f);

        labelPaint.setColor(0xAAFFD700);
        labelPaint.setTextSize(12f);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setShadowLayer(2f, 1f, 1f, Color.BLACK);

        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                scale *= detector.getScaleFactor();
                scale = Math.max(0.3f, Math.min(8.0f, scale));
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
        rebuildDiscoveredSet();
        centerOnCurrentRoom();
        render();
    }

    public void setDisplayRegion(int region) {
        this.displayRegion = region;
        centerOnCurrentRoom();
        render();
    }

    public int getDisplayRegion() {
        return displayRegion;
    }

    private void rebuildDiscoveredSet() {
        discoveredSet.clear();
        if (player == null) return;
        Map<String, List<String>> discovered = player.getDiscoveredRooms();
        for (List<String> rooms : discovered.values()) {
            discoveredSet.addAll(rooms);
        }
        discoveredSet.add(Constants.HUB_ROOM_ID);
    }

    private void centerOnCurrentRoom() {
        if (currentRoomId == null) return;
        int region = RoomIdHelper.getRegion(currentRoomId);
        if (region == displayRegion) {
            int row = RoomIdHelper.getRow(currentRoomId);
            int col = RoomIdHelper.getCol(currentRoomId);
            offsetX = -(col * CELL_SIZE * scale);
            offsetY = -(row * CELL_SIZE * scale);
        } else {
            offsetX = 0;
            offsetY = 0;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        return true;
    }

    @Override public void surfaceCreated(SurfaceHolder holder) { isReady = true; render(); }
    @Override public void surfaceChanged(SurfaceHolder h, int f, int w, int ht) { render(); }
    @Override public void surfaceDestroyed(SurfaceHolder holder) { isReady = false; }

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

            // Handle hub region specially
            if (displayRegion == 0) {
                drawHubView(canvas, w, h);
                return;
            }

            canvas.save();
            canvas.translate(w / 2f + offsetX, h / 2f + offsetY);
            canvas.scale(scale, scale);

            BiomeType biome = BiomeType.fromRegion(displayRegion);
            int regionColor = biome.getColor();
            WorldMesh mesh = WorldMesh.getInstance();

            int size = Constants.GRID_SIZE;

            // Draw discovered rooms and their connections
            for (int row = 0; row < size; row++) {
                for (int col = 0; col < size; col++) {
                    String roomId = RoomIdHelper.makeRoomId(displayRegion, row, col);
                    if (!discoveredSet.contains(roomId)) continue;

                    float cx = col * CELL_SIZE;
                    float cy = row * CELL_SIZE;
                    float half = CELL_SIZE / 2f;

                    // Draw cell
                    cellPaint.setColor(regionColor);
                    cellPaint.setAlpha(180);
                    cellPaint.setStyle(Paint.Style.FILL);
                    RectF cellRect = new RectF(cx - half + 1, cy - half + 1,
                            cx + half - 1, cy + half - 1);
                    canvas.drawRect(cellRect, cellPaint);

                    // Waypoint highlight
                    if (RoomIdHelper.isWaypoint(RoomIdHelper.toRoomNumber(row, col))) {
                        canvas.drawRect(cellRect, waypointPaint);
                    }

                    // Current room highlight
                    if (roomId.equals(currentRoomId)) {
                        RectF highlightRect = new RectF(cx - half - 1, cy - half - 1,
                                cx + half + 1, cy + half + 1);
                        canvas.drawRect(highlightRect, currentPaint);
                    }

                    // Draw connections (passages) to neighbors
                    Map<Direction, String> neighbors = mesh.getNeighbors(roomId);
                    for (Map.Entry<Direction, String> entry : neighbors.entrySet()) {
                        Direction dir = entry.getKey();
                        String neighborId = entry.getValue();
                        int neighborRegion = RoomIdHelper.getRegion(neighborId);

                        if (neighborRegion != displayRegion) {
                            // Portal door — draw small colored dot for target region
                            BiomeType targetBiome = BiomeType.fromRegion(neighborRegion);
                            portalPaint.setColor(targetBiome.getColor());
                            portalPaint.setStyle(Paint.Style.FILL);

                            float px = cx + dir.getDCol() * (half - 1);
                            float py = cy + dir.getDRow() * (half - 1);
                            canvas.drawCircle(px, py, PORTAL_RADIUS, portalPaint);
                        } else if (discoveredSet.contains(neighborId)) {
                            // Same-region neighbor that's also discovered — draw passage
                            passagePaint.setColor(regionColor);
                            passagePaint.setAlpha(120);
                            passagePaint.setStyle(Paint.Style.FILL);

                            float nx = RoomIdHelper.getCol(neighborId) * CELL_SIZE;
                            float ny = RoomIdHelper.getRow(neighborId) * CELL_SIZE;

                            // Draw a thin rect connecting the two cells
                            if (dir == Direction.RIGHT || dir == Direction.LEFT) {
                                float minX = Math.min(cx, nx);
                                float maxX = Math.max(cx, nx);
                                canvas.drawRect(minX + half - 1, cy - PASSAGE_WIDTH / 2f,
                                        maxX - half + 1, cy + PASSAGE_WIDTH / 2f, passagePaint);
                            } else {
                                float minY = Math.min(cy, ny);
                                float maxY = Math.max(cy, ny);
                                canvas.drawRect(cx - PASSAGE_WIDTH / 2f, minY + half - 1,
                                        cx + PASSAGE_WIDTH / 2f, maxY - half + 1, passagePaint);
                            }
                        }
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

    /**
     * Draw a simple hub overview showing connections to each region.
     */
    private void drawHubView(Canvas canvas, int w, int h) {
        float centerX = w / 2f;
        float centerY = h / 2f;

        // Hub circle
        cellPaint.setColor(BiomeType.HUB.getColor());
        cellPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(centerX, centerY, 30, cellPaint);

        labelPaint.setColor(0xAAFFD700);
        labelPaint.setTextSize(16f);
        canvas.drawText("HUB", centerX, centerY + 5, labelPaint);

        // Draw connections to each region
        float radius = 100f;
        WorldMesh mesh = WorldMesh.getInstance();
        Map<Direction, String> hubNeighbors = mesh.getNeighbors(Constants.HUB_ROOM_ID);

        for (int region = 1; region <= Constants.NUM_REGIONS; region++) {
            BiomeType biome = BiomeType.fromRegion(region);
            float angle = (float) Math.toRadians((region - 1) * 45.0 - 90.0);
            float rx = centerX + (float) Math.cos(angle) * radius;
            float ry = centerY + (float) Math.sin(angle) * radius;

            // Draw line from hub
            passagePaint.setColor(biome.getColor());
            passagePaint.setAlpha(80);
            passagePaint.setStrokeWidth(2f);
            passagePaint.setStyle(Paint.Style.STROKE);
            canvas.drawLine(centerX, centerY, rx, ry, passagePaint);

            // Region dot
            cellPaint.setColor(biome.getColor());
            cellPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(rx, ry, 20, cellPaint);

            // Region label
            labelPaint.setColor(biome.getColor());
            labelPaint.setTextSize(11f);
            canvas.drawText(biome.getDisplayName(), rx, ry + 32, labelPaint);

            // Room count
            Map<String, List<String>> discovered = player.getDiscoveredRooms();
            List<String> rooms = discovered.get(String.valueOf(region));
            int count = rooms != null ? rooms.size() : 0;
            labelPaint.setColor(0xAAFFFFFF);
            labelPaint.setTextSize(10f);
            canvas.drawText(count + " rooms", rx, ry + 44, labelPaint);
        }
    }
}
