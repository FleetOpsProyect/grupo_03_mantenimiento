package co.edu.univalle.fleetops.mantenimiento.infrastructure.config;

import co.edu.univalle.fleetops.mantenimiento.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Detrás del proxy (Nginx con HTTPS en :6767) la aplicación solo ve el request interno. Si el spec
 * publicara una URL absoluta, Swagger UI enviaría las peticiones a un esquema y puerto equivocados.
 */
@AutoConfigureMockMvc
class OpenApiConfigIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Publica una URL de servidor relativa para que Swagger use el origen de la página")
    void shouldPublishRelativeServerUrl() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servers", hasSize(1)))
                .andExpect(jsonPath("$.servers[0].url").value("/"));
    }

    @Test
    @DisplayName("No arma la URL con el host y esquema del request interno que llega desde el proxy")
    void shouldIgnoreInternalRequestHostAndScheme() throws Exception {
        mockMvc.perform(get("/v3/api-docs").header("Host", "fleetops-mantenimiento.duckdns.org"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servers[0].url").value("/"));
    }
}
