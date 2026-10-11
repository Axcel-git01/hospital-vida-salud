package pe.edu.hospital.citas;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record CitaRequest(
        @NotNull Long pacienteId,
        @NotBlank @Size(max = 60) String especialidad,
        @NotNull @Future LocalDateTime fechaHora,
        @NotBlank @Size(max = 20) String profesionalCodigo,
        @Size(max = 200) String motivo) {
}
