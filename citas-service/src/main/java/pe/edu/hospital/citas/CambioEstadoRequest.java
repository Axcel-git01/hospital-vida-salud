package pe.edu.hospital.citas;

import jakarta.validation.constraints.NotNull;

public record CambioEstadoRequest(@NotNull EstadoCita estado) {
}
