package pe.edu.hospital.laboratorio;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("solicitud_laboratorio")
public record SolicitudLaboratorio(
        @Id Long id,
        String codigo,
        Long pacienteId,
        Long atencionId,
        String tipoExamen,
        EstadoSolicitud estado,
        String resultadoFicticio,
        LocalDateTime fechaSolicitud,
        LocalDateTime fechaActualizacion) {

    public SolicitudLaboratorio conEstado(EstadoSolicitud nuevo, String resultado) {
        return new SolicitudLaboratorio(id, codigo, pacienteId, atencionId, tipoExamen, nuevo,
                resultado != null ? resultado : resultadoFicticio, fechaSolicitud, LocalDateTime.now());
    }
}
