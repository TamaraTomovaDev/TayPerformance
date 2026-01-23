package com.tayperformance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "customers")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String phone;

    private String firstName;

    @Builder.Default // Zorgt dat de default 'true' blijft bij gebruik van builder
    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    private OffsetDateTime createdAt; // Aangepast naar OffsetDateTime

    @UpdateTimestamp
    private OffsetDateTime updatedAt; // Aangepast naar OffsetDateTime

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Appointment> appointments;
}