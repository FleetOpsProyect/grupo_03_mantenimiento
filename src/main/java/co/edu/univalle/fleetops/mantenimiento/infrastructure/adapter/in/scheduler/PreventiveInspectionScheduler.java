package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.in.scheduler;

import co.edu.univalle.fleetops.mantenimiento.domain.exception.ServiceUnavailableException;
import co.edu.univalle.fleetops.mantenimiento.domain.port.in.ExecutePreventiveInspectionUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Adaptador de entrada que ejecuta el Scheduler periódico de inspección preventiva multicriterio (HU-02).
 *
 * <p>Programado diariamente a las 02:00 AM. En caso de fallo crítico al consultar la flota activa en
 * MS Vehículos (error 5xx o timeout), reintenta a los 30 minutos con un máximo de 2 reintentos.
 */
@Component
public class PreventiveInspectionScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(PreventiveInspectionScheduler.class);

    private final ExecutePreventiveInspectionUseCase inspectionUseCase;
    private final TaskScheduler taskScheduler;
    private final long retryDelayMs;
    private final int maxRetries;
    private final AtomicInteger retryCount = new AtomicInteger(0);

    public PreventiveInspectionScheduler(
            ExecutePreventiveInspectionUseCase inspectionUseCase,
            TaskScheduler taskScheduler,
            @Value("${scheduler.preventive-inspection.retry-delay-ms:1800000}") long retryDelayMs,
            @Value("${scheduler.preventive-inspection.max-retries:2}") int maxRetries
    ) {
        this.inspectionUseCase = inspectionUseCase;
        this.taskScheduler = taskScheduler;
        this.retryDelayMs = retryDelayMs;
        this.maxRetries = maxRetries;
    }

    @Scheduled(
            cron = "${scheduler.preventive-inspection.cron:0 0 2 * * *}",
            zone = "${scheduler.preventive-inspection.zone:America/Bogota}"
    )
    public void executeDailyInspection() {
        LOG.info("Ejecutando tarea programada: Inspección preventiva diaria de las 02:00 AM");
        retryCount.set(0);
        executeWithRetryHandling();
    }

    public void executeWithRetryHandling() {
        try {
            inspectionUseCase.executeInspection();
            retryCount.set(0);
            LOG.info("Inspección preventiva diaria completada exitosamente");
        } catch (ServiceUnavailableException ex) {
            handleFailure(ex);
        } catch (Exception ex) {
            LOG.error("Error inesperado en la ejecución del scheduler de inspección preventiva: {}", ex.getMessage(), ex);
        }
    }

    private void handleFailure(ServiceUnavailableException ex) {
        int currentAttempt = retryCount.incrementAndGet();
        if (currentAttempt <= maxRetries) {
            LOG.warn("Reintento programado ({}/{}) en {} ms debido a fallo en MS Vehículos: {}",
                    currentAttempt, maxRetries, retryDelayMs, ex.getMessage());
            taskScheduler.schedule(this::executeWithRetryHandling, Instant.now().plusMillis(retryDelayMs));
        } else {
            LOG.error("Se alcanzó el límite máximo de reintentos ({}) para la inspección preventiva diaria. "
                    + "Se aborta la ejecución por hoy: {}", maxRetries, ex.getMessage());
            retryCount.set(0);
        }
    }

    public int getRetryCount() {
        return retryCount.get();
    }
}
