package ru.vsu.cs.uvarov_d_p.ui.command.impl;

import ru.vsu.cs.uvarov_d_p.entity.Book;
import ru.vsu.cs.uvarov_d_p.service.LibraryService;
import ru.vsu.cs.uvarov_d_p.ui.command.Command;
import ru.vsu.cs.uvarov_d_p.ui.command.ConsoleHelper;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SearchByIsbnCommand implements Command {

    private final LibraryService service;
    private final ConsoleHelper helper;

    public SearchByIsbnCommand(LibraryService service, ConsoleHelper helper) {
        this.service = Objects.requireNonNull(service, "Сервис не может быть null");
        this.helper = Objects.requireNonNull(helper, "ConsoleHelper не может быть null");
    }

    @Override
    public void execute() {
        String isbn = helper.readNonBlank("Введите ISBN: ");
        Optional<Book> bookOpt = service.searchByIsbn(isbn);
        List<Book> books = bookOpt.stream().toList();
        helper.printBooks(books, "Ничего не найдено.");
        System.out.println();
    }
}