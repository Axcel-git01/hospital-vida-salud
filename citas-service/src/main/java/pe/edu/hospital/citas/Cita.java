package pe.edu.hospital.citas;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("cita")
public record Cita(
        @Id Long id,
        String codigo,
        Long pacienteId,
        String especialidad,
        LocalDateTime fechaHora,
        String profesionalCodigo,
        String motivo,
        EstadoCita estado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion) {

    public Cita conEstado(EstadoCita nuevo) {
        return new Cita(id, codigo, pacienteId, especialidad, fechaHora, profesionalCodigo, motivo,
                nuevo, fechaCreacion, LocalDateTime.now());
    }

    public Cita reprogramada(LocalDateTime nuevaFechaHora, String nuevoProfesional) {
        return new Cita(id, codigo, pacienteId, especialidad, nuevaFechaHora, nuevoProfesional, motivo,
                estado, fechaCreacion, LocalDateTime.now());
    }
}
