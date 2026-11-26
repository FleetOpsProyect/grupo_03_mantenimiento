package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateManualOrderRequest {

    @NotNull(message = "vehiculo_id es obligatorio")
    @JsonProperty("vehiculo_id")
    @JsonAlias("vehicleId")
    private UUID vehiculoId;

    @NotNull(message = "tipo_mantenimiento es obligatorio")
    @Pattern(regexp = "^(PREVENTIVO|CORRECTIVO|PREVENTIVE|CORRECTIVE)$",
            message = "tipo_mantenimiento no contemplado")
    @JsonProperty("tipo_mantenimiento")
    @JsonAlias("maintenanceType")
    private String tipoMantenimiento;

    @NotBlank(message = "descripcion es obligatoria")
    @Size(min = 10, max = 300, message = "descripcion debe tener entre 10 y 300 caracteres")
    @JsonProperty("descripcion")
    @JsonAlias("description")
    private String descripcion;

    @NotBlank(message = "taller_asignado es obligatorio")
    @Size(max = 100, message = "taller_asignado no puede superar 100 caracteres")
    @JsonProperty("taller_asignado")
    @JsonAlias("assignedWorkshop")
    private String tallerAsignado;

    @JsonProperty("tecnico_id")
    @JsonAlias("technicianId")
    private UUID tecnicoId;
}
