package ru.vsu.cs.uvarovdp.factory;

import ru.vsu.cs.uvarovdp.util.IsbnValidator;
import ru.vsu.cs.uvarovdp.util.impl.IsbnValidatorImpl;

public final class IsbnValidatorFactory {

    private IsbnValidatorFactory() {
    }

    public static IsbnValidator create() {
        return new IsbnValidatorImpl();
    }
}
