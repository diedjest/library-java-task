package ru.vsu.cs.uvarov_d_p.entity;

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
