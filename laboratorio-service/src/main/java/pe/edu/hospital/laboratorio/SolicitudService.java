package pe.edu.hospital.laboratorio;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import pe.edu.hospital.common.EventoNegocio;
import pe.edu.hospital.common.EventoPublisher;
import pe.edu.hospital.common.RecursoNoEncontradoException;
import pe.edu.hospital.common.ReglaNegocioException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class SolicitudService {

    private final SolicitudRepository repo;
    private final AtencionesClient atenciones;
    private final EventoPublisher eventos;

    public SolicitudService(SolicitudRepository repo, AtencionesClient atenciones, EventoPublisher eventos) {
        this.repo = repo;
        this.atenciones = atenciones;
        this.eventos = eventos;
    }

    /** La solicitud exige una atención existente y no anulada; no hay FK entre bases: solo el identificador. */
    public Mono<SolicitudLaboratorio> crear(SolicitudRequest r) {
        return atenciones.obtener(r.atencionId())
                .flatMap(a -> "ANULADA".equals(a.estado())
                        ? Mono.<AtencionDto>error(new ReglaNegocioException("La atención está ANULADA"))
                        : Mono.just(a))
                .flatMap(a -> {
                    LocalDateTime ahora = LocalDateTime.now();
                    return repo.save(new SolicitudLaboratorio(null,
                            "LAB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                            a.pacienteId(), a.id(), r.tipoExamen(), EstadoSolicitud.SOLICITADO, null, ahora, ahora));
                })
                .flatMap(s -> publicar(s, null).thenReturn(s));
    }

    public Mono<SolicitudLaboratorio> obtener(Long id) {
        return repo.findById(id)
                .switchIfEmpty(Mono.error(new RecursoNoEncontradoException("Solicitud " + id + " no existe")));
    }

    public Flux<SolicitudLaboratorio> listar(Long pacienteId) {
        return pacienteId == null ? repo.findAll() : repo.findByPacienteId(pacienteId);
    }

    public Mono<SolicitudLaboratorio> cambiarEstado(Long id, EstadoSolicitud nuevo, String resultado) {
        return obtener(id).flatMap(s -> {
            if (!s.estado().puedeTransicionarA(nuevo)) {
                return Mono.<SolicitudLaboratorio>error(new ReglaNegocioException(
                        "Transición no permitida: " + s.estado() + " -> " + nuevo));
            }
            if (nuevo == EstadoSolicitud.COMPLETADO && (resultado == null || resultado.isBlank())) {
                return Mono.<SolicitudLaboratorio>error(new ReglaNegocioException(
                        "Debe indicar un resultado ficticio para completar la solicitud"));
            }
            return repo.save(s.conEstado(nuevo, resultado)).flatMap(g -> publicar(g, s.estado()).thenReturn(g));
        });
    }

    private Mono<Void> publicar(SolicitudLaboratorio s, EstadoSolicitud anterior) {
        return eventos.publicar("laboratorio." + s.estado().name().toLowerCase(),
                new EventoNegocio("LABORATORIO", s.id(), s.pacienteId(),
                        anterior == null ? null : anterior.name(), s.estado().name(),
                        "laboratorio-service", Instant.now()));
    }
}
