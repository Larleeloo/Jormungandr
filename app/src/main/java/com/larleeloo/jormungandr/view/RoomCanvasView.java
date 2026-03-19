package com.larleeloo.jormungandr.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.larleeloo.jormungandr.asset.GameAssetManager;
import com.larleeloo.jormungandr.data.GameRepository;
import com.larleeloo.jormungandr.model.BiomeType;
import com.larleeloo.jormungandr.model.CreatureDef;
import com.larleeloo.jormungandr.model.Direction;
import com.larleeloo.jormungandr.model.ItemDef;
import com.larleeloo.jormungandr.model.Room;
import com.larleeloo.jormungandr.model.RoomObject;
import com.larleeloo.jormungandr.util.Constants;
import com.larleeloo.jormungandr.util.RoomIdHelper;

import java.util.ArrayList;
import java.util.List;

public class RoomCanvasView extends SurfaceView implements SurfaceHolder.Callback {

    public interface RoomInteractionListener {
        void onDoorTapped(Direction direction, String targetRoomId);
        void onObjectTapped(RoomObject object);
        void onBackgroundTapped(float x, float y);
    }

    private Room currentRoom;
    private RoomInteractionListener listener;
    private final Paint backgroundPaint = new Paint();
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint floorPaint = new Paint();
    private final Paint wallPaint = new Paint();
    private final Paint ceilingPaint = new Paint();
    private final Paint hudPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint doorLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final List<DoorHitBox> doorHitBoxes = new ArrayList<>();
    private final List<ObjectHitBox> objectHitBoxes = new ArrayList<>();

    private int canvasWidth;
    private int canvasHeight;
    private boolean isReady = false;

    public RoomCanvasView(Context context) {
        super(context);
        init();
    }

    public RoomCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RoomCanvasView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        getHolder().addCallback(this);
        setFocusable(true);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(40f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setShadowLayer(3f, 2f, 2f, Color.BLACK);

        hudPaint.setColor(Color.WHITE);
        hudPaint.setTextSize(28f);
        hudPaint.setShadowLayer(2f, 1f, 1f, Color.BLACK);

        doorLabelPaint.setColor(Color.WHITE);
        doorLabelPaint.setTextSize(24f);
        doorLabelPaint.setTextAlign(Paint.Align.CENTER);
        doorLabelPaint.setShadowLayer(2f, 1f, 1f, Color.BLACK);
    }

    public void setRoom(Room room) {
        this.currentRoom = room;
        if (isReady) {
            renderRoom();
        }
    }

