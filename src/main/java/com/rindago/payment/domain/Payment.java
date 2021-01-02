package com.rindago.payment.domain;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.Instant;

@Entity
@Table(name = "payment")
@Data
@Builder
@NoArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @ManyToOne(optional = false)
    @NotNull
    private Account senderAccount;

    @ManyToOne(optional = false)
    @NotNull
    private Account receiverAccount;

    @NotNull
    @Column(nullable = false)
    private Instant timestamp;
}
