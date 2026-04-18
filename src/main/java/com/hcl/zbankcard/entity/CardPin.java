package com.hcl.zbankcard.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "card_pins")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CardPin extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false, unique = true)
    private CreditCard creditCard;

    /**
     * PIN stored as a BCrypt hash. Never store or log plain-text PINs.
     */
    @Column(name = "pin_hash", nullable = false, length = 255)
    private String pinHash;

    @Builder.Default
    @Column(name = "first_time_login", nullable = false)
    private boolean firstTimeLogin = true;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Builder.Default
    @Column(name = "failed_attempt_count", nullable = false)
    private Integer failedAttemptCount = 0;

    @Column(name = "last_failed_attempt_at")
    private LocalDateTime lastFailedAttemptAt;
}
