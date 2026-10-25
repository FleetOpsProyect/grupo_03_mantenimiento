package co.edu.univalle.fleetops.mantenimiento;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MantenimientoApplication {

    public static void main(String[] args) {
        SpringApplication.run(MantenimientoApplication.class, args);
    }
}
