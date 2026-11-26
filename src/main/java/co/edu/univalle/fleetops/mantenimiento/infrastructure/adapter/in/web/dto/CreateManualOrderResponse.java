package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class CreateManualOrderResponse {

    @JsonProperty("orden_id")
    private UUID ordenId;

    @JsonProperty("mensaje")
    private String mensaje;
}
