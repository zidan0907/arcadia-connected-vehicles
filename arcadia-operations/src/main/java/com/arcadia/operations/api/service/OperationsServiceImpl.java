package com.arcadia.operations.api.service;

import com.arcadia.operations.api.domain.*;
import com.arcadia.operations.api.dto.*;
import com.arcadia.operations.api.repository.RemoteCommandRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class OperationsServiceImpl implements OperationsService {
    private final RemoteCommandRepository repository;

    public OperationsServiceImpl(RemoteCommandRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public CommandAcceptedResponse submitCommand(String vin, String requestId, CommandRequest request) {
        if (requestId != null && !requestId.isBlank()) {
            var existing = repository.findByRequestId(requestId);
            if (existing.isPresent()) {
                RemoteCommand c = existing.get();
                if (!c.getVin().equals(vin)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "X-Request-Id already used for another vehicle");
                }
                return accepted(c);
            }
        }
        String commandId = "cmd-" + UUID.randomUUID().toString().substring(0, 8);
        RemoteCommand saved = repository.save(new RemoteCommand(commandId, vin, request.command(), request.source(),
                request.requestedBy(), request.params(), request.timestamp(), requestId));
        return accepted(saved);
    }

    private CommandAcceptedResponse accepted(RemoteCommand c) {
        return new CommandAcceptedResponse(c.getCommandId(), c.getVin(), c.getStatus(), c.getAcceptedAt(),
                "/api/v1/commands/" + c.getCommandId());
    }

    @Override
    @Transactional(readOnly = true)
    public CommandStatusResponse getCommandStatus(String commandId) {
        return status(find(commandId));
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleStatusResponse getVehicleStatus(String vin) {
        RemoteCommand latest = repository.findFirstByVinOrderByAcceptedAtDesc(vin)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle status not found for VIN " + vin));
        String door = "UNLOCKED";
        boolean climate = false;
        if (latest.getStatus() == CommandStatus.SUCCESS) {
            if (latest.getCommand() == CommandType.DOOR_LOCK) door = "LOCKED";
            if (latest.getCommand() == CommandType.DOOR_UNLOCK) door = "UNLOCKED";
            if (latest.getCommand() == CommandType.CLIMATE_START) climate = true;
            if (latest.getCommand() == CommandType.CLIMATE_STOP) climate = false;
        }
        return new VehicleStatusResponse(vin, door, climate, 0, latest.getExecutedAt() == null ? latest.getAcceptedAt() : latest.getExecutedAt());
    }

    @Override
    @Transactional
    public CommandStatusResponse acknowledgeCommand(String vin, String commandId, CommandAckRequest request) {
        RemoteCommand c = find(commandId);
        if (!c.getVin().equals(vin)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Command not found for VIN " + vin);
        }
        if (c.getStatus() == CommandStatus.SUCCESS || c.getStatus() == CommandStatus.FAILED
                || c.getStatus() == CommandStatus.TIMED_OUT) {
            return status(c);
        }
        c.setStatus(CommandStatus.valueOf(request.status().toString()));
        c.setExecutedAt(request.executedAt() == null ? OffsetDateTime.now() : request.executedAt());
        c.setErrorMessage(request.errorMessage());
        return status(repository.save(c));
    }

    private RemoteCommand find(String id) {
        return repository.findByCommandId(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Command not found: " + id));
    }

    private CommandStatusResponse status(RemoteCommand c) {
        return new CommandStatusResponse(c.getCommandId(), c.getVin(), c.getCommand(),
                c.getStatus(), c.getExecutedAt(), c.getErrorMessage());
    }
}
