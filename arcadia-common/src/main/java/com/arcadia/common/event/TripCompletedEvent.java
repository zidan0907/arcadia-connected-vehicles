package com.arcadia.common.event;

import java.time.Instant;
import java.util.Objects;

/** Event published after a trip has been persisted and its telemetry accepted. */
public record TripCompletedEvent(
        String tripId,
        String driverId,
        String vin,
        double distanceKm,
        int harshBrakingCount,
        double idleMinutes,
        double accessoryLoad,
        Instant occurredAt) implements DomainEvent {

    public TripCompletedEvent {
        Objects.requireNonNull(tripId, "tripId must not be null");
        Objects.requireNonNull(driverId, "driverId must not be null");
        Objects.requireNonNull(vin, "vin must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        if (distanceKm < 0 || harshBrakingCount < 0 || idleMinutes < 0 || accessoryLoad < 0) {
            throw new IllegalArgumentException("Trip metrics must not be negative");
        }
    }
}
