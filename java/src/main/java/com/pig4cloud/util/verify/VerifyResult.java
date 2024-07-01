package com.pig4cloud.util.verify;

public class VerifyResult {
    private final boolean valid;
    private final String message;

    private VerifyResult(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
    }

    public static VerifyResult valid(String message) {
        return new VerifyResult(true, message);
    }

    public static VerifyResult invalid(String message) {
        return new VerifyResult(false, message);
    }

    public boolean isValid() {
        return valid;
    }

    public String getMessage() {
        return message;
    }
}
