package ru.vsu.cs.uvarovdp.ex;

public class BusinessRuleException extends AppException {

    private final BusinessRuleType type;

    public BusinessRuleException(BusinessRuleType type, String message) {
        super(message);
        this.type = type;
    }

    public BusinessRuleType getType() {
        return type;
    }
}
