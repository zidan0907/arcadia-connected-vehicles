package com.arcadia.operations.api.service;

import com.arcadia.operations.api.dto.*;

public interface OperationsService {

    CommandAcceptedResponse submitCommand(String vin, String requestId, CommandRequest request);

    CommandStatusResponse getCommandStatus(String commandId);

    VehicleStatusResponse getVehicleStatus(String vin);

    CommandStatusResponse acknowledgeCommand(String vin, String commandId, CommandAckRequest request);
}
