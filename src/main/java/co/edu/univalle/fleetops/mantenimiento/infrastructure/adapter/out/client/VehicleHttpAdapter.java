package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.client;

import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.VehicleCategory;
import co.edu.univalle.fleetops.mantenimiento.domain.port.out.VehicleServiceClientPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class VehicleHttpAdapter implements VehicleServiceClientPort {

    private static final Logger log = LoggerFactory.getLogger(VehicleHttpAdapter.class);

    private final RestTemplate restTemplate;
    private final String vehiclesBaseUrl;

    public VehicleHttpAdapter(RestTemplate restTemplate,
                              @Value("${services.vehicles.url:http://localhost:8081}") String vehiclesBaseUrl) {
        this.restTemplate = restTemplate;
        this.vehiclesBaseUrl = vehiclesBaseUrl;
    }

    @Override
    public Optional<VehicleSummary> getVehicleById(UUID vehicleId) {
        String url = vehiclesBaseUrl + "/vehiculos/" + vehicleId;
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map body = response.getBody();
                Integer mileage = body.get("kilometraje_actual") != null
                        ? ((Number) body.get("kilometraje_actual")).intValue()
                        : (body.get("mileage") != null ? ((Number) body.get("mileage")).intValue() : null);

                String status = body.get("estado") != null
                        ? body.get("estado").toString()
                        : (body.get("status") != null ? body.get("status").toString() : "ACTIVO");

                String categoryStr = body.get("categoria") != null
                        ? body.get("categoria").toString()
                        : (body.get("category") != null ? body.get("category").toString() : null);

                VehicleCategory category = null;
                if (categoryStr != null) {
                    try {
                        category = VehicleCategory.valueOf(categoryStr.toUpperCase());
                    } catch (IllegalArgumentException ignored) {
                        // Categoria no catalogada
                    }
                }

                return Optional.of(new VehicleSummary(vehicleId, mileage, status, category));
            }
            return Optional.empty();
        } catch (HttpClientErrorException.NotFound ex) {
            log.warn("Vehiculo con id {} no encontrado en MS Vehiculos (404)", vehicleId);
            return Optional.empty();
        } catch (RestClientException ex) {
            log.error("Error al consultar vehiculo {} en {}: {}", vehicleId, url, ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public boolean updateVehicleStatus(UUID vehicleId, UUID maintenanceId, String newStatus) {
        String url = vehiclesBaseUrl + "/vehiculos/" + vehicleId + "/estado";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> payload = Collections.singletonMap("estado", newStatus);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.PUT, request, Void.class);
            boolean isSuccess = response.getStatusCode().is2xxSuccessful();
            if (isSuccess) {
                log.info("Estado del vehiculo {} actualizado a {} exitosamente", vehicleId, newStatus);
            } else {
                log.error("Respuesta no exitosa al actualizar estado del vehiculo {}: {}", vehicleId, response.getStatusCode());
            }
            return isSuccess;
        } catch (RestClientException ex) {
            log.error("Fallo de comunicacion con MS Vehiculos al cambiar estado a {}: {}", newStatus, ex.getMessage());
            return false;
        }
    }

    @Override
    public List<VehicleSummary> getActiveVehicles() {
        String url = vehiclesBaseUrl + "/vehiculos?estado=ACTIVO";
        try {
            ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<VehicleSummary> result = new ArrayList<>();
                for (Object item : response.getBody()) {
                    if (item instanceof Map map) {
                        Object idObj = map.get("id");
                        if (idObj != null) {
                            UUID id = UUID.fromString(idObj.toString());
                            Integer mileage = map.get("kilometraje_actual") != null
                                    ? ((Number) map.get("kilometraje_actual")).intValue()
                                    : null;
                            String status = map.get("estado") != null ? map.get("estado").toString() : "ACTIVO";
                            result.add(new VehicleSummary(id, mileage, status, null));
                        }
                    }
                }
                return result;
            }
            return Collections.emptyList();
        } catch (RestClientException ex) {
            log.error("Error al obtener vehiculos activos en {}: {}", url, ex.getMessage());
            return Collections.emptyList();
        }
    }
}
