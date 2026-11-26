package co.edu.univalle.fleetops.mantenimiento.application.service;

import co.edu.univalle.fleetops.mantenimiento.domain.exception.EntityNotFoundException;
import co.edu.univalle.fleetops.mantenimiento.domain.exception.ResourceConflictException;
import co.edu.univalle.fleetops.mantenimiento.domain.model.MaintenanceRule;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.RuleScope;
import co.edu.univalle.fleetops.mantenimiento.domain.port.in.RegisterRuleUseCase;
import co.edu.univalle.fleetops.mantenimiento.domain.port.out.RuleRepositoryPort;
import co.edu.univalle.fleetops.mantenimiento.domain.port.out.VehicleServiceClientPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * HU-01: Registrar y parametrizar reglas de mantenimiento preventivo.
 *
 * <p>No se anota con {@code @Transactional} a propósito: la llamada HTTP a Vehículos
 * no debe ejecutarse con una transacción de base de datos abierta (DAS, política ante
 * fallos de Vehículos). El guardado es una única operación y ya es transaccional por sí misma.
 */
@Service
public class RegisterRuleService implements RegisterRuleUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(RegisterRuleService.class);

    private final RuleRepositoryPort ruleRepository;
    private final VehicleServiceClientPort vehicleClient;

    public RegisterRuleService(RuleRepositoryPort ruleRepository, VehicleServiceClientPort vehicleClient) {
        this.ruleRepository = ruleRepository;
        this.vehicleClient = vehicleClient;
    }

    @Override
    public MaintenanceRule registerRule(MaintenanceRule rule) {
        // 1. Reglas de negocio del dominio (alcance, umbrales). Lanza BusinessRuleException -> 422.
        rule.validate();
        if (rule.getIsActive() == null) {
            rule.setIsActive(true);
        }

        // 2. Si la regla es para un vehículo concreto, ese vehículo debe existir en MS Vehículos.
        if (rule.getScope() == RuleScope.VEHICLE && vehicleClient.getVehicleById(rule.getVehicleId()).isEmpty()) {
            throw new EntityNotFoundException("VEHICLE_NOT_FOUND", "El vehículo especificado no existe en el sistema");
        }

        // 3. Solo puede existir una regla activa por alcance y objetivo.
        if (Boolean.TRUE.equals(rule.getIsActive())
                && ruleRepository.findActiveRule(rule.getScope(), rule.getCategory(), rule.getVehicleId()).isPresent()) {
            throw new ResourceConflictException("RULE_ALREADY_EXISTS",
                    "Ya existe una regla activa para el alcance especificado");
        }

        // 4. Persistir. Si otra petición concurrente ganó la carrera, el índice único parcial de la BD
        //    lanza DataIntegrityViolationException, que el manejador global traduce a 409.
        MaintenanceRule saved = ruleRepository.save(rule);
        LOG.info("Regla de mantenimiento registrada: id={}, alcance={}", saved.getId(), saved.getScope());
        return saved;
    }
}
