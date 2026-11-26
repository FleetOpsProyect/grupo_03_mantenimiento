package co.edu.univalle.fleetops.mantenimiento.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuración del documento OpenAPI (Swagger UI).
 *
 * <p>En producción la aplicación corre detrás de Nginx (HTTPS en el puerto 6767) y solo ve el request
 * interno por HTTP. Si springdoc generara la URL del servidor a partir de ese request, Swagger UI
 * enviaría las peticiones a {@code http://<dominio>} sin puerto: un esquema y puerto equivocados.
 * Con una URL relativa, Swagger UI usa el mismo origen de la página desde la que se abrió.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI maintenanceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("FleetOps - Microservicio de Mantenimiento")
                        .version("v1"))
                .servers(List.of(new Server().url("/").description("Mismo origen que Swagger UI")));
    }
}
