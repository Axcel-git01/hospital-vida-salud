package pe.edu.hospital.atenciones;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AtencionRepository extends ReactiveCrudRepository<Atencion, Long> {
    Mono<Boolean> existsByCitaIdAndEstadoNot(Long citaId, EstadoAtencion estado);

    Flux<Atencion> findByPacienteId(Long pacienteId);
}
