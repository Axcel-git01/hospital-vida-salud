package pe.edu.hospital.atenciones;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import pe.edu.hospital.common.ConflictoException;
import pe.edu.hospital.common.EventoNegocio;
import pe.edu.hospital.common.EventoPublisher;
import pe.edu.hospital.common.RecursoNoEncontradoException;
import pe.edu.hospital.common.ReglaNegocioException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class AtencionService {

    private static final String MSG_DUP = "Ya existe una atención vigente para esa cita";

    private final AtencionRepository repo;
    private final CitasClient citas;
    private final EventoPublisher eventos;

    public AtencionService(AtencionRepository repo, CitasClient citas, EventoPublisher eventos) {
        this.repo = repo;
        this.citas = citas;
        this.eventos = eventos;
    }

    /** Una atención solo se registra sobre una cita válida (existente y CONFIRMADA). */
    public Mono<Atencion> crear(AtencionRequest r) {
        return citas.obtener(r.citaId())
                .flatMap(c -> "CONFIRMADA".equals(c.estado())
                        ? Mono.just(c)
                        : Mono.<CitaDto>error(new ReglaNegocioException("La cita debe estar CONFIRMADA")))
                .flatMap(c -> repo.existsByCitaIdAndEstadoNot(c.id(), EstadoAtencion.ANULADA)
                        .flatMap(existe -> existe
                                ? Mono.<Atencion>error(new ConflictoException(MSG_DUP))
                                : repo.save(new Atencion(null,
                                        "ATN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                                        c.pacienteId(), c.id(), LocalDateTime.now(), r.area(),
                                        EstadoAtencion.REGISTRADA))))
                .flatMap(a -> publicar(a, null).thenReturn(a))
                .onErrorMap(DuplicateKeyException.class, e -> new ConflictoException(MSG_DUP));
    }

    public Mono<Atencion> obtener(Long id) {
        return repo.findById(id)
                .switchIfEmpty(Mono.error(new RecursoNoEncontradoException("Atención " + id + " no existe")));
    }

    public Flux<Atencion> listar(Long pacienteId) {
        return pacienteId == null ? repo.findAll() : repo.findByPacienteId(pacienteId);
    }

    public Mono<Atencion> cambiarEstado(Long id, EstadoAtencion nuevo) {
        return obtener(id).flatMap(a -> {
            if (!a.estado().puedeTransicionarA(nuevo)) {
                return Mono.<Atencion>error(new ReglaNegocioException(
                        "Transición no permitida: " + a.estado() + " -> " + nuevo));
            }
            return repo.save(a.conEstado(nuevo)).flatMap(g -> publicar(g, a.estado()).thenReturn(g));
        });
    }

    private Mono<Void> publicar(Atencion a, EstadoAtencion anterior) {
        return eventos.publicar("atencion." + a.estado().name().toLowerCase(),
                new EventoNegocio("ATENCION", a.id(), a.pacienteId(),
                        anterior == null ? null : anterior.name(), a.estado().name(),
                        "atenciones-service", Instant.now()));
    }
}
