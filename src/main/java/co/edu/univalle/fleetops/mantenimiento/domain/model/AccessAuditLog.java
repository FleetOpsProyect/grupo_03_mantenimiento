package co.edu.univalle.fleetops.mantenimiento.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessAuditLog {

    private UUID id;
    private String actorType;
    private String actorId;
    private String actorEmail;
    private String action;
    private String resourceAccessed;
    private String httpMethod;
    private String ipAddress;
    private String userAgent;
    private String statusResult;
    private String details;
    private LocalDateTime createdAt;
}
