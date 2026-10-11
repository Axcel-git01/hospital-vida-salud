package pe.edu.hospital.citas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "pe.edu.hospital")
public class CitasApplication {
    public static void main(String[] args) {
        SpringApplication.run(CitasApplication.class, args);
    }
}
