package pe.edu.hospital.citas;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/** Nueva fecha/hora de la cita. Si no se indica profesional, se conserva el actual. */
public record ReprogramarRequest(
        @NotNull @Future LocalDateTime fechaHora,
        @Size(max = 20) String profesionalCodigo) {
}
