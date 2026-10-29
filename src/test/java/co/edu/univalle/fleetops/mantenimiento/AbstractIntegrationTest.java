package co.edu.univalle.fleetops.mantenimiento;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DirtiesContext
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    protected static final PostgreSQLContainer<?> postgresContainer =
            new PostgreSQLContainer<>("postgres:16-alpine");
}
