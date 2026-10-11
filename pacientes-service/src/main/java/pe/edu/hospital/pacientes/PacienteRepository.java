package pe.edu.hospital.pacientes;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface PacienteRepository extends ReactiveCrudRepository<Paciente, Long> {
    Mono<Boolean> existsByTipoDocumentoAndNumeroDocumento(String tipoDocumento, String numeroDocumento);
}
