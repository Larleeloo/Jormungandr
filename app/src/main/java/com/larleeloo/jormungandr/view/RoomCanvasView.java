package com.larleeloo.jormungandr.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.larleeloo.jormungandr.model.BiomeType;
import com.larleeloo.jormungandr.model.Direction;
import com.larleeloo.jormungandr.model.Room;
import com.larleeloo.jormungandr.model.RoomObject;

import java.io.IOException;
import java.io.InputStream;
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
            bgBitmap.recycle();
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
        float doorBottomY = floorY;

        int bgColor = BiomeType.fromRegion(currentRoom.getRegion()).getColor();
        int doorColor = lighten(bgColor, 0.15f);

        // Left door
        if (currentRoom.hasDoor(Direction.LEFT)) {
            float dx = wallMargin + canvasWidth * 0.03f;
            float dy = doorBottomY - doorH;
            PlaceholderRenderer.drawDoor(canvas, doorColor, dx, dy, doorW, doorH);
            doorLabelPaint.setColor(Color.WHITE);
            canvas.drawText("LEFT", dx + doorW / 2, dy - 8, doorLabelPaint);
            doorHitBoxes.add(new DoorHitBox(Direction.LEFT,
                    currentRoom.getDoorTarget(Direction.LEFT),
                    new RectF(dx, dy, dx + doorW, dy + doorH)));
        }

        // Right door
        if (currentRoom.hasDoor(Direction.RIGHT)) {
            float dx = canvasWidth - wallMargin - canvasWidth * 0.03f - doorW;
            float dy = doorBottomY - doorH;
            PlaceholderRenderer.drawDoor(canvas, doorColor, dx, dy, doorW, doorH);
            canvas.drawText("RIGHT", dx + doorW / 2, dy - 8, doorLabelPaint);
            doorHitBoxes.add(new DoorHitBox(Direction.RIGHT,
                    currentRoom.getDoorTarget(Direction.RIGHT),
                    new RectF(dx, dy, dx + doorW, dy + doorH)));
        }

        // Forward door (centered, slightly higher)
        if (currentRoom.hasDoor(Direction.FORWARD)) {
            float dx = canvasWidth / 2f - doorW / 2;
            float dy = canvasHeight * 0.15f;
            float fwdDoorH = doorH * 0.8f;
            float fwdDoorW = doorW * 0.85f;
            dx = canvasWidth / 2f - fwdDoorW / 2;
            PlaceholderRenderer.drawDoor(canvas, doorColor, dx, dy, fwdDoorW, fwdDoorH);
            canvas.drawText("FORWARD", dx + fwdDoorW / 2, dy - 8, doorLabelPaint);
            doorHitBoxes.add(new DoorHitBox(Direction.FORWARD,
                    currentRoom.getDoorTarget(Direction.FORWARD),
                    new RectF(dx, dy, dx + fwdDoorW, dy + fwdDoorH)));
        }

        // Back door (bottom center, like entrance)
        if (currentRoom.hasDoor(Direction.BACK)) {
            float dx = canvasWidth / 2f - doorW * 0.6f / 2;
            float dy = floorY - doorH * 0.5f;
            float backDoorW = doorW * 0.6f;
            float backDoorH = doorH * 0.5f;

            // Draw a simpler "back" indicator
            Paint backPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            backPaint.setColor(doorColor);
            backPaint.setAlpha(180);
            canvas.drawRoundRect(new RectF(dx, dy, dx + backDoorW, dy + backDoorH),
                    8f, 8f, backPaint);

            // Arrow pointing down
            Paint arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            arrowPaint.setColor(Color.WHITE);
            arrowPaint.setTextSize(28f);
            arrowPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("\u2193 BACK", dx + backDoorW / 2, dy + backDoorH / 2 + 10, arrowPaint);

            doorHitBoxes.add(new DoorHitBox(Direction.BACK,
                    currentRoom.getDoorTarget(Direction.BACK),
                    new RectF(dx, dy, dx + backDoorW, dy + backDoorH)));
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

        for (RoomObject obj : currentRoom.getObjects()) {
            // Skip hidden objects (e.g., concealed traps)
            if (obj.isHidden()) continue;

            float objX = roomLeft + obj.getX() * roomW;
            float objY = roomTop + obj.getY() * roomH;
            float objW = obj.getWidth() * roomW;
            float objH = obj.getHeight() * roomH;

            String shape;
            int color;

            switch (obj.getType()) {
                case "chest":
                    shape = obj.isOpened() ? "rectangle" : "chest";
                    color = obj.isOpened() ? 0xFF555555 : 0xFF8B4513;
                    break;
                case "creature":
                    if (!obj.isAlive()) continue;
                    shape = "creature";
                    color = 0xFFCC0000;
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
                    break;
                case "decoration":
                    shape = "rectangle";
                    color = 0xFF888888;
                    break;
                default:
                    shape = "rectangle";
                    color = 0xFF888888;
                    break;
            }

            PlaceholderRenderer.drawShape(canvas, shape, color, objX, objY, objW, objH);

            objectHitBoxes.add(new ObjectHitBox(obj,
                    new RectF(objX, objY, objX + objW, objY + objH)));
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
        canvas.drawText(biome.getDisplayName() + " - Zone " + currentRoom.getZone(),
                20, canvasHeight - 48, hudPaint);

        // Waypoint indicator
        if (currentRoom.isWaypoint()) {
            Paint wpPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            wpPaint.setColor(0xFF00FF00);
            wpPaint.setTextSize(32f);
            wpPaint.setTextAlign(Paint.Align.CENTER);
            wpPaint.setShadowLayer(3f, 0, 0, Color.BLACK);
            canvas.drawText("WAYPOINT", canvasWidth / 2f, canvasHeight * 0.95f, wpPaint);
        }
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
        String[] paths = {
                "backgrounds/" + assetId + ".png",
                "backgrounds/" + assetId + ".gif",
        };
        for (String path : paths) {
            try {
                InputStream is = getContext().getAssets().open(path);
                Bitmap bmp = BitmapFactory.decodeStream(is);
                is.close();
                return bmp;
            } catch (IOException ignored) {}
        }
        return null;
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
