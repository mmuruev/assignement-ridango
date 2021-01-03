package com.rindago.payment.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class PaymentRequestDTO {
    @NotNull
    private  Long senderAccountId;

    @NotNull
    private  Long receiverAccountId;

    @NotNull
    @Min(value = 0)
    private BigDecimal amount;
}
