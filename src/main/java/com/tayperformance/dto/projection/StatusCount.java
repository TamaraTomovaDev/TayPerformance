package com.tayperformance.dto.projection;

import com.tayperformance.entity.AppointmentStatus;

public interface StatusCount {
    AppointmentStatus getStatus();
    Long getCount();
}