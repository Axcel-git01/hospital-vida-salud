package pe.edu.hospital.laboratorio;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** El resultado es ficticio y solo se exige al completar la solicitud. */
public record CambioEstadoRequest(@NotNull EstadoSolicitud estado, @Size(max = 200) String resultadoFicticio) {
}
