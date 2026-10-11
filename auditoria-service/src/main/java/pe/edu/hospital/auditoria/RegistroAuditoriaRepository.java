package pe.edu.hospital.auditoria;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface RegistroAuditoriaRepository extends ReactiveCrudRepository<RegistroAuditoria, Long> {
    Flux<RegistroAuditoria> findByRecursoAndRecursoIdOrderByFechaEventoAsc(String recurso, Long recursoId);
}
