package ru.vsu.cs.uvarov_d_p.util.impl;

import ru.vsu.cs.uvarov_d_p.domain.BookConstraints;
import ru.vsu.cs.uvarov_d_p.util.IsbnValidator;

import java.util.Locale;
import java.util.regex.Pattern;

public class IsbnValidatorImpl implements IsbnValidator {

    private static final Pattern ISBN_10_PATTERN =
            Pattern.compile("^\\d{" + (BookConstraints.ISBN_SHORT_LENGTH - 1) + "}[0-9X]$");

    private static final Pattern ISBN_13_PATTERN =
            Pattern.compile("^\\d{" + BookConstraints.ISBN_LONG_LENGTH + "}$");

    @Override
    public String normalize(String isbn) {
        if (isbn == null) {
            return "";
        }
        return isbn.replaceAll("[\\s-]+", "").toUpperCase(Locale.ROOT);
    }

    @Override
    public boolean isValid(String isbn) {
        String normalized = normalize(isbn);
        if (normalized.length() == BookConstraints.ISBN_SHORT_LENGTH) {
            return ISBN_10_PATTERN.matcher(normalized).matches();
        }
        if (normalized.length() == BookConstraints.ISBN_LONG_LENGTH) {
            return ISBN_13_PATTERN.matcher(normalized).matches();
        }
        return false;
    }
}