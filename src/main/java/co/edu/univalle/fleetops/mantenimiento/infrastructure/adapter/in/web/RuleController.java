package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.in.web;

import co.edu.univalle.fleetops.mantenimiento.domain.model.MaintenanceRule;
import co.edu.univalle.fleetops.mantenimiento.domain.port.in.RegisterRuleUseCase;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.in.web.dto.RegisterRuleRequest;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.in.web.dto.RuleResponse;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.in.web.mapper.RuleWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador de entrada REST para las reglas de mantenimiento preventivo (HU-01). */
@RestController
@RequestMapping("/maintenance/rules")
@Tag(name = "Reglas preventivas", description = "HU-01: parametrización de reglas de inspección preventiva")
public class RuleController {

    private final RegisterRuleUseCase registerRuleUseCase;
    private final RuleWebMapper mapper;

    public RuleController(RegisterRuleUseCase registerRuleUseCase, RuleWebMapper mapper) {
        this.registerRuleUseCase = registerRuleUseCase;
        this.mapper = mapper;
    }

    @PostMapping
    @Operation(summary = "Registrar una regla de mantenimiento preventivo (GLOBAL, CATEGORIA o VEHICULO)")
    public ResponseEntity<RuleResponse> registerRule(@Valid @RequestBody RegisterRuleRequest request) {
        MaintenanceRule created = registerRuleUseCase.registerRule(mapper.toDomain(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(created));
    }
}
