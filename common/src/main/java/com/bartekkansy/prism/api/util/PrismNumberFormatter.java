package com.bartekkansy.prism.api.util;

import java.text.DecimalFormat;

/**
 * Utility for formatting large numbers into human-readable strings.
 * <p>
 * Converts raw values into "Short Scale" notation (e.g., 1,500 becomes 1.5k).
 * </p>
 */
public class PrismNumberFormatter {

    private static final String[] SUFFIXES = {"", "k", "M", "G", "T", "P", "E"};
    private static final DecimalFormat FORMAT = new DecimalFormat("#.#");

    /**
     * Formats a long value with a metric suffix.
     * * @param value The raw number to format.
     * @return A formatted string (e.g., "1.2M").
     */
    public static String format(long value) {
        if (value < 1000) return String.valueOf(value);

        int exp = (int) (Math.log(value) / Math.log(1000));
        // Safety check for suffix bounds
        exp = Math.min(exp, SUFFIXES.length - 1);

        double shortValue = value / Math.pow(1000, exp);
        return FORMAT.format(shortValue) + SUFFIXES[exp];
    }
}