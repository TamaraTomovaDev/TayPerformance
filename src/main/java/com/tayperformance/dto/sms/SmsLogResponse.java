package com.tayperformance.dto.sms;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class SmsLogResponse {

    private Long id;

    private Long appointmentId;

    private String type;      // CONFIRM / UPDATE / REMINDER
    private String status;    // QUEUED / SENT / FAILED / DELIVERED

    private String toPhone;

    private String messageBody;

    private OffsetDateTime createdAt;
    private OffsetDateTime sentAt;
    private OffsetDateTime deliveredAt;
}
