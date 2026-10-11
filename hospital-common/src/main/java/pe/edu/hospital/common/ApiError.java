package pe.edu.hospital.common;

import java.time.Instant;

/** Formato único de error devuelto por todos los microservicios. */
public record ApiError(Instant timestamp, int status, String error, String mensaje) {
}
