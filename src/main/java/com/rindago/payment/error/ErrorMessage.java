package com.rindago.payment.error;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
@JsonIgnoreProperties(ignoreUnknown=true)
public class ErrorMessage {
    private ErrorType type;
    private List<ErrorInfo> errors;
}
