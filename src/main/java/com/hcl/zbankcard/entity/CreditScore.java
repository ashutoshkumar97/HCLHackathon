package com.hcl.zbankcard.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "credit_scores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CreditScore extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private CreditCardApplication application;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "score_source", nullable = false, length = 100)
    private String scoreSource;

    @Column(name = "card_count", nullable = false)
    private Integer cardCount;
}
