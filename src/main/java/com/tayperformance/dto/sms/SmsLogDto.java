package com.tayperformance.dto.sms;

import com.tayperformance.entity.SmsStatus;
import com.tayperformance.entity.SmsType;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class SmsLogDto {
    private Long id;
    private Long appointmentId;
    private SmsType type;
    private SmsStatus status;
    private String toPhone;
    private String messageBody;
    private String providerMessageId;
    private String errorMessage;
    private OffsetDateTime createdAt;
    private OffsetDateTime sentAt;
    private OffsetDateTime deliveredAt;
}