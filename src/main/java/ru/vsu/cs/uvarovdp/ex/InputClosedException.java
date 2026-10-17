package ru.vsu.cs.uvarovdp.ex;

public class InputClosedException extends RuntimeException {

    public InputClosedException() {
        super("Ввод завершён");
    }
}