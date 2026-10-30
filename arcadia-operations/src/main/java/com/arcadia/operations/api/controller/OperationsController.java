package com.arcadia.operations.api.controller;

import com.arcadia.operations.api.dto.*;
import com.arcadia.operations.api.service.OperationsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class OperationsController {
    private final OperationsService service;

    public OperationsController(OperationsService service) {
        this.service = service;
    }

    @PostMapping("/vehicles/{vin}/commands")
    public ResponseEntity<CommandAcceptedResponse> initiateCommand(@PathVariable String vin,
                                                                   @RequestHeader(value="X-Request-Id", required=false) String requestId,
                                                                   @Valid @RequestBody CommandRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(service.submitCommand(vin, requestId, request));
    }

    @GetMapping("/commands/{commandId}")
    public CommandStatusResponse getCommandStatus(@PathVariable String commandId) {
        return service.getCommandStatus(commandId);
    }

    @GetMapping("/vehicles/{vin}/status")
    public VehicleStatusResponse getVehicleStatus(@PathVariable String vin) {
        return service.getVehicleStatus(vin);
    }

    @PostMapping("/vehicles/{vin}/commands/{commandId}/ack")
    public CommandStatusResponse acknowledgeCommand(@PathVariable String vin,
                                                    @PathVariable String commandId,
                                                    @Valid @RequestBody CommandAckRequest request) {
        return service.acknowledgeCommand(vin, commandId, request);
    }
}
