package com.bartekkansy.prism.api.util;

import java.awt.*;

public class Utilities {
    /**
     * Injects a specific alpha value into an existing color.
     * * @param color The base {@link Color}.
     * @param alpha The desired alpha (0-255).
     * @return An ARGB integer.
     */
    public static int applyAlpha(Color color, int alpha) {
        return (alpha << 24) | (color.getRGB() & 0x00FFFFFF);
    }
}
