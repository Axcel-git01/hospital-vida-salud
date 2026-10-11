package pe.edu.hospital.laboratorio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitudRequest(@NotNull Long atencionId, @NotBlank @Size(max = 60) String tipoExamen) {
}
