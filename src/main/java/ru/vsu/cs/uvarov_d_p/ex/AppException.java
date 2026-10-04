package ru.vsu.cs.uvarov_d_p.ex;

public abstract class AppException extends RuntimeException {
    public AppException(String message) {
        super(message);
    }
}
