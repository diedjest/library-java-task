package ru.vsu.cs.uvarovdp.ex;

public abstract class AppException extends RuntimeException {
    public AppException(String message) {
        super(message);
    }
}
