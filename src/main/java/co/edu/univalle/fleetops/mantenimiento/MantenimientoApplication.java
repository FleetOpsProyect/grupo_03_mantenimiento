package co.edu.univalle.fleetops.mantenimiento;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

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
        Path envPath = Paths.get(".env");
        if (!Files.exists(envPath)) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(envPath);
            for (String line : lines) {
                parseAndSetEnvLine(line);
            }
        } catch (Exception ignored) {
            // Si falla la lectura de .env, Spring Boot usará los fallbacks o variables del sistema
        }
    }

    private static void parseAndSetEnvLine(String rawLine) {
        String line = rawLine.trim();
        if (line.isEmpty() || line.startsWith("#")) {
            return;
        }
        int eqIdx = line.indexOf('=');
        if (eqIdx <= 0) {
            return;
        }
        String key = line.substring(0, eqIdx).trim();
        String value = line.substring(eqIdx + 1).trim();
        if (System.getProperty(key) == null && System.getenv(key) == null) {
            System.setProperty(key, value);
        }
    }
}