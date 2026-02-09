package com.tayperformance.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "customers",
        indexes = {
                @Index(name = "idx_customers_phone", columnList = "phone"),
                @Index(name = "idx_customers_active", columnList = "active")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@ToString(exclude = {"appointments"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Customer {

    @EqualsAndHashCode.Include
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @NotBlank
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$")
    @Column(unique = true, nullable = false, length = 30)
    private String phone;

    @Size(max = 80)
    @Column(name = "first_name", length = 80)
    private String firstName;

    @Size(max = 80)
    @Column(name = "last_name", length = 80)
    private String lastName;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "customer", cascade = {CascadeType.PERSIST}, orphanRemoval = false)
    @JsonIgnore
    private List<Appointment> appointments = new ArrayList<>();

    public String getDisplayName() {
        if (firstName != null && !firstName.isBlank() && lastName != null && !lastName.isBlank()) {
            return firstName + " " + lastName;
        }
        if (firstName != null && !firstName.isBlank()) return firstName;
        return phone;
    }
}
