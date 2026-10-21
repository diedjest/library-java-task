package ru.vsu.cs.uvarovdp.ui.command;

public final class ConsoleOutput {

    public static final int WIDTH = 100;
    public static final String SEPARATOR = "-".repeat(WIDTH);
    public static final String HEADER = "=".repeat(WIDTH);

    private ConsoleOutput() {
    }

    public static void printWithGap(String message) {
        System.out.println(message);
        printBlankLine();
    }

    public static void printBlankLine() {
        System.out.println();
    }
}
