package co.edu.univalle.fleetops.mantenimiento.domain.exception;

import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.MaintenanceStatus;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.MaintenanceType;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.VehicleCategory;
import co.edu.univalle.fleetops.mantenimiento.domain.port.in.QueryOrdersUseCase.OrderFilter;
import co.edu.univalle.fleetops.mantenimiento.domain.port.in.QueryOrdersUseCase.PagedOrdersResult;
import co.edu.univalle.fleetops.mantenimiento.domain.port.out.VehicleServiceClientPort.VehicleSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("Domain Exceptions and Port Records Unit Tests")
class DomainExceptionsAndPortsTest {

    @Test
    @DisplayName("Domain exceptions capture error codes and messages accurately")
    void testDomainExceptions() {
        DomainException domainEx = new DomainException("DOMAIN_ERROR", "General domain error");
        assertEquals("General domain error", domainEx.getMessage());
        assertEquals("DOMAIN_ERROR", domainEx.getErrorCode());

        BusinessRuleException ruleEx = new BusinessRuleException("RULE_FAILED", "Rule violation");
        assertEquals("Rule violation", ruleEx.getMessage());
        assertEquals("RULE_FAILED", ruleEx.getErrorCode());

        BusinessRuleException singleRuleEx = new BusinessRuleException("Default rule violation");
        assertEquals("Default rule violation", singleRuleEx.getMessage());
        assertEquals("BUSINESS_RULE_VIOLATION", singleRuleEx.getErrorCode());

        EntityNotFoundException notFoundEx = new EntityNotFoundException("NOT_FOUND", "Entity not found");
        assertEquals("Entity not found", notFoundEx.getMessage());
        assertEquals("NOT_FOUND", notFoundEx.getErrorCode());

        EntityNotFoundException singleNotFoundEx = new EntityNotFoundException("Entity 123 not found");
        assertEquals("Entity 123 not found", singleNotFoundEx.getMessage());
        assertEquals("ENTITY_NOT_FOUND", singleNotFoundEx.getErrorCode());

        ResourceConflictException conflictEx = new ResourceConflictException("CONFLICT", "Duplicate entity");
        assertEquals("Duplicate entity", conflictEx.getMessage());
        assertEquals("CONFLICT", conflictEx.getErrorCode());

        ResourceConflictException singleConflictEx = new ResourceConflictException("Duplicate email");
        assertEquals("Duplicate email", singleConflictEx.getMessage());
        assertEquals("RESOURCE_CONFLICT", singleConflictEx.getErrorCode());

        InvalidStateTransitionException transitionEx = new InvalidStateTransitionException("Cannot move from COMPLETED to IN_PROGRESS");
        assertEquals("Cannot move from COMPLETED to IN_PROGRESS", transitionEx.getMessage());
        assertEquals("INVALID_TRANSITION", transitionEx.getErrorCode());
    }

    @Test
    @DisplayName("Port records correctly hold domain query parameters and summaries")
    void testPortRecords() {
        UUID vehicleId = UUID.randomUUID();
        UUID techId = UUID.randomUUID();

        OrderFilter filter = new OrderFilter(
                MaintenanceStatus.IN_PROGRESS,
                MaintenanceType.PREVENTIVE,
                vehicleId,
                techId,
                1,
                20
        );

        assertEquals(MaintenanceStatus.IN_PROGRESS, filter.status());
        assertEquals(MaintenanceType.PREVENTIVE, filter.maintenanceType());
        assertEquals(vehicleId, filter.vehicleId());
        assertEquals(techId, filter.technicianId());
        assertEquals(1, filter.page());
        assertEquals(20, filter.size());

        PagedOrdersResult result = new PagedOrdersResult(
                List.of(),
                1,
                20,
                0L,
                0,
                Map.of("avg_repair_hours", 3.5)
        );

        assertNotNull(result.content());
        assertEquals(1, result.page());
        assertEquals(20, result.size());
        assertEquals(0L, result.totalElements());
        assertEquals(0, result.totalPages());
        assertEquals(3.5, result.summary().get("avg_repair_hours"));

        VehicleSummary vehicle = new VehicleSummary(
                vehicleId,
                55000,
                "AVAILABLE",
                VehicleCategory.CAMIONETA
        );

        assertEquals(vehicleId, vehicle.id());
        assertEquals(55000, vehicle.mileage());
        assertEquals("AVAILABLE", vehicle.status());
        assertEquals(VehicleCategory.CAMIONETA, vehicle.category());
    }
}
