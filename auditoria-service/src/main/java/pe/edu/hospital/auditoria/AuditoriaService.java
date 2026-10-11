package pe.edu.hospital.auditoria;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import pe.edu.hospital.common.EventoNegocio;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class AuditoriaService {

    private final RegistroAuditoriaRepository repo;

    public AuditoriaService(RegistroAuditoriaRepository repo) {
        this.repo = repo;
    }

    /** Guarda: fecha, recurso, estado anterior, estado nuevo y origen del cambio. */
    public Mono<RegistroAuditoria> procesar(EventoNegocio e) {
        return repo.save(new RegistroAuditoria(null, e.recurso(), e.recursoId(), e.estadoAnterior(),
                e.estadoNuevo(), e.origen(), LocalDateTime.ofInstant(e.fecha(), ZoneOffset.UTC),
                LocalDateTime.now(ZoneOffset.UTC), e.detalle()));
    }

    public Flux<RegistroAuditoria> listar() {
        return repo.findAll();
    }

    public Flux<RegistroAuditoria> historial(String recurso, Long recursoId) {
        return repo.findByRecursoAndRecursoIdOrderByFechaEventoAsc(recurso.toUpperCase(), recursoId);
    }
}
