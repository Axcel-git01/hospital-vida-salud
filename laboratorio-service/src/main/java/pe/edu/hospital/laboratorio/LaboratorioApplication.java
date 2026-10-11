package pe.edu.hospital.laboratorio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "pe.edu.hospital")
public class LaboratorioApplication {
    public static void main(String[] args) {
        SpringApplication.run(LaboratorioApplication.class, args);
    }
}
