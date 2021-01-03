package com.rindago.payment.controller;

import com.rindago.payment.error.ErrorInfo;
import com.rindago.payment.error.ErrorMessage;
import com.rindago.payment.error.ErrorType;
import com.rindago.payment.exceptions.TransactionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class ExceptionController {
    private final MessageSource messageSource;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ErrorMessage processValidationError(MethodArgumentNotValidException ex) {
        BindingResult result = ex.getBindingResult();

        List<ErrorInfo> errors = result.getFieldErrors().stream()
                .map(this::processFieldError)
                .collect(Collectors.toList());

        log.error("Request error {}", errors);

        return ErrorMessage.builder()
                .type(ErrorType.VALIDATION_ERROR)
                .errors(errors)
                .build();
    }

    @ExceptionHandler(TransactionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ErrorMessage processTransactionError(TransactionException ex) {
        ErrorInfo errorInfo = ErrorInfo.builder()
                .message(ex.getMessage())
                .code(ex.getErrorCode())
                .build();

        log.error("Request error {}", errorInfo);

        return ErrorMessage.builder()
                .type(ErrorType.TRANSACTION_ERROR)
                .errors(List.of(errorInfo))
                .build();
    }

    private ErrorInfo processFieldError(FieldError error) {
        if (error == null) {
            return null;
        }

        String msg = messageSource.getMessage(Objects.requireNonNull(error.getDefaultMessage()), null, "", LocaleContextHolder.getLocale());

        return ErrorInfo.builder()
                .message(msg == null || msg.isEmpty() ? null : msg)
                .field(error.getField())
                .code(error.getDefaultMessage())
                .build();
    }
}