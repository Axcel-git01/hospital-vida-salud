package pe.edu.hospital.pacientes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("paciente")
public record Paciente(
        @Id Long id,
        String tipoDocumento,
        String numeroDocumento,
        String nombres,
        String apellidos,
        LocalDate fechaNacimiento,
        String telefono,
        String correo,
        String direccion,
        LocalDateTime fechaRegistro,
        String estado) {
}
