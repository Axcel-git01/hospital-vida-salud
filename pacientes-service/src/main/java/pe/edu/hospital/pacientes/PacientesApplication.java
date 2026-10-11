package pe.edu.hospital.pacientes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "pe.edu.hospital")
public class PacientesApplication {
    public static void main(String[] args) {
        SpringApplication.run(PacientesApplication.class, args);
    }
}