    public void setInteractionListener(RoomInteractionListener listener) {
        this.listener = listener;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        isReady = true;
        if (currentRoom != null) {
            renderRoom();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        canvasWidth = width;
        canvasHeight = height;
        if (currentRoom != null) {
            renderRoom();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isReady = false;
    }

    public void renderRoom() {
        if (!isReady || currentRoom == null) return;

        Canvas canvas = null;
        try {
            canvas = getHolder().lockCanvas();
            if (canvas == null) return;

            canvasWidth = canvas.getWidth();
            canvasHeight = canvas.getHeight();
            doorHitBoxes.clear();
            objectHitBoxes.clear();

            drawBackground(canvas);
            drawRoomStructure(canvas);
            drawDoors(canvas);
            drawObjects(canvas);
            drawRoomInfo(canvas);

        } finally {
            if (canvas != null) {
                getHolder().unlockCanvasAndPost(canvas);
            }
        }
    }

    private void drawBackground(Canvas canvas) {
        int bgColor;
        if (currentRoom.getRegion() == 0) {
            bgColor = BiomeType.HUB.getColor();
        } else {
            bgColor = BiomeType.fromRegion(currentRoom.getRegion()).getColor();
        }

        // Try loading a background image from assets
        Bitmap bgBitmap = loadAsset(currentRoom.getBackgroundId());
        if (bgBitmap != null) {
            canvas.drawBitmap(bgBitmap, null,
                    new RectF(0, 0, canvasWidth, canvasHeight), null);
        } else {
            // Gradient-style placeholder background
            backgroundPaint.setColor(bgColor);
            canvas.drawRect(0, 0, canvasWidth, canvasHeight, backgroundPaint);
        }
    }

    private void drawRoomStructure(Canvas canvas) {
        // Draw dollhouse/cutaway room structure
        float wallMargin = canvasWidth * 0.05f;
        float ceilingY = canvasHeight * 0.08f;
        float floorY = canvasHeight * 0.82f;

        // Ceiling
        int bgColor = BiomeType.fromRegion(currentRoom.getRegion()).getColor();
        ceilingPaint.setColor(darken(bgColor, 0.4f));
        canvas.drawRect(wallMargin, ceilingY, canvasWidth - wallMargin, ceilingY + canvasHeight * 0.04f, ceilingPaint);

        // Left wall
        wallPaint.setColor(darken(bgColor, 0.6f));
        canvas.drawRect(wallMargin, ceilingY, wallMargin + canvasWidth * 0.03f, floorY, wallPaint);

        // Right wall
        canvas.drawRect(canvasWidth - wallMargin - canvasWidth * 0.03f, ceilingY,
                canvasWidth - wallMargin, floorY, wallPaint);

        // Floor
        floorPaint.setColor(darken(bgColor, 0.3f));
        canvas.drawRect(wallMargin, floorY, canvasWidth - wallMargin, floorY + canvasHeight * 0.06f, floorPaint);

        // Floor pattern (stone tiles)
        Paint tilePaint = new Paint();
        tilePaint.setColor(darken(bgColor, 0.25f));
        tilePaint.setStyle(Paint.Style.STROKE);
        tilePaint.setStrokeWidth(1.5f);
        float tileW = (canvasWidth - wallMargin * 2) / 8f;
        for (int i = 0; i <= 8; i++) {
            float tx = wallMargin + i * tileW;
            canvas.drawLine(tx, floorY, tx, floorY + canvasHeight * 0.06f, tilePaint);
        }
    }

    private void drawDoors(Canvas canvas) {
        float wallMargin = canvasWidth * 0.05f;
        float doorW = canvasWidth * 0.12f;
        float doorH = canvasHeight * 0.22f;
        float floorY = canvasHeight * 0.82f;

        int bgColor = BiomeType.fromRegion(currentRoom.getRegion()).getColor();
        int doorColor = lighten(bgColor, 0.15f);
        doorLabelPaint.setColor(Color.WHITE);

        // West door (left wall)
        if (currentRoom.hasDoor(Direction.WEST)) {
            float dx = wallMargin + canvasWidth * 0.03f;
            float dy = floorY - doorH;
            PlaceholderRenderer.drawDoor(canvas, doorColor, dx, dy, doorW, doorH);
            canvas.drawText("W", dx + doorW / 2, dy - 8, doorLabelPaint);
            doorHitBoxes.add(new DoorHitBox(Direction.WEST,
                    currentRoom.getDoorTarget(Direction.WEST),
                    new RectF(dx, dy, dx + doorW, dy + doorH)));
        }

        // East door (right wall)
        if (currentRoom.hasDoor(Direction.EAST)) {
            float dx = canvasWidth - wallMargin - canvasWidth * 0.03f - doorW;
            float dy = floorY - doorH;
            PlaceholderRenderer.drawDoor(canvas, doorColor, dx, dy, doorW, doorH);
            canvas.drawText("E", dx + doorW / 2, dy - 8, doorLabelPaint);
            doorHitBoxes.add(new DoorHitBox(Direction.EAST,
                    currentRoom.getDoorTarget(Direction.EAST),
                    new RectF(dx, dy, dx + doorW, dy + doorH)));
        }

        // North door (top center)
        if (currentRoom.hasDoor(Direction.NORTH)) {
            float fwdDoorW = doorW * 0.85f;
            float fwdDoorH = doorH * 0.8f;
            float dx = canvasWidth / 2f - fwdDoorW / 2;
            float dy = canvasHeight * 0.15f;
            PlaceholderRenderer.drawDoor(canvas, doorColor, dx, dy, fwdDoorW, fwdDoorH);
            canvas.drawText("N", dx + fwdDoorW / 2, dy - 8, doorLabelPaint);
            doorHitBoxes.add(new DoorHitBox(Direction.NORTH,
                    currentRoom.getDoorTarget(Direction.NORTH),
                    new RectF(dx, dy, dx + fwdDoorW, dy + fwdDoorH)));
        }

        // South door (bottom center)
        if (currentRoom.hasDoor(Direction.SOUTH)) {
            float dx = canvasWidth / 2f - doorW / 2;
            float dy = floorY - doorH;
            PlaceholderRenderer.drawDoor(canvas, doorColor, dx, dy, doorW, doorH);
            canvas.drawText("S", dx + doorW / 2, dy - 8, doorLabelPaint);
            doorHitBoxes.add(new DoorHitBox(Direction.SOUTH,
                    currentRoom.getDoorTarget(Direction.SOUTH),
                    new RectF(dx, dy, dx + doorW, dy + doorH)));
        }
    }

    private void drawObjects(Canvas canvas) {
        if (currentRoom.getObjects() == null) return;

        float roomLeft = canvasWidth * 0.1f;
        float roomRight = canvasWidth * 0.9f;
        float roomTop = canvasHeight * 0.15f;
        float roomBottom = canvasHeight * 0.82f;
        float roomW = roomRight - roomLeft;
        float roomH = roomBottom - roomTop;

        GameAssetManager assetManager = GameAssetManager.getInstance(getContext());
        GameRepository repo = GameRepository.getInstance(getContext());

        for (RoomObject obj : currentRoom.getObjects()) {
            // Skip hidden objects (e.g., concealed traps)
            if (obj.isHidden()) continue;

            float objX = roomLeft + obj.getX() * roomW;
            float objY = roomTop + obj.getY() * roomH;
            float objW = obj.getWidth() * roomW;
            float objH = obj.getHeight() * roomH;

            String shape;
            int color;
            String spritePath = null;

            switch (obj.getType()) {
                case "chest":
                    shape = obj.isOpened() ? "rectangle" : "chest";
                    color = obj.isOpened() ? 0xFF555555 : 0xFF8B4513;
                    break;
                case "creature":
                    if (!obj.isAlive()) continue;
                    shape = "creature";
                    color = 0xFFCC0000;
                    // Look up CreatureDef for sprite and color
                    if (obj.getCreatureDefId() != null) {
                        CreatureDef cDef = repo.getCreatureRegistry()
                                .getCreature(obj.getCreatureDefId());
                        if (cDef != null) {
                            spritePath = cDef.getSpritePath();
                            color = cDef.getPlaceholderColorInt();
                        }
                    }
                    break;
                case "trap":
                    if (obj.isTriggered()) continue;
                    shape = "trap";
                    color = 0xFFFF4444;
                    break;
                case "item":
                    if (obj.getQuantity() <= 0) continue;
                    shape = "circle";
                    color = 0xFFFFD700;
                    // Look up ItemDef for sprite, shape, and color
                    if (obj.getItemId() != null) {
                        ItemDef iDef = repo.getItemRegistry()
                                .getItem(obj.getItemId());
                        if (iDef != null) {
                            spritePath = iDef.getSpritePath();
                            color = iDef.getPlaceholderColorInt();
                            if (iDef.getPlaceholderShape() != null) {
                                shape = iDef.getPlaceholderShape();
                            }
                        }
                    }
                    break;
                case "decoration":
                    shape = getDecorationShape(obj.getSpriteId());
                    color = getDecorationColor(obj.getSpriteId());
                    break;
                default:
                    shape = "rectangle";
                    color = 0xFF888888;
                    break;
            }

            // Try loading actual sprite via spritePath or spriteId, fall back to placeholder
            Bitmap objBmp = null;
            if (spritePath != null) {
                objBmp = assetManager.loadSprite(spritePath);
            }
            if (objBmp == null && obj.getSpriteId() != null) {
                objBmp = assetManager.loadSpriteById(obj.getSpriteId(), "entities/hostile");
            }
            if (objBmp != null) {
                // Render sprites as square using the smaller dimension
                float side = Math.min(objW, objH);
                float cx = objX + objW / 2f;
                float cy = objY + objH / 2f;
                RectF destRect = new RectF(cx - side / 2f, cy - side / 2f,
                        cx + side / 2f, cy + side / 2f);
                canvas.drawBitmap(objBmp, null, destRect, null);
            } else {
                PlaceholderRenderer.drawShape(canvas, shape, color, objX, objY, objW, objH);
            }

            objectHitBoxes.add(new ObjectHitBox(obj,
                    new RectF(objX, objY, objX + objW, objY + objH)));

            // Draw label for interactable decorations
            if ("decoration".equals(obj.getType()) && isInteractableDecoration(obj.getSpriteId())) {
                String label = getDecorationLabel(obj.getSpriteId());
                if (label != null) {
                    PlaceholderRenderer.drawLabel(canvas, label,
                            objX, objY + objH + 18, objW, 18f);
                }
            }
        }
    }

    private static boolean isInteractableDecoration(String spriteId) {
        if (spriteId == null) return false;
        switch (spriteId) {
            case "shop_counter":
            case "storage_chest":
            case "trade_post":
            case "crystal":
                return true;
            default:
                return false;
        }
    }

    private static String getDecorationShape(String spriteId) {
        if (spriteId == null) return "rectangle";
        switch (spriteId) {
            case "crystal": return "gem";
            case "shop_counter": return "chest";
            case "storage_chest": return "chest";
            case "trade_post": return "rectangle";
            default: return "rectangle";
        }
    }

    private static int getDecorationColor(String spriteId) {
        if (spriteId == null) return 0xFF888888;
        switch (spriteId) {
            case "crystal": return 0xFF00CCFF;
            case "shop_counter": return 0xFFDAA520;
            case "storage_chest": return 0xFF8B4513;
            case "trade_post": return 0xFF9932CC;
            default: return 0xFF888888;
        }
    }

    private static String getDecorationLabel(String spriteId) {
        if (spriteId == null) return null;
        switch (spriteId) {
            case "crystal": return "Crystal";
            case "shop_counter": return "Shop";
            case "storage_chest": return "Storage";
            case "trade_post": return "Trade";
            default: return null;
        }
    }

    private void drawRoomInfo(Canvas canvas) {
        // Room ID and zone info
        hudPaint.setTextSize(22f);
        hudPaint.setColor(0xAAFFFFFF);

        String roomInfo = currentRoom.getRoomId();
        if (currentRoom.isWaypoint()) {
            roomInfo += " [WAYPOINT]";
            hudPaint.setColor(0xFF00FF00);
        } else if (currentRoom.isCreatureDen()) {
            roomInfo += " [CREATURE DEN]";
            hudPaint.setColor(0xFFFF4444);
        }
        canvas.drawText(roomInfo, 20, canvasHeight - 20, hudPaint);

        // Biome name
        BiomeType biome = BiomeType.fromRegion(currentRoom.getRegion());
        hudPaint.setColor(0xAAFFFFFF);
        hudPaint.setTextSize(20f);
        int row = RoomIdHelper.getRow(currentRoom.getRoomId());
        int col = RoomIdHelper.getCol(currentRoom.getRoomId());
        String coordLabel = "(" + row + "," + col + ") Tier " + currentRoom.getZone();
        canvas.drawText(biome.getDisplayName() + " - " + coordLabel,
                20, canvasHeight - 48, hudPaint);

        // Waypoint indicator
        if (currentRoom.isWaypoint()) {
            Paint wpPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            wpPaint.setColor(0xFF00FF00);
            wpPaint.setTextSize(32f);
            wpPaint.setTextAlign(Paint.Align.CENTER);
            wpPaint.setShadowLayer(3f, 0, 0, Color.BLACK);
            canvas.drawText("WAYPOINT", canvasWidth / 2f, canvasHeight * 0.95f, wpPaint);
        } else if (currentRoom.getRegion() > 0) {
            // Hot/cold waypoint proximity indicator
            drawWaypointProximity(canvas);
        }
    }

    private void drawWaypointProximity(Canvas canvas) {
        int roomNumber = RoomIdHelper.getRoomNumber(currentRoom.getRoomId());
        int region = currentRoom.getRegion();
        int row = RoomIdHelper.getRow(roomNumber);
        int col = RoomIdHelper.getCol(roomNumber);

        // Manhattan distance to the nearest seeded waypoint in this region
        int[] waypoints = RoomIdHelper.getWaypointSet(region);
        int distance = Integer.MAX_VALUE;
        for (int wp : waypoints) {
            int wr = RoomIdHelper.getRow(wp);
            int wc = RoomIdHelper.getCol(wp);
            int d = Math.abs(row - wr) + Math.abs(col - wc);
            if (d < distance) distance = d;
        }
        if (waypoints.length == 0) distance = 999;

        String label;
        int color;
        if (distance <= 1) {
            label = "SCORCHING";
            color = 0xFFFF0000;
        } else if (distance <= 3) {
            label = "HOT";
            color = 0xFFFF4400;
        } else if (distance <= 6) {
            label = "WARM";
            color = 0xFFFF8800;
        } else if (distance <= 10) {
            label = "COOL";
            color = 0xFF4488FF;
        } else if (distance <= 15) {
            label = "COLD";
            color = 0xFF0044FF;
        } else {
            label = "FREEZING";
            color = 0xFF0000CC;
        }

        Paint proximityPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        proximityPaint.setColor(color);
        proximityPaint.setTextSize(20f);
        proximityPaint.setTextAlign(Paint.Align.RIGHT);
        proximityPaint.setShadowLayer(2f, 1f, 1f, Color.BLACK);
        canvas.drawText("Waypoint: " + label, canvasWidth - 20, canvasHeight - 20, proximityPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN) return super.onTouchEvent(event);

        float tx = event.getX();
        float ty = event.getY();

        // Check doors first
        for (DoorHitBox door : doorHitBoxes) {
            if (door.bounds.contains(tx, ty)) {
                if (listener != null) {
                    listener.onDoorTapped(door.direction, door.targetRoomId);
                }
                return true;
            }
        }

        // Check objects
        for (ObjectHitBox obj : objectHitBoxes) {
            if (obj.bounds.contains(tx, ty)) {
                if (listener != null) {
                    listener.onObjectTapped(obj.object);
                }
                return true;
            }
        }

        // Background tap
        if (listener != null) {
            listener.onBackgroundTapped(tx, ty);
        }
        return true;
    }

    private Bitmap loadAsset(String assetId) {
        if (assetId == null) return null;
        GameAssetManager assetMgr = GameAssetManager.getInstance(getContext());
        return assetMgr.loadSpriteById(assetId, "backgrounds");
    }

    private static int darken(int color, float factor) {
        int r = (int) (Color.red(color) * factor);
        int g = (int) (Color.green(color) * factor);
        int b = (int) (Color.blue(color) * factor);
        return Color.argb(Color.alpha(color), r, g, b);
    }

    private static int lighten(int color, float amount) {
        int r = Math.min(255, Color.red(color) + (int) (255 * amount));
        int g = Math.min(255, Color.green(color) + (int) (255 * amount));
        int b = Math.min(255, Color.blue(color) + (int) (255 * amount));
        return Color.argb(Color.alpha(color), r, g, b);
    }

    private static class DoorHitBox {
        Direction direction;
        String targetRoomId;
        RectF bounds;

        DoorHitBox(Direction direction, String targetRoomId, RectF bounds) {
            this.direction = direction;
            this.targetRoomId = targetRoomId;
            this.bounds = bounds;
        }
    }

    private static class ObjectHitBox {
        RoomObject object;
        RectF bounds;

        ObjectHitBox(RoomObject object, RectF bounds) {
            this.object = object;
            this.bounds = bounds;
        }
    }
}
