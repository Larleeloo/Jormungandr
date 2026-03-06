package com.example.jormungandr.asset;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Utility class for loading and scaling bitmaps from assets.
 */
public class SpriteLoader {

    /**
     * Load and scale a bitmap to fit within the given dimensions.
     */
    public static Bitmap loadScaled(Context context, String assetPath, int targetWidth, int targetHeight) {
        try {
            // First pass: get dimensions
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            InputStream is = context.getAssets().open(assetPath);
            BitmapFactory.decodeStream(is, null, opts);
            is.close();

            // Calculate sample size
            opts.inSampleSize = calculateInSampleSize(opts, targetWidth, targetHeight);

            // Second pass: load scaled
            opts.inJustDecodeBounds = false;
            is = context.getAssets().open(assetPath);
            Bitmap bitmap = BitmapFactory.decodeStream(is, null, opts);
            is.close();

            if (bitmap != null && (bitmap.getWidth() != targetWidth || bitmap.getHeight() != targetHeight)) {
                Bitmap scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
                if (scaled != bitmap) bitmap.recycle();
                return scaled;
            }
            return bitmap;
        } catch (IOException e) {
            return null;
        }
    }

    private static int calculateInSampleSize(BitmapFactory.Options options,
                                              int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}
