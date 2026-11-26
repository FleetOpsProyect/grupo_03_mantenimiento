package co.edu.univalle.fleetops.mantenimiento.domain.event;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento CloudEvents v1.0 publicado cuando se requiere una orden de mantenimiento preventivo (HU-02).
 */
public record PreventiveMaintenanceRequiredEvent(
        String specversion,
        String id,
        String type,
        String source,
        String time,
        String datacontenttype,
        Data data
) {
    public static final String EVENT_TYPE = "com.fleetops.mantenimiento.preventivo.requerido";
    public static final String EVENT_SOURCE = "/servicios/ms-mantenimiento";
    public static final String SUBJECT = "mantenimiento.preventivo.requerido";

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Data(
            UUID ordenId,
            UUID mantenimientoId,
            UUID vehiculoId,
            String tipoMantenimiento,
            String motivo,
            Integer kilometrajeActual,
            Integer umbralKm,
            Long diasTranscurridos,
            String timestamp
    ) {
    }

    public static PreventiveMaintenanceRequiredEvent create(
            UUID orderId,
            UUID vehicleId,
            String reason,
            Integer currentMileage,
            Integer thresholdKm,
            Long elapsedDays
    ) {
        String nowIso = Instant.now().toString();
        Data data = new Data(
                orderId,
                orderId,
                vehicleId,
                "PREVENTIVO",
                reason,
                currentMileage,
                thresholdKm,
                elapsedDays,
                nowIso
        );
        return new PreventiveMaintenanceRequiredEvent(
                "1.0",
                "evt-mpr-" + UUID.randomUUID(),
                EVENT_TYPE,
                EVENT_SOURCE,
                nowIso,
                "application/json",
                data
        );
    }
}
