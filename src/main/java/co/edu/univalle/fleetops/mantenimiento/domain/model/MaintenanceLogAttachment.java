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
public class MaintenanceLogAttachment {

    private UUID id;
    private UUID maintenanceLogId;
    private String fileUrl;
    private String fileName;
    private String mimeType;
    private Long fileSizeBytes;
    private LocalDateTime createdAt;
}
