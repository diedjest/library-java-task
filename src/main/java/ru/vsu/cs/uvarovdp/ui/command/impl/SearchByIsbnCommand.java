package ru.vsu.cs.uvarovdp.ui.command.impl;

import ru.vsu.cs.uvarovdp.entity.Book;
import ru.vsu.cs.uvarovdp.service.LibraryService;
import ru.vsu.cs.uvarovdp.ui.command.BookTablePrinter;
import ru.vsu.cs.uvarovdp.ui.command.Command;
import ru.vsu.cs.uvarovdp.ui.command.ConsoleInput;
import ru.vsu.cs.uvarovdp.ui.command.ConsoleOutput;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SearchByIsbnCommand implements Command {

    private final LibraryService service;
    private final ConsoleInput input;
    private final BookTablePrinter printer;

    public SearchByIsbnCommand(LibraryService service, ConsoleInput input, BookTablePrinter printer) {
        this.service = Objects.requireNonNull(service, "Сервис не может быть null");
        this.input = Objects.requireNonNull(input, "ConsoleInput не может быть null");
        this.printer = Objects.requireNonNull(printer, "BookTablePrinter не может быть null");
    }

    @Override
    public void execute() {
        String isbn = input.readNonBlank("Введите ISBN: ");
        Optional<Book> bookOpt = service.searchByIsbn(isbn);
        List<Book> books = bookOpt.stream().toList();
        printer.printBooks(books, "Ничего не найдено.");
        ConsoleOutput.printBlankLine();
    }
}