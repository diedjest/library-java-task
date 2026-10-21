package ru.vsu.cs.uvarovdp.util;

public interface IsbnValidator {

    boolean isValid(String isbn);

    String normalize(String isbn);
}