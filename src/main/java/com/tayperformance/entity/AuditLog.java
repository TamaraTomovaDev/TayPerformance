// src/main/java/com/tayperformance/entity/AuditLog.java
package com.tayperformance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_entity", columnList = "entity_type,entity_id"),
                @Index(name = "idx_audit_created", columnList = "created_at"),
                @Index(name = "idx_audit_user", columnList = "actor_user_id")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false, length = 60)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(nullable = false, length = 60)
    private String action;

    @Column(name = "details_json", columnDefinition = "TEXT")
    private String detailsJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actor;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
