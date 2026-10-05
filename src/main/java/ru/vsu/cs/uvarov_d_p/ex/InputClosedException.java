package ru.vsu.cs.uvarov_d_p.ex;

public class InputClosedException extends RuntimeException {

    public InputClosedException() {
        super("Ввод завершён");
    }
}