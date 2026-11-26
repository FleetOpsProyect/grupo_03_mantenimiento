package co.edu.univalle.fleetops.mantenimiento.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SchedulingConfigTest {

    @Test
    @DisplayName("Debe instanciar e inicializar un ThreadPoolTaskScheduler")
    void shouldProvideTaskScheduler() {
        SchedulingConfig config = new SchedulingConfig();
        TaskScheduler scheduler = config.taskScheduler();

        assertNotNull(scheduler);
        assertInstanceOf(ThreadPoolTaskScheduler.class, scheduler);

        ((ThreadPoolTaskScheduler) scheduler).destroy();
    }
}
