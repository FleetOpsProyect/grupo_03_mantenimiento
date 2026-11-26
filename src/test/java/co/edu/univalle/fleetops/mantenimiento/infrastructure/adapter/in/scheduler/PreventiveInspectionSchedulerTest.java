package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.in.scheduler;

import co.edu.univalle.fleetops.mantenimiento.domain.exception.ServiceUnavailableException;
import co.edu.univalle.fleetops.mantenimiento.domain.port.in.ExecutePreventiveInspectionUseCase;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PreventiveInspectionSchedulerTest {

    @Mock
    private ExecutePreventiveInspectionUseCase inspectionUseCase;
    @Mock
    private TaskScheduler taskScheduler;

    private MeterRegistry meterRegistry;
    private PreventiveInspectionScheduler scheduler;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        scheduler = new PreventiveInspectionScheduler(inspectionUseCase, taskScheduler, meterRegistry, 1000L, 2);
    }

    @Test
    @DisplayName("Ejecuta la inspección preventiva diaria con éxito, registra timer y mantiene el contador en 0")
    void shouldExecuteDailyInspectionSuccessfully() {
        scheduler.executeDailyInspection();

        verify(inspectionUseCase).executeInspection();
        verify(taskScheduler, never()).schedule(any(Runnable.class), any(Instant.class));
        assertEquals(0, scheduler.getRetryCount());
        assertEquals(1, meterRegistry.timer("scheduler_duracion_segundos").count());
        assertTrue(meterRegistry.timer("scheduler_duracion_segundos").totalTime(java.util.concurrent.TimeUnit.NANOSECONDS) >= 0);
    }

    @Test
    @DisplayName("Fallo por ServiceUnavailable registra timer y programa el primer reintento en el TaskScheduler")
    void shouldScheduleFirstRetryOnServiceUnavailable() {
        doThrow(new ServiceUnavailableException("VEHICLE_SERVICE_UNAVAILABLE", "MS Vehículos caído"))
                .when(inspectionUseCase).executeInspection();

        scheduler.executeDailyInspection();

        assertEquals(1, scheduler.getRetryCount());
        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(taskScheduler).schedule(any(Runnable.class), instantCaptor.capture());
        assertNotNull(instantCaptor.getValue());
        assertEquals(1, meterRegistry.timer("scheduler_duracion_segundos").count());
    }

    @Test
    @DisplayName("Fallo consecutivo programa el segundo reintento y acumula muestras en el timer")
    void shouldScheduleSecondRetryOnConsecutiveFailure() {
        doThrow(new ServiceUnavailableException("VEHICLE_SERVICE_UNAVAILABLE", "MS Vehículos caído"))
                .when(inspectionUseCase).executeInspection();

        scheduler.executeWithRetryHandling();
        assertEquals(1, scheduler.getRetryCount());

        scheduler.executeWithRetryHandling();
        assertEquals(2, scheduler.getRetryCount());

        verify(taskScheduler, times(2)).schedule(any(Runnable.class), any(Instant.class));
        assertEquals(2, meterRegistry.timer("scheduler_duracion_segundos").count());
    }

    @Test
    @DisplayName("Superado el máximo de reintentos (2), no programa más y resetea el contador")
    void shouldAbortWhenMaxRetriesExceeded() {
        doThrow(new ServiceUnavailableException("VEHICLE_SERVICE_UNAVAILABLE", "MS Vehículos caído"))
                .when(inspectionUseCase).executeInspection();

        scheduler.executeWithRetryHandling(); // intento 1
        scheduler.executeWithRetryHandling(); // intento 2
        scheduler.executeWithRetryHandling(); // intento 3 (excede max 2)

        verify(taskScheduler, times(2)).schedule(any(Runnable.class), any(Instant.class));
        assertEquals(0, scheduler.getRetryCount());
        assertEquals(3, meterRegistry.timer("scheduler_duracion_segundos").count());
    }

    @Test
    @DisplayName("Captura errores inesperados, registra duración en timer y no propaga")
    void shouldHandleUnexpectedExceptionsGracefully() {
        doThrow(new IllegalStateException("Fallo inesperado"))
                .when(inspectionUseCase).executeInspection();

        scheduler.executeDailyInspection();

        verify(taskScheduler, never()).schedule(any(Runnable.class), any(Instant.class));
        assertEquals(0, scheduler.getRetryCount());
        assertEquals(1, meterRegistry.timer("scheduler_duracion_segundos").count());
    }
}
