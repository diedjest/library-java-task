package ru.vsu.cs.uvarovdp.dto;

import java.util.List;

public record BookData(String title, String author, String isbn, List<String> genres) {
}
