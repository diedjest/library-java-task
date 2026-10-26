package ru.vsu.cs.uvarovdp.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.vsu.cs.uvarovdp.entity.Book;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Тестирование BookComparators")
class BookComparatorsTest {

    private Book book(String title, String author, String isbn) {
        return new Book(title, author, isbn, List.of("Роман"));
    }

    private List<String> sortedTitles(Book... books) {
        List<Book> list = new ArrayList<>(List.of(books));
        list.sort(BookComparators.BY_AUTHOR_THEN_TITLE);
        return list.stream().map(Book::getTitle).toList();
    }

    @Test
    @DisplayName("Книги сортируются по фамилии автора, а не по имени")
    void shouldSortBySurname() {
        Book tolstoy = book("Война и мир", "Лев Толстой", "9785041166410");
        Book gogol = book("Мёртвые души", "Николай Гоголь", "9785041166434");
        Book bulgakov = book("Мастер и Маргарита", "Михаил Булгаков", "9785041166427");

        assertEquals(List.of("Мастер и Маргарита", "Мёртвые души", "Война и мир"), sortedTitles(tolstoy, gogol, bulgakov));
    }

    @Test
    @DisplayName("Сортировка по фамилии не зависит от регистра")
    void shouldIgnoreCaseInSurname() {
        Book lower = book("Б", "иван абрамов", "9785041166410");
        Book upper = book("А", "Пётр Борисов", "9785041166427");

        assertEquals(List.of("Б", "А"), sortedTitles(upper, lower));
    }

    @Test
    @DisplayName("При одинаковой фамилии книги сортируются по полному имени автора")
    void shouldSortByFullAuthorWhenSurnameEqual() {
        Book second = book("Б", "Пётр Иванов", "9785041166410");
        Book first = book("А", "Алексей Иванов", "9785041166427");

        assertEquals(List.of("А", "Б"), sortedTitles(second, first));
    }

    @Test
    @DisplayName("При одинаковом авторе книги сортируются по названию")
    void shouldSortByTitleWhenAuthorEqual() {
        Book war = book("Война и мир", "Лев Толстой", "9785041166410");
        Book anna = book("Анна Каренина", "Лев Толстой", "9785041166427");

        assertEquals(List.of("Анна Каренина", "Война и мир"), sortedTitles(war, anna));
    }

    @Test
    @DisplayName("При одинаковых авторе и названии книги сортируются по ISBN")
    void shouldSortByIsbnAsLastResort() {
        Book high = book("Том", "Лев Толстой", "9785041166427");
        Book low = book("Том", "Лев Толстой", "9785041166410");

        List<Book> list = new ArrayList<>(List.of(high, low));
        list.sort(BookComparators.BY_AUTHOR_THEN_TITLE);

        assertEquals(List.of("9785041166410", "9785041166427"), list.stream().map(Book::getIsbn).toList());
    }
}
