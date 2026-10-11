package pe.edu.hospital.laboratorio;

/** Contrato mínimo que laboratorio-service necesita de atenciones-service. */
public record AtencionDto(Long id, Long pacienteId, String estado) {
}
