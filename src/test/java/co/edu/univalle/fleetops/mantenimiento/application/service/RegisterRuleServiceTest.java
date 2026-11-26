package co.edu.univalle.fleetops.mantenimiento.application.service;

import co.edu.univalle.fleetops.mantenimiento.domain.exception.BusinessRuleException;
import co.edu.univalle.fleetops.mantenimiento.domain.exception.EntityNotFoundException;
import co.edu.univalle.fleetops.mantenimiento.domain.exception.ResourceConflictException;
import co.edu.univalle.fleetops.mantenimiento.domain.model.MaintenanceRule;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.RuleScope;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.VehicleCategory;
import co.edu.univalle.fleetops.mantenimiento.domain.port.out.RuleRepositoryPort;
import co.edu.univalle.fleetops.mantenimiento.domain.port.out.VehicleServiceClientPort;
import co.edu.univalle.fleetops.mantenimiento.domain.port.out.VehicleServiceClientPort.VehicleSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterRuleServiceTest {

    @Mock
    private RuleRepositoryPort ruleRepository;
    @Mock
    private VehicleServiceClientPort vehicleClient;

    private RegisterRuleService service;

    @BeforeEach
    void setUp() {
        service = new RegisterRuleService(ruleRepository, vehicleClient);
    }

    private static MaintenanceRule.MaintenanceRuleBuilder validRule() {
        return MaintenanceRule.builder().mileageIntervalKm(10000).timeIntervalDays(180);
    }

    @Test
    @DisplayName("Registra una regla GLOBAL activa por defecto sin consultar a Vehículos")
    void shouldRegisterGlobalRuleActiveByDefault() {
        MaintenanceRule rule = validRule().scope(RuleScope.GLOBAL).build();
        when(ruleRepository.findActiveRule(RuleScope.GLOBAL, null, null)).thenReturn(Optional.empty());
        when(ruleRepository.save(rule)).thenAnswer(inv -> inv.getArgument(0));

        MaintenanceRule saved = service.registerRule(rule);

        assertTrue(saved.getIsActive());
        verify(ruleRepository).save(rule);
        verifyNoInteractions(vehicleClient);
    }

    @Test
    @DisplayName("Rechaza con 409 si ya existe una regla activa para el mismo alcance")
    void shouldRejectDuplicateActiveRule() {
        MaintenanceRule rule = validRule().scope(RuleScope.CATEGORY).category(VehicleCategory.CAMIONETA).build();
        when(ruleRepository.findActiveRule(RuleScope.CATEGORY, VehicleCategory.CAMIONETA, null))
                .thenReturn(Optional.of(validRule().scope(RuleScope.CATEGORY).build()));

        ResourceConflictException ex = assertThrows(ResourceConflictException.class, () -> service.registerRule(rule));

        assertEquals("RULE_ALREADY_EXISTS", ex.getErrorCode());
        verify(ruleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Una regla inactiva no choca con la regla activa existente")
    void shouldSkipConflictCheckForInactiveRule() {
        MaintenanceRule rule = validRule().scope(RuleScope.GLOBAL).isActive(false).build();
        when(ruleRepository.save(rule)).thenAnswer(inv -> inv.getArgument(0));

        service.registerRule(rule);

        verify(ruleRepository, never()).findActiveRule(any(), any(), any());
        verify(ruleRepository).save(rule);
    }

    @Test
    @DisplayName("Regla VEHICULO: rechaza con 404 si el vehículo no existe en MS Vehículos")
    void shouldRejectVehicleRuleWhenVehicleDoesNotExist() {
        UUID vehicleId = UUID.randomUUID();
        MaintenanceRule rule = validRule().scope(RuleScope.VEHICLE).vehicleId(vehicleId).build();
        when(vehicleClient.getVehicleById(vehicleId)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> service.registerRule(rule));

        assertEquals("VEHICLE_NOT_FOUND", ex.getErrorCode());
        verify(ruleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Regla VEHICULO: se registra cuando el vehículo existe")
    void shouldRegisterVehicleRuleWhenVehicleExists() {
        UUID vehicleId = UUID.randomUUID();
        MaintenanceRule rule = validRule().scope(RuleScope.VEHICLE).vehicleId(vehicleId).build();
        when(vehicleClient.getVehicleById(vehicleId))
                .thenReturn(Optional.of(new VehicleSummary(vehicleId, 5000, "ACTIVO", VehicleCategory.AUTOMOVIL)));
        when(ruleRepository.findActiveRule(RuleScope.VEHICLE, null, vehicleId)).thenReturn(Optional.empty());
        when(ruleRepository.save(rule)).thenAnswer(inv -> inv.getArgument(0));

        MaintenanceRule saved = service.registerRule(rule);

        assertEquals(vehicleId, saved.getVehicleId());
    }

    @Test
    @DisplayName("Una regla con umbrales inválidos se rechaza antes de llamar a cualquier dependencia")
    void shouldValidateBeforeCallingDependencies() {
        MaintenanceRule rule = MaintenanceRule.builder()
                .scope(RuleScope.VEHICLE).vehicleId(UUID.randomUUID())
                .mileageIntervalKm(10).timeIntervalDays(180).build();

        assertThrows(BusinessRuleException.class, () -> service.registerRule(rule));

        verifyNoInteractions(vehicleClient, ruleRepository);
    }
}
