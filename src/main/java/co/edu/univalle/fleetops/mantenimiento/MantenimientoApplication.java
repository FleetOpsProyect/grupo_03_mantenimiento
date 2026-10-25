package co.edu.univalle.fleetops.mantenimiento;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MantenimientoApplication {

    static {
        loadEnv();
    }

    public static void main(String[] args) {
        SpringApplication.run(MantenimientoApplication.class, args);
    }

    private static void loadEnv() {
        java.nio.file.Path envPath = java.nio.file.Paths.get(".env");
        if (java.nio.file.Files.exists(envPath)) {
            try {
                java.util.List<String> lines = java.nio.file.Files.readAllLines(envPath);
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    int eqIdx = line.indexOf('=');
                    if (eqIdx > 0) {
                        String key = line.substring(0, eqIdx).trim();
                        String value = line.substring(eqIdx + 1).trim();
                        if (System.getProperty(key) == null && System.getenv(key) == null) {
                            System.setProperty(key, value);
                        }
                    }
                }
            } catch (Exception ignored) {
                // Si ocurre algún problema leyendo .env, Spring Boot utilizará los fallbacks o variables de entorno del sistema
            }
        }
    }
}
