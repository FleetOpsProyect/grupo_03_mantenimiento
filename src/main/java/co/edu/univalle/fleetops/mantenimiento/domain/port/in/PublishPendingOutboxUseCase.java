package co.edu.univalle.fleetops.mantenimiento.domain.port.in;

public interface PublishPendingOutboxUseCase {
    void publishPendingMessages();
}
