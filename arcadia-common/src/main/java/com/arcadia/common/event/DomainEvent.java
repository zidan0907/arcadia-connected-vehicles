package com.arcadia.common.event;

import java.time.Instant;

/** Marker contract for events published between bounded contexts. */
public interface DomainEvent {
    Instant occurredAt();
}
