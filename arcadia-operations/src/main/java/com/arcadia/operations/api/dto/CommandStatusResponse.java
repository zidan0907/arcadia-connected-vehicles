package com.arcadia.operations.api.dto;

import com.arcadia.operations.api.domain.CommandStatus;
import com.arcadia.operations.api.domain.CommandType;
import java.time.OffsetDateTime;

public record CommandStatusResponse(
        String commandId,
        String vin,
        CommandType command,
        CommandStatus status,
        OffsetDateTime executedAt,
        String errorMessage) {
}
