package com.larleeloo.jormungandr.util;

import android.graphics.Color;

public final class ColorHelper {
    private ColorHelper() {}

    public static int darken(int color, float factor) {
        int r = (int)(Color.red(color) * factor);
        int g = (int)(Color.green(color) * factor);
        int b = (int)(Color.blue(color) * factor);
        return Color.argb(Color.alpha(color), r, g, b);
    }

    public static int lighten(int color, float amount) {
        int r = Math.min(255, Color.red(color) + (int)(255 * amount));
        int g = Math.min(255, Color.green(color) + (int)(255 * amount));
        int b = Math.min(255, Color.blue(color) + (int)(255 * amount));
        return Color.argb(Color.alpha(color), r, g, b);
    }

    public static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    public static int parseHexColor(String hex) {
        try {
            return (int) Long.parseLong(hex.replace("#", ""), 16) | 0xFF000000;
        } catch (Exception e) {
            return 0xFF888888;
        }
    }
}
