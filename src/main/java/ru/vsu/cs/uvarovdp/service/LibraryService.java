package ru.vsu.cs.uvarovdp.service;

import ru.vsu.cs.uvarovdp.dto.BookData;
import ru.vsu.cs.uvarovdp.entity.Book;
import ru.vsu.cs.uvarovdp.ex.BusinessRuleException;
import ru.vsu.cs.uvarovdp.ex.NotFoundException;
import ru.vsu.cs.uvarovdp.ex.ValidationException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LibraryService {

    /**
     * Регистрирует новую книгу в каталоге библиотеки.
     *
     * @param data название, автор, ISBN и жанры книги
     * @return сохранённая книга
     * @throws ValidationException   если переданные данные некорректны (пустые поля, неверный формат ISBN, неверное число жанров)
     * @throws BusinessRuleException если книга с указанным ISBN уже существует в каталоге
     */
    Book addBook(BookData data);

    /**
     * Редактирует основные сведения о существующей книге.
     *
     * @param bookId идентификатор книги
     * @param data   новые название, автор, ISBN и жанры
     * @return обновлённая книга
     * @throws NotFoundException     если книга с указанным ID не найдена
     * @throws ValidationException   если переданные данные некорректны
     * @throws BusinessRuleException если указанный ISBN уже занят другой книгой
     */
    Book editBook(UUID bookId, BookData data);

    /**
     * Удаляет книгу из каталога по идентификатору.
     *
     * @param bookId идентификатор книги
     * @throws NotFoundException     если книга с указанным ID не найдена
     * @throws BusinessRuleException если книга в данный момент выдана читателю
     */
    void deleteBook(UUID bookId);

    /**
     * Добавляет дополнительный жанр к существующей книге.
     *
     * @param bookId идентификатор книги
     * @param genre  название жанра
     * @return обновлённая книга
     * @throws NotFoundException     если книга с указанным ID не найдена
     * @throws ValidationException   если жанр пустой или уже присутствует у книги
     * @throws BusinessRuleException если превышен лимит количества жанров
     */
    Book addGenre(UUID bookId, String genre);

    /**
     * Оформляет выдачу книги читателю.
     *
     * @param bookId       идентификатор книги
     * @param borrowerName имя читателя
     * @return обновлённая книга
     * @throws NotFoundException     если книга с указанным ID не найдена
     * @throws BusinessRuleException если книга уже выдана или имя читателя пустое
     */
    Book borrowBook(UUID bookId, String borrowerName);

    /**
     * Принимает выданную книгу обратно в библиотеку.
     *
     * @param bookId идентификатор книги
     * @return обновлённая книга
     * @throws NotFoundException     если книга с указанным ID не найдена
     * @throws BusinessRuleException если книга не числится выданной
     */
    Book returnBook(UUID bookId);

    /**
     * Возвращает книгу по её уникальному идентификатору.
     *
     * @param bookId идентификатор книги
     * @return найденная книга
     * @throws NotFoundException если книга с указанным ID не найдена
     */
    Book getBookById(UUID bookId);

    /**
     * Возвращает список всех книг каталога, отсортированный по автору и названию.
     *
     * @return неизменяемый список всех книг
     */
    List<Book> getAllBooks();

    /**
     * Выполняет поиск книг по автору без учёта регистра.
     *
     * @param query подстрока для поиска (при пустом запросе возвращаются все книги)
     * @return неизменяемый список найденных книг
     */
    List<Book> searchByAuthor(String query);

    /**
     * Выполняет точный поиск книги по её ISBN.
     *
     * @param isbn номер ISBN
     * @return Optional с найденной книгой, либо пустой Optional
     * @throws ValidationException если формат переданного ISBN некорректен
     */
    Optional<Book> searchByIsbn(String isbn);

    /**
     * Проверяет корректность формата ISBN и его доступность для использования в каталоге.
     *
     * @param isbn          номер ISBN для проверки
     * @param excludeBookId идентификатор книги, исключаемой из проверки уникальности (null для новой книги)
     * @throws ValidationException   если передан некорректный формат ISBN
     * @throws BusinessRuleException если книга с указанным ISBN уже существует в каталоге
     */
    void checkIsbnAvailable(String isbn, UUID excludeBookId);
}