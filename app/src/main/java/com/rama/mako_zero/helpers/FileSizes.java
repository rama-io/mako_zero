package com.rama.mako_zero.helpers;

import java.util.Locale;

    private static final String[] UNITS = {"KB", "MB", "GB", "TB"};

    private FileSizes() {
    }

    public static String format(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double value = bytes;
        int unit = -1;
        do {
            value /= 1024;
            unit++;
        } while (value >= 1024 && unit < UNITS.length - 1);
        String number = value >= 10 ? String.valueOf(Math.round(value)) : String.format(Locale.US, "%.1f", value);
        return number + " " + UNITS[unit];
    }
}
