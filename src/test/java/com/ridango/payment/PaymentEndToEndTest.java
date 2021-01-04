package com.ridango.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rindago.payment.PaymentApplication;
import com.rindago.payment.domain.Account;
import com.rindago.payment.dto.PaymentRequestDTO;
import com.rindago.payment.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = PaymentApplication.class)
@AutoConfigureMockMvc
public class PaymentEndToEndTest {
    @Autowired
    private MockMvc restMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ObjectMapper mapper;

    @BeforeEach
    public void initTest() {
    }

    @Test
    public void testTransactionFlow() throws Exception {
        // Initialize the database
        Account senderUserAccount = new Account(null, "Test@User", BigDecimal.valueOf(100));
        Account receiverUserAccount = new Account(null, "Ridango@User", BigDecimal.valueOf(100));

        accountRepository.saveAll(List.of(senderUserAccount, receiverUserAccount));
        accountRepository.flush();

        PaymentRequestDTO requestDTO = new PaymentRequestDTO(senderUserAccount.getId(), receiverUserAccount.getId(), BigDecimal.valueOf(100));

        // Make transaction
        restMvc.perform(post("/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(requestDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.transactionId").isNumber())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());

        Account account = accountRepository.findById(senderUserAccount.getId()).orElseThrow();
        assertThat(account.getBalance().stripTrailingZeros()).isEqualTo(BigDecimal.ZERO);

        Account accountReceiver = accountRepository.findById(receiverUserAccount.getId()).orElseThrow();
        assertThat(accountReceiver.getBalance().longValue()).isEqualTo(200);
    }

    @Test
    public void testTransactionFlow_not_enough_points() throws Exception {
        // Initialize the database
        Account senderUserAccount = new Account(null, "Test+1@User", BigDecimal.valueOf(99));
        Account receiverUserAccount = new Account(null, "Ridango+1@User", BigDecimal.valueOf(100));

        accountRepository.saveAll(List.of(senderUserAccount, receiverUserAccount));
        accountRepository.flush();

        PaymentRequestDTO requestDTO = new PaymentRequestDTO(senderUserAccount.getId(), receiverUserAccount.getId(), BigDecimal.valueOf(100));

        // Make transaction
        restMvc.perform(post("/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(requestDTO)))
                .andDo(print())
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.type").value(equalTo("TRANSACTION_ERROR")))
                .andExpect(jsonPath("$.errors[0].code").value(equalTo("NOT_ENOUGH_AMOUNT")))
        ;

        Account account = accountRepository.findById(senderUserAccount.getId()).orElseThrow();
        assertThat(account.getBalance().stripTrailingZeros()).isEqualTo(BigDecimal.valueOf(99));

        Account accountReceiver = accountRepository.findById(receiverUserAccount.getId()).orElseThrow();
        assertThat(accountReceiver.getBalance().longValue()).isEqualTo(100);
    }

    @Test
    public void testTransactionFlow_not_negative_points() throws Exception {
        // Initialize the database
        Account senderUserAccount = new Account(null, "Test+20@User", BigDecimal.valueOf(100));
        Account receiverUserAccount = new Account(null, "Ridango+20@User", BigDecimal.valueOf(100));

        accountRepository.saveAll(List.of(senderUserAccount, receiverUserAccount));
        accountRepository.flush();

        PaymentRequestDTO requestDTO = new PaymentRequestDTO(senderUserAccount.getId(), receiverUserAccount.getId(), BigDecimal.valueOf(-1));

        // Make transaction
        restMvc.perform(post("/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(requestDTO)))
                .andDo(print())
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.type").value(equalTo("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.errors[0].code").value(equalTo("must be greater than or equal to 0")))
                .andExpect(jsonPath("$.errors[0].field").value(equalTo("amount")))
        ;

        Account account = accountRepository.findById(senderUserAccount.getId()).orElseThrow();
        assertThat(account.getBalance().longValue()).isEqualTo(100);

        Account accountReceiver = accountRepository.findById(receiverUserAccount.getId()).orElseThrow();
        assertThat(accountReceiver.getBalance().longValue()).isEqualTo(100);
    }

    @Test
    public void testTransactionFlow_not_receiver() throws Exception {
        // Initialize the database
        Account senderUserAccount = new Account(null, "Test+3@User", BigDecimal.valueOf(100));

        accountRepository.save(senderUserAccount);
        accountRepository.flush();

        PaymentRequestDTO requestDTO = new PaymentRequestDTO(senderUserAccount.getId(), null, BigDecimal.valueOf(100));

        // Make transaction
        restMvc.perform(post("/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(requestDTO)))
                .andDo(print())
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.type").value(equalTo("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.errors[0].code").value(equalTo("must not be null")))
                .andExpect(jsonPath("$.errors[0].field").value(equalTo("receiverAccountId")))
        ;

        Account account = accountRepository.findById(senderUserAccount.getId()).orElseThrow();
        assertThat(account.getBalance().longValue()).isEqualTo(100);
    }

    @Test
    public void testTransactionFlow_no_sender_id() throws Exception {
        PaymentRequestDTO requestDTO = new PaymentRequestDTO(-1L, -1L, BigDecimal.valueOf(100));

        // Make transaction
        restMvc.perform(post("/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(requestDTO)))
                .andDo(print())
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.type").value(equalTo("TRANSACTION_ERROR")))
                .andExpect(jsonPath("$.errors[0].code").value(equalTo("NOT_FOUND_OWNER")))
        ;

    }

    @Test
    public void testTransactionFlow_no_receiver_id() throws Exception {
        // Initialize the database
        Account senderUserAccount = new Account(null, "Test+36@User", BigDecimal.valueOf(100));

        accountRepository.save(senderUserAccount);
        accountRepository.flush();

        PaymentRequestDTO requestDTO = new PaymentRequestDTO(senderUserAccount.getId(), -1L, BigDecimal.valueOf(100));

        // Make transaction
        restMvc.perform(post("/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(requestDTO)))
                .andDo(print())
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.type").value(equalTo("TRANSACTION_ERROR")))
                .andExpect(jsonPath("$.errors[0].code").value(equalTo("NOT_FOUND_OWNER")))
        ;
    }

    @Test
    public void testTransactionFlow_not_transaction_amount() throws Exception {
        // Initialize the database
        Account senderUserAccount = new Account(null, "Test+2@User", BigDecimal.valueOf(100));
        Account receiverUserAccount = new Account(null, "Ridango+2@User", BigDecimal.valueOf(100));

        accountRepository.saveAll(List.of(senderUserAccount, receiverUserAccount));
        accountRepository.flush();

        PaymentRequestDTO requestDTO = new PaymentRequestDTO(senderUserAccount.getId(), receiverUserAccount.getId(), null);

        // Make transaction
        restMvc.perform(post("/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(requestDTO)))
                .andDo(print())
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.type").value(equalTo("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.errors[0].code").value(equalTo("must not be null")))
                .andExpect(jsonPath("$.errors[0].field").value(equalTo("amount")))
        ;

        Account account = accountRepository.findById(senderUserAccount.getId()).orElseThrow();
        assertThat(account.getBalance().longValue()).isEqualTo(100);

        Account accountReceiver = accountRepository.findById(receiverUserAccount.getId()).orElseThrow();
        assertThat(accountReceiver.getBalance().longValue()).isEqualTo(100);
    }
}
