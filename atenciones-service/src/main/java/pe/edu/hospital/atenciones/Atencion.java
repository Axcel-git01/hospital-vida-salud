package pe.edu.hospital.atenciones;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("atencion")
public record Atencion(
        @Id Long id,
        String codigo,
        Long pacienteId,
        Long citaId,
        LocalDateTime fechaAtencion,
        String area,
        EstadoAtencion estado) {

    public Atencion conEstado(EstadoAtencion nuevo) {
        return new Atencion(id, codigo, pacienteId, citaId, fechaAtencion, area, nuevo);
    }
}
