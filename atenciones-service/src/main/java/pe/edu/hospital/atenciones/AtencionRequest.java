package pe.edu.hospital.atenciones;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AtencionRequest(@NotNull Long citaId, @NotBlank @Size(max = 60) String area) {
}
