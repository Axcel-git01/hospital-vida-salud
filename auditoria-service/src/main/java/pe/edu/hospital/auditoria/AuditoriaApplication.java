package pe.edu.hospital.auditoria;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "pe.edu.hospital")
public class AuditoriaApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuditoriaApplication.class, args);
    }
}
