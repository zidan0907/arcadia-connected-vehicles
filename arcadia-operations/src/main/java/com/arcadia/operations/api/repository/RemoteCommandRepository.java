package com.arcadia.operations.api.repository;

import com.arcadia.operations.api.domain.RemoteCommand;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface RemoteCommandRepository extends JpaRepository<RemoteCommand, UUID> {
    Optional<RemoteCommand> findByCommandId(String commandId);

    Optional<RemoteCommand> findFirstByVinOrderByAcceptedAtDesc(String vin);

    Optional<RemoteCommand> findByRequestId(String requestId);
}
