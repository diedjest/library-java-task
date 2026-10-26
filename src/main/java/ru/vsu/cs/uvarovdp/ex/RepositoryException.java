package ru.vsu.cs.uvarovdp.ex;

public class RepositoryException extends AppException {
    public RepositoryException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }
}
