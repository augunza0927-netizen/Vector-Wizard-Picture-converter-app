package com.vectorconvert.util;

import java.io.File;


public final class FileUtils {

    private FileUtils() {
    }

    public static boolean hasImageExtension(String name) {
        if (name == null) return false;
        if (name == null) return false;
        String lower = name.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png");
    }

    public static boolean hasZipExtension(String name) {
        if (name == null) return false;
        return name.toLowerCase().endsWith(".zip");
    }

    public static boolean isImageFile(File file) {
        if (file == null) return false;
        return hasImageExtension(file.getName());
    }

    public static boolean isZipFile(File file) {
        if (file == null) return false;
        return hasZipExtension(file.getName());
    }


    public static String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB" };
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        digitGroups = Math.min(digitGroups, units.length - 1);
        double sizeInUnit = bytes / Math.pow(1024, digitGroups);
        return String.format(java.util.Locale.US, "%.1f %s", sizeInUnit, units[digitGroups]);
    }

    // %
    public static String calculateRatio(long originalBytes, long svgBytes) {
        if (originalBytes <= 0 || svgBytes <= 0) return "--";
        double percentage = ((double) svgBytes / originalBytes) * 100.0;
        return String.format(java.util.Locale.US, "%.1f%%", percentage);
    }
}
