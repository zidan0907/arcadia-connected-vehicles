package com.arcadia.operations.api.dto;

import java.time.OffsetDateTime;

public record VehicleStatusResponse(
        String vin,
        String doorLockStatus,
        boolean climateActive,
        int batteryLevelPercent,
        OffsetDateTime lastSyncedAt) {
}
