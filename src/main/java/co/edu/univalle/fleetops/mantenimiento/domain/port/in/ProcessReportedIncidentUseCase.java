package co.edu.univalle.fleetops.mantenimiento.domain.port.in;

import java.util.UUID;

public interface ProcessReportedIncidentUseCase {
    void processIncident(UUID incidentId, UUID vehicleId, String description, String incidentType);
}
