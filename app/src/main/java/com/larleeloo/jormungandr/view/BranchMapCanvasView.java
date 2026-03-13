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

import com.larleeloo.jormungandr.engine.WorldMesh;
import com.larleeloo.jormungandr.model.BiomeType;
import com.larleeloo.jormungandr.model.Direction;
import com.larleeloo.jormungandr.model.Player;
import com.larleeloo.jormungandr.util.Constants;
import com.larleeloo.jormungandr.util.RoomIdHelper;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.HashSet;

/**
 * A branching tree-layout map view that shows the actual topology of discovered
 * rooms. The trunk forms a radial spine from the hub, and branches fork off
 * perpendicularly. Cross-region connections are shown as a smaller overlay dot
 * of the destination region's color on top of the source room's primary dot.
 */
public class BranchMapCanvasView extends SurfaceView implements SurfaceHolder.Callback {

    private Player player;
    private String currentRoomId;
    private float offsetX = 0, offsetY = 0;
    private float scale = 1.0f;
    private boolean isReady = false;

    /** Computed positions for each discovered room ID. */
    private final Map<String, float[]> roomPositions = new HashMap<>();

    /** Set of discovered room IDs for fast lookup. */
    private final Set<String> discoveredSet = new HashSet<>();

    private final Paint nodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint currentNodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint waypointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;

    // Layout constants
    private static final float HUB_RADIUS = 14f;
    private static final float NODE_RADIUS = 5f;
    private static final float OVERLAY_RADIUS = 2.5f;
    private static final float WAYPOINT_EXTRA = 2f;
    private static final float CURRENT_EXTRA = 3f;
    private static final float TRUNK_SPACING = 14f;
    private static final float BRANCH_SPACING = 12f;
    private static final float BRANCH_OFFSET_BASE = 16f;
    private static final float REGION_RADIAL_OFFSET = 30f;
    private static final float EDGE_WIDTH = 1.2f;

