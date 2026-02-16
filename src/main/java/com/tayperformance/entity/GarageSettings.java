package com.tayperformance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "garage_settings")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class GarageSettings {

    @Id
    private Long id; // altijd 1

    @Column(name = "garage_name", nullable = false, length = 120)
    private String garageName;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(length = 40)
    private String phone;

    @Column(name = "kvk_number", length = 40)
    private String kvkNumber;

    @Column(name = "logo_url", length = 255)
    private String logoUrl;

    @Column(name = "template_confirmation", nullable = false, columnDefinition = "text")
    private String templateConfirmation;

    @Column(name = "template_ready", nullable = false, columnDefinition = "text")
    private String templateReady;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
