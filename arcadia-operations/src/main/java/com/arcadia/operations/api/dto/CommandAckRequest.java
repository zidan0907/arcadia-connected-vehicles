package com.arcadia.operations.api.dto;

import com.arcadia.operations.api.domain.CommandStatus;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record CommandAckRequest(
        @NotNull CommandStatus status,
        OffsetDateTime executedAt,
        String errorMessage) {
}
