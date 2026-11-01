package ru.vsu.cs.uvarovdp.util;

import ru.vsu.cs.uvarovdp.entity.Book;

import java.util.Comparator;

public final class BookComparators {

    public static final Comparator<Book> BY_AUTHOR_THEN_TITLE =
            Comparator.comparing((Book book) -> AuthorNameUtils.extractSurname(book.getAuthor()), String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(Book::getAuthor, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(Book::getTitle, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(Book::getIsbn);

    private BookComparators() {
    }
}
