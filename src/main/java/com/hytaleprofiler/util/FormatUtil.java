package com.hytaleprofiler.util;

import java.text.DecimalFormat;

/**
 * Utility class for formatting values for display.
 */
public final class FormatUtil {
    private static final DecimalFormat DECIMAL_1 = new DecimalFormat("#,##0.0");
    private static final DecimalFormat DECIMAL_2 = new DecimalFormat("#,##0.00");
    private static final DecimalFormat DECIMAL_3 = new DecimalFormat("#,##0.000");
    private static final DecimalFormat INTEGER = new DecimalFormat("#,##0");
    private static final DecimalFormat PERCENT = new DecimalFormat("0.0");

    private FormatUtil() {}

    /**
     * Format milliseconds with appropriate precision.
     */
    public static String formatMs(double ms) {
        if (ms < 0.001) {
            return "<0.001ms";
        } else if (ms < 1.0) {
            return DECIMAL_3.format(ms) + "ms";
        } else if (ms < 10.0) {
            return DECIMAL_2.format(ms) + "ms";
        } else {
            return DECIMAL_1.format(ms) + "ms";
        }
    }

    /**
     * Format nanoseconds to milliseconds.
     */
    public static String formatNsToMs(double ns) {
        return formatMs(ns / 1_000_000.0);
    }

    /**
     * Format bytes to human-readable size.
     */
    public static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + "B";
        } else if (bytes < 1024 * 1024) {
            return DECIMAL_1.format(bytes / 1024.0) + "KB";
        } else if (bytes < 1024L * 1024 * 1024) {
            return DECIMAL_1.format(bytes / (1024.0 * 1024)) + "MB";
        } else {
            return DECIMAL_2.format(bytes / (1024.0 * 1024 * 1024)) + "GB";
        }
    }

    /**
     * Format a count with thousand separators.
     */
    public static String formatCount(int count) {
        return INTEGER.format(count);
    }

    /**
     * Format a count with thousand separators.
     */
    public static String formatCount(long count) {
        return INTEGER.format(count);
    }

    /**
     * Format a percentage value.
     */
    public static String formatPercent(double percent) {
        return PERCENT.format(percent) + "%";
    }

    /**
     * Format TPS value.
     */
    public static String formatTps(double tps) {
        return DECIMAL_1.format(tps) + "/20";
    }

    /**
     * Create a progress bar string.
     */
    public static String progressBar(double percent, int width) {
        int filled = (int) Math.round((percent / 100.0) * width);
        filled = Math.max(0, Math.min(width, filled));
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < width; i++) {
            sb.append(i < filled ? "=" : " ");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Pad a string to a specified width (left-aligned).
     */
    public static String padRight(String s, int width) {
        if (s.length() >= width) return s;
        return s + " ".repeat(width - s.length());
    }

    /**
     * Pad a string to a specified width (right-aligned).
     */
    public static String padLeft(String s, int width) {
        if (s.length() >= width) return s;
        return " ".repeat(width - s.length()) + s;
    }

    /**
     * Extract a simple class name from a fully qualified class name.
     */
    public static String simpleClassName(String fullyQualifiedName) {
        int lastDot = fullyQualifiedName.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < fullyQualifiedName.length() - 1) {
            return fullyQualifiedName.substring(lastDot + 1);
        }
        return fullyQualifiedName;
    }

    /**
     * Extract mod name from a fully qualified class name.
     * Pattern: com.<modname>.* or com.hypixel.hytale.* (core)
     */
    public static String extractModName(String className) {
        if (className == null || className.isEmpty()) {
            return "Unknown";
        }
        if (className.startsWith("com.hypixel.hytale")) {
            return "Hytale (Core)";
        }
        String[] parts = className.split("\\.");
        if (parts.length >= 2) {
            return capitalize(parts[1]);
        }
        return "Unknown";
    }

    /**
     * Capitalize the first letter of a string.
     */
    public static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
