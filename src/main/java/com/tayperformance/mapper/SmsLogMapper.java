package com.tayperformance.mapper;

import com.tayperformance.dto.sms.SmsLogResponse;
import com.tayperformance.entity.SmsLog;

public final class SmsLogMapper {

    private SmsLogMapper() {
    }

    public static SmsLogResponse toResponse(SmsLog log) {
        if (log == null) return null;

        return SmsLogResponse.builder()
                .id(log.getId())
                .appointmentId(
                        log.getAppointment() != null ? log.getAppointment().getId() : null
                )
                .type(log.getType().name())
                .status(log.getStatus().name())
                .toPhone(log.getToPhone())
                .messageBody(log.getMessageBody())
                .createdAt(log.getCreatedAt())
                .sentAt(log.getSentAt())
                .deliveredAt(log.getDeliveredAt())
                .build();
    }
}
