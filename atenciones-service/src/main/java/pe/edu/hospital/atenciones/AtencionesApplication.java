package pe.edu.hospital.atenciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "pe.edu.hospital")
public class AtencionesApplication {
    public static void main(String[] args) {
        SpringApplication.run(AtencionesApplication.class, args);
    }
}
