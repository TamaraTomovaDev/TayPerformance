package com.tayperformance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "appointments",
        indexes = {
                @Index(name = "idx_appointment_time", columnList = "startTime,endTime")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relatie naar klant (verplicht)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false)
    private String carBrand; // Merk verplicht

    private String carModel; // Model optioneel

    @Column(columnDefinition = "TEXT")
    private String description; // Vrije tekst

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price; // Geld = BigDecimal

    @Column(nullable = false)
    private LocalDateTime startTime; // Starttijd

    @Column(nullable = false)
    private LocalDateTime endTime; // Eindtijd

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
