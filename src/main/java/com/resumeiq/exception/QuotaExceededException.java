package com.resumeiq.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
public class QuotaExceededException extends RuntimeException {
    public QuotaExceededException() {
        super("You have used all 3 free analyses this month. Upgrade to PRO for unlimited access.");
    }
}