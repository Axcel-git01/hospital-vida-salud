package pe.edu.hospital.atenciones;

import jakarta.validation.constraints.NotNull;

public record CambioEstadoRequest(@NotNull EstadoAtencion estado) {
}
