package co.edu.univalle.fleetops.mantenimiento.domain.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreventiveMaintenanceRequiredEventTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Crea correctamente los metadatos CloudEvents v1.0 y su payload Data")
    void shouldCreateCloudEventMetadataAndData() {
        UUID orderId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        String reason = "KILOMETRAJE_SUPERADO";
        int currentMileage = 15000;
        int thresholdKm = 10000;
        long elapsedDays = 45L;

        PreventiveMaintenanceRequiredEvent event = PreventiveMaintenanceRequiredEvent.create(
                orderId,
                vehicleId,
                reason,
                currentMileage,
                thresholdKm,
                elapsedDays
        );

        assertNotNull(event);
        assertEquals("1.0", event.specversion());
        assertTrue(event.id().startsWith("evt-mpr-"));
        assertEquals(PreventiveMaintenanceRequiredEvent.EVENT_TYPE, event.type());
        assertEquals(PreventiveMaintenanceRequiredEvent.EVENT_SOURCE, event.source());
        assertEquals("application/json", event.datacontenttype());
        assertNotNull(event.time());

        PreventiveMaintenanceRequiredEvent.Data data = event.data();
        assertNotNull(data);
        assertEquals(orderId, data.ordenId());
        assertEquals(orderId, data.mantenimientoId());
        assertEquals(vehicleId, data.vehiculoId());
        assertEquals("PREVENTIVO", data.tipoMantenimiento());
        assertEquals(reason, data.motivo());
        assertEquals(currentMileage, data.kilometrajeActual());
        assertEquals(thresholdKm, data.umbralKm());
        assertEquals(elapsedDays, data.diasTranscurridos());
        assertNotNull(data.timestamp());
    }

    @Test
    @DisplayName("Serializa correctamente el payload Data en formato JSON snake_case")
    void shouldSerializeEventDataInSnakeCase() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();

        PreventiveMaintenanceRequiredEvent event = PreventiveMaintenanceRequiredEvent.create(
                orderId,
                vehicleId,
                "KILOMETRAJE_SUPERADO",
                15000,
                10000,
                45L
        );

        String json = objectMapper.writeValueAsString(event);
        JsonNode root = objectMapper.readTree(json);
        JsonNode dataNode = root.get("data");

        assertNotNull(dataNode);
        assertEquals(orderId.toString(), dataNode.get("orden_id").asText());
        assertEquals(orderId.toString(), dataNode.get("mantenimiento_id").asText());
        assertEquals(vehicleId.toString(), dataNode.get("vehiculo_id").asText());
        assertEquals("PREVENTIVO", dataNode.get("tipo_mantenimiento").asText());
        assertEquals("KILOMETRAJE_SUPERADO", dataNode.get("motivo").asText());
        assertEquals(15000, dataNode.get("kilometraje_actual").asInt());
        assertEquals(10000, dataNode.get("umbral_km").asInt());
        assertEquals(45, dataNode.get("dias_transcurridos").asLong());
        assertNotNull(dataNode.get("timestamp").asText());
    }
}