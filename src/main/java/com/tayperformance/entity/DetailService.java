// src/main/java/com/tayperformance/entity/DetailService.java
package com.tayperformance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "services",
        indexes = {
                @Index(name = "idx_services_active", columnList = "active"),
                @Index(name = "idx_services_name", columnList = "name")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DetailService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "min_minutes", nullable = false)
    private Integer minMinutes;

    @Column(name = "max_minutes", nullable = false)
    private Integer maxMinutes;

    @Column(name = "default_minutes", nullable = false)
    private Integer defaultMinutes;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
