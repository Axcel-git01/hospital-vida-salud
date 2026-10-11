package pe.edu.hospital.citas;

/** Contrato mínimo que citas-service necesita de pacientes-service. */
public record PacienteDto(Long id, String nombres, String apellidos, String estado) {
}
