// src/main/java/com/tayperformance/entity/SmsLog.java
package com.tayperformance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "sms_logs",
        indexes = {
                @Index(name = "idx_sms_appt", columnList = "appointment_id"),
                @Index(name = "idx_sms_status", columnList = "status"),
                @Index(name = "idx_sms_created", columnList = "created_at")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SmsLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SmsType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SmsStatus status = SmsStatus.QUEUED;

    @Column(name = "to_phone", nullable = false, length = 30)
    private String toPhone;

    @Column(name = "provider_message_id", length = 120)
    private String providerMessageId;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
