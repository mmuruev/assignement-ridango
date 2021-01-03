package com.rindago.payment.controller;

import com.rindago.payment.dto.PaymentRequestDTO;
import com.rindago.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/payment")
    public ResponseEntity<PaymentService.Transaction> makePayment(@Valid @RequestBody PaymentRequestDTO paymentRequestDTO) {

        PaymentService.Transaction transaction = paymentService.performTransaction(
                paymentRequestDTO.getAmount(),
                paymentRequestDTO.getSenderAccountId(),
                paymentRequestDTO.getReceiverAccountId()
        );

        return ResponseEntity.ok()
                .body(transaction);
    }
}
