package com.arcadia.operations.api.dto;

import com.arcadia.operations.api.domain.CommandStatus;
import java.time.OffsetDateTime;

public record CommandAcceptedResponse(
        String commandId,
        String vin,
        CommandStatus status,
        OffsetDateTime acceptedAt,
        String pollUrl) {
}