    public BranchMapCanvasView(Context context) { super(context); init(context); }
    public BranchMapCanvasView(Context context, AttributeSet attrs) { super(context, attrs); init(context); }
    public BranchMapCanvasView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle); init(context);
    }

    private void init(Context context) {
        getHolder().addCallback(this);
        setFocusable(true);

        currentNodePaint.setColor(Color.WHITE);
        currentNodePaint.setStyle(Paint.Style.STROKE);
        currentNodePaint.setStrokeWidth(2f);
        waypointPaint.setColor(0xFF00FF00);
        waypointPaint.setStyle(Paint.Style.STROKE);
        waypointPaint.setStrokeWidth(1.5f);
        linePaint.setColor(0x44FFFFFF);
        linePaint.setStrokeWidth(EDGE_WIDTH);
        labelPaint.setColor(0xAAFFD700);
        labelPaint.setTextSize(11f);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setShadowLayer(2f, 1f, 1f, Color.BLACK);

        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                scale *= detector.getScaleFactor();
                scale = Math.max(0.15f, Math.min(5.0f, scale));
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
        buildLayout();
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

    // ---- Layout computation ----

    /**
     * Build spatial positions for every discovered room using a BFS tree-walk
     * from the hub outward. Trunk rooms proceed radially; branches fork
     * perpendicular to the trunk direction.
     */
    private void buildLayout() {
        roomPositions.clear();
        discoveredSet.clear();
        if (player == null) return;

        // Collect all discovered rooms into a fast-lookup set
        Map<String, List<String>> discovered = player.getDiscoveredRooms();
        for (List<String> rooms : discovered.values()) {
            discoveredSet.addAll(rooms);
        }
        // Always include hub
        discoveredSet.add(Constants.HUB_ROOM_ID);

        // Hub at origin
        roomPositions.put(Constants.HUB_ROOM_ID, new float[]{0, 0});

        WorldMesh mesh = WorldMesh.getInstance();

        // BFS from hub, assigning positions as we go
        Queue<LayoutTask> queue = new ArrayDeque<>();

        // Seed: hub's neighbors (each region's first trunk room)
        Map<Direction, String> hubNeighbors = mesh.getNeighbors(Constants.HUB_ROOM_ID);
        for (Map.Entry<Direction, String> entry : hubNeighbors.entrySet()) {
            String targetId = entry.getValue();
            if (!discoveredSet.contains(targetId)) continue;
            int targetRegion = RoomIdHelper.getRegion(targetId);
            float angle = getRegionAngle(targetRegion);
            queue.add(new LayoutTask(targetId, 0, 0, angle, REGION_RADIAL_OFFSET, true, 0));
        }

        Set<String> visited = new HashSet<>();
        visited.add(Constants.HUB_ROOM_ID);

        while (!queue.isEmpty()) {
            LayoutTask task = queue.poll();
            if (visited.contains(task.roomId)) continue;
            visited.add(task.roomId);

            // Position this room
            float x = task.parentX + (float) Math.cos(Math.toRadians(task.angle)) * task.distance;
            float y = task.parentY + (float) Math.sin(Math.toRadians(task.angle)) * task.distance;
            roomPositions.put(task.roomId, new float[]{x, y});

            int region = RoomIdHelper.getRegion(task.roomId);
            int roomNum = RoomIdHelper.getRoomNumber(task.roomId);
            Map<Direction, String> neighbors = mesh.getNeighbors(task.roomId);

            // Determine the "forward" angle: same direction as this room was placed from parent
            float forwardAngle = task.angle;
            float leftAngle = forwardAngle - 90;
            float rightAngle = forwardAngle + 90;

            // Spacing shrinks slightly as branches go deeper
            float depthFactor = Math.max(0.5f, 1.0f - task.depth * 0.012f);
            float fwdSpacing = (task.isTrunk ? TRUNK_SPACING : BRANCH_SPACING) * depthFactor;
            float branchSpacing = BRANCH_OFFSET_BASE * depthFactor;

            for (Map.Entry<Direction, String> entry : neighbors.entrySet()) {
                Direction dir = entry.getKey();
                String neighborId = entry.getValue();
                if (dir == Direction.BACK) continue; // don't walk backwards
                if (visited.contains(neighborId)) continue;
                if (!discoveredSet.contains(neighborId)) continue;

                int neighborRegion = RoomIdHelper.getRegion(neighborId);
                int neighborRoom = RoomIdHelper.getRoomNumber(neighborId);

                // Cross-region link — don't lay out the remote room from here;
                // it'll be laid out from its own region's trunk.
                if (neighborRegion != region && neighborRegion != 0) continue;

                float childAngle;
                float childDist;
                boolean childIsTrunk = false;

                if (dir == Direction.FORWARD) {
                    childAngle = forwardAngle;
                    childDist = fwdSpacing;
                    childIsTrunk = task.isTrunk && RoomIdHelper.isTrunkRoom(neighborRoom);
                } else if (dir == Direction.LEFT) {
                    childAngle = leftAngle;
                    childDist = branchSpacing;
                } else { // RIGHT
                    childAngle = rightAngle;
                    childDist = branchSpacing;
                }

                queue.add(new LayoutTask(neighborId, x, y, childAngle, childDist,
                        childIsTrunk, task.depth + 1));
            }
        }
    }

    /**
     * Angle (degrees) from hub center toward a given region. Regions 1-8
     * are evenly spaced at 45° starting from the top (−90°).
     */
    private float getRegionAngle(int region) {
        if (region <= 0) return 0;
        return (region - 1) * 45f - 90f;
    }

    // ---- Rendering ----

    public void render() {
        if (!isReady || player == null) return;

        Canvas canvas = null;
        try {
            canvas = getHolder().lockCanvas();
            if (canvas == null) return;

            int w = canvas.getWidth();
            int h = canvas.getHeight();

            canvas.drawColor(0xFF0D0520);
            canvas.save();
            canvas.translate(w / 2f + offsetX, h / 2f + offsetY);
            canvas.scale(scale, scale);

            WorldMesh mesh = WorldMesh.getInstance();

            // Pass 1: draw edges between discovered, adjacent rooms
            for (Map.Entry<String, float[]> entry : roomPositions.entrySet()) {
                String roomId = entry.getKey();
                float[] pos = entry.getValue();
                Map<Direction, String> neighbors = mesh.getNeighbors(roomId);

                int roomRegion = RoomIdHelper.getRegion(roomId);

                for (Map.Entry<Direction, String> nEntry : neighbors.entrySet()) {
                    String neighborId = nEntry.getValue();
                    float[] neighborPos = roomPositions.get(neighborId);
                    if (neighborPos == null) continue;

                    int neighborRegion = RoomIdHelper.getRegion(neighborId);
                    // Use region color for same-region edges; dim white for cross-region
                    if (neighborRegion == roomRegion || roomRegion == 0) {
                        BiomeType biome = BiomeType.fromRegion(
                                roomRegion == 0 ? neighborRegion : roomRegion);
                        linePaint.setColor(biome.getColor());
                        linePaint.setAlpha(60);
                    } else {
                        linePaint.setColor(0x44FFFFFF);
                    }
                    canvas.drawLine(pos[0], pos[1], neighborPos[0], neighborPos[1], linePaint);
                }
            }

            // Pass 2: draw nodes
            for (Map.Entry<String, float[]> entry : roomPositions.entrySet()) {
                String roomId = entry.getKey();
                float[] pos = entry.getValue();
                int region = RoomIdHelper.getRegion(roomId);
                int roomNum = RoomIdHelper.getRoomNumber(roomId);
                BiomeType biome = BiomeType.fromRegion(region);

                boolean isHub = roomId.equals(Constants.HUB_ROOM_ID);
                boolean isWaypoint = RoomIdHelper.isWaypoint(roomId);
                boolean isCurrent = roomId.equals(currentRoomId);

                float radius = isHub ? HUB_RADIUS : NODE_RADIUS;

                // Current room highlight
                if (isCurrent) {
                    canvas.drawCircle(pos[0], pos[1], radius + CURRENT_EXTRA,
                            currentNodePaint);
                }

                // Waypoint ring
                if (isWaypoint) {
                    canvas.drawCircle(pos[0], pos[1], radius + WAYPOINT_EXTRA,
                            waypointPaint);
                }

                // Primary dot
                nodePaint.setColor(biome.getColor());
                nodePaint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(pos[0], pos[1], radius, nodePaint);

                // Cross-region overlay dots: for each neighbor in a different region,
                // draw a small dot of the target region's color on top of this node.
                Map<Direction, String> neighbors = mesh.getNeighbors(roomId);
                int overlayIndex = 0;
                for (Map.Entry<Direction, String> nEntry : neighbors.entrySet()) {
                    String neighborId = nEntry.getValue();
                    int neighborRegion = RoomIdHelper.getRegion(neighborId);
                    if (neighborRegion != region && neighborRegion != 0 && region != 0) {
                        BiomeType targetBiome = BiomeType.fromRegion(neighborRegion);
                        overlayPaint.setColor(targetBiome.getColor());
                        overlayPaint.setStyle(Paint.Style.FILL);

                        // Offset overlays slightly so multiple don't stack exactly
                        float oX = pos[0] + (overlayIndex % 2 == 0 ? -2.5f : 2.5f);
                        float oY = pos[1] + (overlayIndex < 2 ? -2.5f : 2.5f);
                        canvas.drawCircle(oX, oY, OVERLAY_RADIUS, overlayPaint);
                        overlayIndex++;
                    }
                }

                // Hub label
                if (isHub) {
                    labelPaint.setColor(0xAAFFD700);
                    canvas.drawText("HUB", pos[0], pos[1] - HUB_RADIUS - 4, labelPaint);
                }
            }

            // Pass 3: region name labels at the outer end of each region's trunk
            for (int r = 1; r <= Constants.NUM_REGIONS; r++) {
                BiomeType biome = BiomeType.fromRegion(r);
                float angle = getRegionAngle(r);
                float radians = (float) Math.toRadians(angle);

                // Find the farthest trunk room for this region to place the label beyond it
                float maxDist = REGION_RADIAL_OFFSET;
                for (Map.Entry<String, float[]> entry : roomPositions.entrySet()) {
                    if (RoomIdHelper.getRegion(entry.getKey()) == r) {
                        float[] p = entry.getValue();
                        float dist = (float) Math.sqrt(p[0] * p[0] + p[1] * p[1]);
                        if (dist > maxDist) maxDist = dist;
                    }
                }

                float labelDist = maxDist + 20f;
                float lx = (float) Math.cos(radians) * labelDist;
                float ly = (float) Math.sin(radians) * labelDist;
                labelPaint.setColor(biome.getColor());
                labelPaint.setTextSize(11f);
                canvas.drawText(biome.getDisplayName(), lx, ly, labelPaint);
            }

            canvas.restore();
        } finally {
            if (canvas != null) {
                getHolder().unlockCanvasAndPost(canvas);
            }
        }
    }

    /**
     * Internal task for BFS layout. Carries enough state to position a room
     * relative to its parent.
     */
    private static class LayoutTask {
        final String roomId;
        final float parentX, parentY;
        final float angle;    // direction from parent (degrees)
        final float distance; // distance from parent
        final boolean isTrunk;
        final int depth;

        LayoutTask(String roomId, float parentX, float parentY,
                   float angle, float distance, boolean isTrunk, int depth) {
            this.roomId = roomId;
            this.parentX = parentX;
            this.parentY = parentY;
            this.angle = angle;
            this.distance = distance;
            this.isTrunk = isTrunk;
            this.depth = depth;
        }
    }
}
