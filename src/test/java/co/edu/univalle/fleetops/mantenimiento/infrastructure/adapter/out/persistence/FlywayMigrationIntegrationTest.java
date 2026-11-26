package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence;

import co.edu.univalle.fleetops.mantenimiento.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlywayMigrationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Debe conectar a la base de datos y ejecutar las migraciones de Flyway")
    void shouldConnectAndMigrateDatabase() throws Exception {
        assertNotNull(dataSource, "El DataSource debe estar configurado");

        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement()) {

            assertFalse(connection.isClosed(), "La conexión debe estar abierta");

            // Verificar que Flyway creó la tabla de control flyway_schema_history
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT table_name FROM information_schema.tables WHERE table_name = 'flyway_schema_history'")) {
                assertTrue(rs.next(), "La tabla flyway_schema_history debe existir");
            }

            // Verificar que existen las 7 tablas creadas por V1
            String[] expectedTables = {
                    "technician",
                    "preventive_rule",
                    "maintenance_order",
                    "maintenance_log",
                    "maintenance_log_attachment",
                    "outbox_message",
                    "access_audit_log"
            };

            for (String tableName : expectedTables) {
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT table_name FROM information_schema.tables WHERE table_name = '" + tableName + "'")) {
                    assertTrue(rs.next(), "La tabla " + tableName + " debe existir en la base de datos");
                }
            }
        }
    }
}
