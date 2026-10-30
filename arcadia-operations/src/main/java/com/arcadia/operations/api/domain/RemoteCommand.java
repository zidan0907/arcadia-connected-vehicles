package com.arcadia.operations.api.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Entity
@Table(name = "remote_commands", schema = "operations")
public class RemoteCommand {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "command_id", nullable = false, unique = true, length = 50)
    private String commandId;
    @Column(nullable = false, length = 32)
    private String vin;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CommandType command;
    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private CommandStatus status;
    @Column(nullable = false, length = 40)
    private String source;
    @Column(name = "requested_by", nullable = false, length = 120)
    private String requestedBy;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "params", columnDefinition = "jsonb")
    private Map<String, Object> params;
    @Column(name = "requested_at", nullable = false)
    private OffsetDateTime requestedAt;
    @Column(name = "accepted_at", nullable = false)
    private OffsetDateTime acceptedAt;
    @Setter
    @Column(name = "executed_at")
    private OffsetDateTime executedAt;
    @Setter
    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;
    @Column(name = "request_id", length = 36)
    private String requestId;

    protected RemoteCommand() {}

    public RemoteCommand(String commandId, String vin, CommandType command,
                         String source, String requestedBy, Map<String,Object> params,
                         OffsetDateTime requestedAt, String requestId) {
        this.commandId = commandId;
        this.vin = vin;
        this.command = command;
        this.source = source;
        this.requestedBy = requestedBy;
        this.params = params;
        this.requestedAt = requestedAt;
        this.requestId = requestId;
        this.status = CommandStatus.IN_PROGRESS;
        this.acceptedAt = OffsetDateTime.now();
    }
}
