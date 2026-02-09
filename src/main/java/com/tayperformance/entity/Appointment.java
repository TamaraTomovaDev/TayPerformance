package com.tayperformance.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Duration;
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
@ToString(exclude = {"customer", "assignedStaff", "createdBy", "updatedBy"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Appointment {

    @EqualsAndHashCode.Include
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnore
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_staff_id")
    @JsonIgnore
    private User assignedStaff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    @JsonIgnore
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_user_id")
    @JsonIgnore
    private User updatedBy;

    @NotBlank
    @Size(min = 2, max = 80)
    @Column(name = "car_brand", nullable = false, length = 80)
    private String carBrand;

    @Size(max = 80)
    @Column(name = "car_model", length = 80)
    private String carModel;

    @Size(max = 5000)
    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Column(name = "start_time", nullable = false)
    private OffsetDateTime startTime;

    @NotNull
    @Column(name = "end_time", nullable = false)
    private OffsetDateTime endTime;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.REQUESTED;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Transient
    public Integer getDurationMinutes() {
        if (startTime == null || endTime == null) return null;
        return (int) Duration.between(startTime, endTime).toMinutes();
    }

    @Transient
    public boolean isModifiable() {
        return status != AppointmentStatus.COMPLETED && status != AppointmentStatus.NOSHOW;
    }

    @AssertTrue(message = "Eindtijd moet na starttijd liggen")
    private boolean isEndTimeAfterStartTime() {
        return startTime == null || endTime == null || endTime.isAfter(startTime);
    }

    @AssertTrue(message = "Afspraak duurt langer dan 8 uur - controleer de tijden")
    private boolean isDurationReasonable() {
        if (startTime == null || endTime == null) return true;
        return Duration.between(startTime, endTime).toHours() <= 8;
    }
}
