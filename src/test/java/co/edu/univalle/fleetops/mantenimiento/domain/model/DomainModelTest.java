package co.edu.univalle.fleetops.mantenimiento.domain.model;

import co.edu.univalle.fleetops.mantenimiento.domain.exception.BusinessRuleException;
import co.edu.univalle.fleetops.mantenimiento.domain.exception.InvalidStateTransitionException;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.MaintenanceOrigin;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.MaintenanceStatus;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.MaintenanceType;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.RuleScope;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.VehicleCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DomainModelTest {

    @Test
    @DisplayName("Debe permitir transiciones válidas de estado en la orden de mantenimiento")
    void shouldAllowValidStateTransitions() {
        MaintenanceOrder order = MaintenanceOrder.builder()
                .id(UUID.randomUUID())
                .vehicleId(UUID.randomUUID())
                .maintenanceType(MaintenanceType.PREVENTIVE)
                .status(MaintenanceStatus.SCHEDULED_PENDING)
                .origin(MaintenanceOrigin.SCHEDULER)
                .build();

        // Asignar técnico
        UUID technicianId = UUID.randomUUID();
        order.assignTechnician(technicianId);
        assertEquals(technicianId, order.getTechnicianId());

        // Iniciar
        order.startProgress();
        assertEquals(MaintenanceStatus.IN_PROGRESS, order.getStatus());

        // Completar
        order.complete("TECHNICIAN", technicianId.toString(), BigDecimal.valueOf(150000));
        assertEquals(MaintenanceStatus.COMPLETED, order.getStatus());
        assertEquals(BigDecimal.valueOf(150000), order.getTotalCost());
        assertNotNull(order.getCompletedAt());
    }

    @Test
    @DisplayName("Debe fallar al intentar completar una orden que está pendiente sin iniciar")
    void shouldFailWhenCompletingPendingOrder() {
        MaintenanceOrder order = MaintenanceOrder.builder()
                .id(UUID.randomUUID())
                .status(MaintenanceStatus.SCHEDULED_PENDING)
                .build();

        assertThrows(InvalidStateTransitionException.class, () ->
                order.complete("TECHNICIAN", "tec-1", BigDecimal.ZERO)
        );
    }

    @Test
    @DisplayName("Debe validar umbrales en reglas de mantenimiento preventivo")
    void shouldValidateMaintenanceRuleThresholds() {
        MaintenanceRule invalidRule = MaintenanceRule.builder()
                .scope(RuleScope.GLOBAL)
                .mileageIntervalKm(500) // menor al mínimo de 1000 km
                .timeIntervalDays(100)
                .build();

        assertThrows(BusinessRuleException.class, invalidRule::validate);

        MaintenanceRule validRule = MaintenanceRule.builder()
                .scope(RuleScope.CATEGORY)
                .category(VehicleCategory.CAMIONETA)
                .mileageIntervalKm(10000)
                .timeIntervalDays(180)
                .build();

        assertDoesNotThrow(validRule::validate);
    }
}
