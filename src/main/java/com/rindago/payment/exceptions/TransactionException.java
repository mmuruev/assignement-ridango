package com.rindago.payment.exceptions;


import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
@Getter
public class TransactionException extends RuntimeException {

    private final String errorCode;

    public TransactionException(String message, TransactionErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode.name();
    }


    public  enum TransactionErrorCode{
        NOT_FOUND_OWNER, NOT_ENOUGH_AMOUNT, ZERO_AMOUNT
    }
}
