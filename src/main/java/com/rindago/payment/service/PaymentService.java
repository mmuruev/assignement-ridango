package com.rindago.payment.service;

import com.rindago.payment.domain.Account;
import com.rindago.payment.domain.Payment;
import com.rindago.payment.repository.AccountRepository;
import com.rindago.payment.repository.PaymentRepository;
import com.sun.tools.javac.util.List;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public void performTransaction(BigDecimal amount, Long fromAccountId, Long toAccountId) {
        Account senderAccount = accountRepository.findById(fromAccountId).orElseThrow(() -> new RuntimeException("Sender account not found " + fromAccountId));
        val senderAccountBalance = senderAccount.getBalance();

        if (amount.compareTo(senderAccountBalance) > 0) {
            throw new RuntimeException("Sender balance is not enough");
        }

        Account receiverAccount = accountRepository.findById(toAccountId).orElseThrow(() -> new RuntimeException("Receiver account not found " + fromAccountId));

        senderAccount.setBalance(senderAccountBalance.subtract(amount));
        receiverAccount.setBalance(receiverAccount.getBalance().add(amount));

        final Payment paymentTransaction = Payment.builder()
                .senderAccount(senderAccount)
                .receiverAccount(receiverAccount)
                .build();

        paymentRepository.save(paymentTransaction);
        accountRepository.saveAll(List.of(senderAccount, receiverAccount));
    }
}
