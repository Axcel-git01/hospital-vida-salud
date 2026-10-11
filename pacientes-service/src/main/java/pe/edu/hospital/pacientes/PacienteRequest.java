package pe.edu.hospital.pacientes;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record PacienteRequest(
        @NotBlank @Size(max = 20) String tipoDocumento,
        @NotBlank @Size(max = 20) String numeroDocumento,
        @NotBlank @Size(max = 100) String nombres,
        @NotBlank @Size(max = 100) String apellidos,
        @NotNull @Past LocalDate fechaNacimiento,
        @Size(max = 20) String telefono,
        @Email @Size(max = 120) String correo,
        @Size(max = 200) String direccion) {
}
