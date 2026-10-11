package pe.edu.hospital.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.Instant;

/**
 * Contrato del evento de negocio publicado en RabbitMQ.
 * No contiene datos personales: solo identificadores y estados.
 *
 * <p>{@code detalle} es opcional y precisa el tipo de cambio cuando el estado no basta
 * (por ejemplo "REPROGRAMADA": la cita cambia de fecha pero conserva su estado).
 * Los mensajes sin ese campo se leen con {@code detalle = null}.
 */
public record EventoNegocio(
        String recurso,          // CITA, PAGO, ATENCION, LABORATORIO
        Long recursoId,
        Long pacienteId,
        String estadoAnterior,   // null si es un recurso nuevo
        String estadoNuevo,
        String origen,           // microservicio que publica
        Instant fecha,
        String detalle) {        // opcional; null en cambios de estado "normales"

    /** Constructor canonico marcado de forma explicita para la (de)serializacion JSON. */
    @JsonCreator
    public EventoNegocio {
    }

    /** Constructor para eventos sin detalle (cambios de estado normales). */
    public EventoNegocio(String recurso, Long recursoId, Long pacienteId, String estadoAnterior,
                         String estadoNuevo, String origen, Instant fecha) {
        this(recurso, recursoId, pacienteId, estadoAnterior, estadoNuevo, origen, fecha, null);
    }
}
