package pe.edu.hospital.atenciones;

/** Contrato mínimo que atenciones-service necesita de citas-service. */
public record CitaDto(Long id, Long pacienteId, String estado) {
}
