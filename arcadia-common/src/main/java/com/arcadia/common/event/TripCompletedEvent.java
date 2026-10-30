package com.arcadia.common.event;

import lombok.Builder;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Builder
public record TripCompletedEvent(
        UUID eventId,
        Instant occurredAt,
        String tripId,
        String driverId,
        String vin,
        double distanceKm,
        int harshBrakingCount,
        int idleMinutes,
        Map<String, Object> accessoryHealth
) implements DomainEvent {
}