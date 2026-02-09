package com.tayperformance.dto.customer;

import com.tayperformance.dto.appointment.AppointmentResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Klantgeschiedenis met afspraken en statistieken.
 */
@Data
@Builder
public class CustomerHistoryResponse {
    private Long customerId;
    private String displayName;
    private String phone;
    private boolean active;

    // Statistieken
    private boolean isLoyal;
    private long totalCompleted;
    private long recentNoShows;

    // Recente afspraken (laatste 6 maanden, max 20)
    private List<AppointmentResponse> recentAppointments;
}