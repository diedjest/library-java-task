package ru.vsu.cs.uvarovdp.util;

public final class AuthorNameUtils {

    private AuthorNameUtils() {
    }

    // Суффиксы вроде «мл.» не обрабатываются
    public static String extractSurname(String author) {
        if (StringUtils.isNullOrBlank(author)) {
            return "";
        }
        String[] parts = author.trim().split("\\s+");
        return parts[parts.length - 1];
    }
}
