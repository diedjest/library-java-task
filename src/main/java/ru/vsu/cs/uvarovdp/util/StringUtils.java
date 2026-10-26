package ru.vsu.cs.uvarovdp.util;

public final class StringUtils {

    private StringUtils() {
    }

    public static boolean isNullOrBlank(String value) {
        return value == null || value.isBlank();
    }
}
