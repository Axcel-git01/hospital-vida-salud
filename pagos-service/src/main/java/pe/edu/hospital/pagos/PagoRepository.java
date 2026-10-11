package pe.edu.hospital.pagos;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PagoRepository extends ReactiveCrudRepository<Pago, Long> {
    Mono<Boolean> existsByNumeroOperacion(String numeroOperacion);

    Flux<Pago> findByPacienteId(Long pacienteId);
}
