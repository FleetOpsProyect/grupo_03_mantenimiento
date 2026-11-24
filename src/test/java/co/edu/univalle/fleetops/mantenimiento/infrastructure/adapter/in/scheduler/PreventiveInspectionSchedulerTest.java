package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.in.scheduler;

import co.edu.univalle.fleetops.mantenimiento.domain.exception.ServiceUnavailableException;
import co.edu.univalle.fleetops.mantenimiento.domain.port.in.ExecutePreventiveInspectionUseCase;
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

    private PreventiveInspectionScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new PreventiveInspectionScheduler(inspectionUseCase, taskScheduler, 1000L, 2);
    }

    @Test
    @DisplayName("Ejecuta la inspección preventiva diaria con éxito y mantiene el contador en 0")
    void shouldExecuteDailyInspectionSuccessfully() {
        scheduler.executeDailyInspection();

        verify(inspectionUseCase).executeInspection();
        verify(taskScheduler, never()).schedule(any(Runnable.class), any(Instant.class));
        assertEquals(0, scheduler.getRetryCount());
    }

    @Test
    @DisplayName("Fallo por ServiceUnavailable programa el primer reintento en el TaskScheduler")
    void shouldScheduleFirstRetryOnServiceUnavailable() {
        doThrow(new ServiceUnavailableException("VEHICLE_SERVICE_UNAVAILABLE", "MS Vehículos caído"))
                .when(inspectionUseCase).executeInspection();

        scheduler.executeDailyInspection();

        assertEquals(1, scheduler.getRetryCount());
        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(taskScheduler).schedule(any(Runnable.class), instantCaptor.capture());
        assertNotNull(instantCaptor.getValue());
    }

    @Test
    @DisplayName("Fallo consecutivo programa el segundo reintento")
    void shouldScheduleSecondRetryOnConsecutiveFailure() {
        doThrow(new ServiceUnavailableException("VEHICLE_SERVICE_UNAVAILABLE", "MS Vehículos caído"))
                .when(inspectionUseCase).executeInspection();

        scheduler.executeWithRetryHandling();
        assertEquals(1, scheduler.getRetryCount());

        scheduler.executeWithRetryHandling();
        assertEquals(2, scheduler.getRetryCount());

        verify(taskScheduler, times(2)).schedule(any(Runnable.class), any(Instant.class));
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
    }

    @Test
    @DisplayName("Captura y no propaga errores inesperados no controlados")
    void shouldHandleUnexpectedExceptionsGracefully() {
        doThrow(new IllegalStateException("Fallo inesperado"))
                .when(inspectionUseCase).executeInspection();

        scheduler.executeDailyInspection();

        verify(taskScheduler, never()).schedule(any(Runnable.class), any(Instant.class));
        assertEquals(0, scheduler.getRetryCount());
    }
}
