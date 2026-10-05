package ru.vsu.cs.uvarov_d_p.util;

public interface IsbnValidator {

    boolean isValid(String isbn);

    String normalize(String isbn);
}