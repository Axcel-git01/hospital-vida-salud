package pe.edu.hospital.auditoria;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("registro_auditoria")
public record RegistroAuditoria(
        @Id Long id,
        String recurso,
        Long recursoId,
        String estadoAnterior,
        String estadoNuevo,
        String origen,
        LocalDateTime fechaEvento,
        LocalDateTime fechaRegistro,
        String detalle) {
}
