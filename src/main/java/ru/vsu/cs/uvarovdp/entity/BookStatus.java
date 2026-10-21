package ru.vsu.cs.uvarovdp.entity;

public enum BookStatus {
    AVAILABLE("В наличии"),
    BORROWED("Выдана");

    private final String title;

    BookStatus(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
