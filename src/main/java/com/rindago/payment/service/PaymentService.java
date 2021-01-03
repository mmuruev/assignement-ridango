package com.rindago.payment.service;

import com.rindago.payment.domain.Account;
import com.rindago.payment.domain.Payment;
import com.rindago.payment.exceptions.TransactionException;
import com.rindago.payment.repository.AccountRepository;
import com.rindago.payment.repository.PaymentRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public Transaction performTransaction(BigDecimal amount, Long fromAccountId, Long toAccountId) {

        if (BigDecimal.ZERO.compareTo(amount) > 0) {
            throw new TransactionException("Cant be less then 0", TransactionException.TransactionErrorCode.ZERO_AMOUNT);
        }
        
        Account senderAccount = accountRepository.findById(fromAccountId)
                .orElseThrow(() -> new TransactionException("Sender account not found " + fromAccountId, TransactionException.TransactionErrorCode.NOT_FOUND_OWNER));
        val senderAccountBalance = senderAccount.getBalance();

        if (amount.compareTo(senderAccountBalance) > 0) {
            throw new TransactionException("Sender balance is not enough", TransactionException.TransactionErrorCode.NOT_ENOUGH_AMOUNT);
        }

        Account receiverAccount = accountRepository.findById(toAccountId)
                .orElseThrow(() -> new TransactionException("Receiver account not found " + fromAccountId, TransactionException.TransactionErrorCode.NOT_FOUND_OWNER));

        senderAccount.setBalance(senderAccountBalance.subtract(amount));
        receiverAccount.setBalance(receiverAccount.getBalance().add(amount));

        final Payment paymentTransaction = Payment.builder()
                .senderAccount(senderAccount)
                .receiverAccount(receiverAccount)
                .timestamp(Instant.now())
                .build();

        paymentRepository.saveAndFlush(paymentTransaction);
        accountRepository.saveAll(List.of(senderAccount, receiverAccount));
        accountRepository.flush();

        return new Transaction(paymentTransaction.getId(), paymentTransaction.getTimestamp());
    }

    @AllArgsConstructor
    @Getter
    public static class Transaction {
        private final Long transactionId;
        private final Instant timestamp;
    }
}
