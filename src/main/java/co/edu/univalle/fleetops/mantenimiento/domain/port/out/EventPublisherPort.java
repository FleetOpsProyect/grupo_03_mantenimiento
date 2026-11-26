package co.edu.univalle.fleetops.mantenimiento.domain.port.out;

public interface EventPublisherPort {
    void publish(String destination, String subject, Object payload);
}
