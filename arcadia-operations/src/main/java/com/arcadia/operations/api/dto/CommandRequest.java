package com.arcadia.operations.api.dto;

import com.arcadia.operations.api.domain.CommandType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.Map;

public record CommandRequest(
        @NotNull CommandType command,
        @NotBlank String source,
        @NotBlank String requestedBy,
        Map<String,Object> params,
        @NotNull OffsetDateTime timestamp) {
}
