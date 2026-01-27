// src/main/java/com/tayperformance/entity/Appointment.java
package com.tayperformance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "appointments",
        indexes = {
                @Index(name = "idx_appt_start_time", columnList = "start_time"),
                @Index(name = "idx_appt_status", columnList = "status"),
                @Index(name = "idx_appt_customer_start", columnList = "customer_id,start_time"),
                @Index(name = "idx_appt_assigned_start", columnList = "assigned_staff_id,start_time")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    // --- Relaties ---
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private DetailService service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_staff_id")
    private User assignedStaff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_user_id")
    private User updatedBy;

    // --- Klant/auto info ---
    @Column(name = "car_brand", nullable = false, length = 80)
    private String carBrand;

    @Column(name = "car_model", length = 80)
    private String carModel;

    @Column(columnDefinition = "TEXT")
    private String description;

    // --- Planning ---
    @Column(name = "start_time", nullable = false)
    private OffsetDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private OffsetDateTime endTime;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    // --- Business ---
    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.REQUESTED;

    // --- Timestamps ---
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
