package ru.vsu.cs.uvarovdp.factory;

import ru.vsu.cs.uvarovdp.service.LibraryService;
import ru.vsu.cs.uvarovdp.ui.ConsoleUI;

public final class ConsoleUIFactory {

    private ConsoleUIFactory() {
    }

    public static ConsoleUI create(LibraryService service) {
        return new ConsoleUI(service);
    }
}
