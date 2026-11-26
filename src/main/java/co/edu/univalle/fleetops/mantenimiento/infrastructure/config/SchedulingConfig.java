package co.edu.univalle.fleetops.mantenimiento.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Configuración de programación de tareas (Scheduling).
 * Asegura la existencia de un bean TaskScheduler en el ApplicationContext para PreventiveInspectionScheduler.
 */
@Configuration
public class SchedulingConfig {

    @Bean
    @ConditionalOnMissingBean(TaskScheduler.class)
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("fleetops-scheduler-");
        scheduler.initialize();
        return scheduler;
    }
}
