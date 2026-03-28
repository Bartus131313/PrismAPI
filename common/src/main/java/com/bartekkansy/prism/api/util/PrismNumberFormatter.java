package com.bartekkansy.prism.api.util;

public class PrismNumberFormatter {
    /**
     * Formats a long value into a human-readable string (e.g., 1500 -> 1.5k).
     * @param value The raw value (Energy, Fluid, Money, etc.)
     * @return A formatted string with suffixes (k, M, G, T).
     */
    public static String format(long value) {
        if (value < 1000) return String.valueOf(value);
        int exp = (int) (Math.log(value) / Math.log(1000));
        char unit = "kMGT".charAt(exp - 1);
        return String.format("%.1f%c", value / Math.pow(1000, exp), unit);
    }
}