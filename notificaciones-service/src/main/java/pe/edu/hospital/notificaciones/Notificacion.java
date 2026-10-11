package pe.edu.hospital.notificaciones;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("notificacion")
public record Notificacion(
        @Id Long id,
        String recurso,
        Long recursoId,
        Long pacienteId,
        String mensaje,
        LocalDateTime fechaEnvio,
        String canal) {
}
