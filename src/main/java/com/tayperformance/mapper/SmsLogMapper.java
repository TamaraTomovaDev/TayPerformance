package com.tayperformance.mapper;

import com.tayperformance.dto.sms.SmsLogDto;
import com.tayperformance.entity.SmsLog;

public final class SmsLogMapper {

    private SmsLogMapper() {}

    public static SmsLogDto toDto(SmsLog s) {
        if (s == null) return null;

        return SmsLogDto.builder()
                .id(s.getId())
                .appointmentId(s.getAppointment().getId())
                .type(s.getType())
                .status(s.getStatus())
                .toPhone(s.getToPhone())
                .messageBody(s.getMessageBody())
                .providerMessageId(s.getProviderMessageId())
                .errorMessage(s.getErrorMessage())
                .createdAt(s.getCreatedAt())
                .sentAt(s.getSentAt())
                .deliveredAt(s.getDeliveredAt())
                .build();
    }
}