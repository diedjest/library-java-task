package ru.vsu.cs.uvarovdp.ui.command.impl;

import ru.vsu.cs.uvarovdp.service.LibraryService;
import ru.vsu.cs.uvarovdp.ui.command.BookTablePrinter;
import ru.vsu.cs.uvarovdp.ui.command.Command;
import ru.vsu.cs.uvarovdp.ui.command.ConsoleOutput;

import java.util.Objects;

public class ShowAllCommand implements Command {

    private final LibraryService service;
    private final BookTablePrinter printer;

    public ShowAllCommand(LibraryService service, BookTablePrinter printer) {
        this.service = Objects.requireNonNull(service, "Сервис не может быть null");
        this.printer = Objects.requireNonNull(printer, "BookTablePrinter не может быть null");
    }

    @Override
    public void execute() {
        printer.printBooks(service.getAllBooks(), "Каталог пуст.");
        ConsoleOutput.printBlankLine();
    }
}