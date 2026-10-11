package pe.edu.hospital.citas;

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
public class CitaService {

    private static final String MSG_HORARIO_OCUPADO =
            "El horario ya fue ocupado por otra cita (profesional o paciente)";

    private final CitaRepository repo;
    private final PacientesClient pacientes;
    private final EventoPublisher eventos;

    public CitaService(CitaRepository repo, PacientesClient pacientes, EventoPublisher eventos) {
        this.repo = repo;
        this.pacientes = pacientes;
        this.eventos = eventos;
    }

    public Mono<Cita> crear(CitaRequest r) {
        return pacientes.obtener(r.pacienteId())                       // llamada síncrona (WebClient)
                .flatMap(p -> "ACTIVO".equals(p.estado())
                        ? Mono.<Void>empty()
                        : Mono.<Void>error(new ReglaNegocioException("El paciente no está ACTIVO")))
                .then(validarAgenda(r))
                .then(Mono.defer(() -> repo.save(nueva(r))
                        .onErrorMap(DuplicateKeyException.class, e -> new ConflictoException(MSG_HORARIO_OCUPADO))))
                .flatMap(c -> publicar(c, null).thenReturn(c));
    }

    public Mono<Cita> obtener(Long id) {
        return repo.findById(id)
                .switchIfEmpty(Mono.error(new RecursoNoEncontradoException("Cita " + id + " no existe")));
    }

    public Flux<Cita> listar(Long pacienteId) {
        return pacienteId == null ? repo.findAll() : repo.findByPacienteId(pacienteId);
    }

    public Mono<Cita> cambiarEstado(Long id, EstadoCita nuevo) {
        return obtener(id).flatMap(c -> {
            if (!c.estado().puedeTransicionarA(nuevo)) {
                return Mono.<Cita>error(new ReglaNegocioException(
                        "Transición no permitida: " + c.estado() + " -> " + nuevo));
            }
            return repo.save(c.conEstado(nuevo)).flatMap(g -> publicar(g, c.estado()).thenReturn(g));
        });
    }

    /**
     * Cambia fecha/hora (y opcionalmente profesional) de una cita vigente. El estado no cambia.
     * Revalida la agenda excluyendo la propia cita y publica el evento cita.reprogramada.
     */
    public Mono<Cita> reprogramar(Long id, ReprogramarRequest r) {
        return obtener(id).flatMap(c -> {
            if (!c.estado().admiteReprogramacion()) {
                return Mono.<Cita>error(new ReglaNegocioException(
                        "Solo se pueden reprogramar citas PENDIENTE o CONFIRMADA (estado actual: " + c.estado() + ")"));
            }
            String profesional = (r.profesionalCodigo() == null || r.profesionalCodigo().isBlank())
                    ? c.profesionalCodigo() : r.profesionalCodigo();
            if (c.fechaHora().equals(r.fechaHora()) && c.profesionalCodigo().equals(profesional)) {
                return Mono.<Cita>error(new ReglaNegocioException(
                        "La nueva fecha y el profesional son iguales a los actuales"));
            }
            return validarAgendaReprogramacion(c, r.fechaHora(), profesional)
                    .then(Mono.defer(() -> repo.save(c.reprogramada(r.fechaHora(), profesional))))
                    .onErrorMap(DuplicateKeyException.class, e -> new ConflictoException(MSG_HORARIO_OCUPADO))
                    .flatMap(g -> publicarReprogramacion(g).thenReturn(g));
        });
    }

    private Mono<Void> validarAgendaReprogramacion(Cita c, LocalDateTime fechaHora, String profesional) {
        return Mono.defer(() -> repo.existsByProfesionalCodigoAndFechaHoraAndEstadoNotAndIdNot(
                        profesional, fechaHora, EstadoCita.CANCELADA, c.id()))
                .flatMap(ocupado -> ocupado
                        ? Mono.<Void>error(new ConflictoException("El profesional ya tiene una cita en ese horario"))
                        : Mono.<Void>empty())
                .then(Mono.defer(() -> repo.existsByPacienteIdAndFechaHoraAndEstadoNotAndIdNot(
                        c.pacienteId(), fechaHora, EstadoCita.CANCELADA, c.id())))
                .flatMap(duplicada -> duplicada
                        ? Mono.<Void>error(new ConflictoException("El paciente ya tiene una cita en ese horario"))
                        : Mono.<Void>empty());
    }

    private Mono<Void> validarAgenda(CitaRequest r) {
        return Mono.defer(() -> repo.existsByProfesionalCodigoAndFechaHoraAndEstadoNot(
                        r.profesionalCodigo(), r.fechaHora(), EstadoCita.CANCELADA))
                .flatMap(ocupado -> ocupado
                        ? Mono.<Void>error(new ConflictoException("El profesional ya tiene una cita en ese horario"))
                        : Mono.<Void>empty())
                .then(Mono.defer(() -> repo.existsByPacienteIdAndFechaHoraAndEstadoNot(
                        r.pacienteId(), r.fechaHora(), EstadoCita.CANCELADA)))
                .flatMap(duplicada -> duplicada
                        ? Mono.<Void>error(new ConflictoException("El paciente ya tiene una cita en ese horario"))
                        : Mono.<Void>empty());
    }

    private Cita nueva(CitaRequest r) {
        LocalDateTime ahora = LocalDateTime.now();
        String codigo = "CIT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new Cita(null, codigo, r.pacienteId(), r.especialidad(), r.fechaHora(), r.profesionalCodigo(),
                r.motivo(), EstadoCita.PENDIENTE, ahora, ahora);
    }

    private Mono<Void> publicar(Cita c, EstadoCita anterior) {
        EventoNegocio evento = new EventoNegocio("CITA", c.id(), c.pacienteId(),
                anterior == null ? null : anterior.name(), c.estado().name(), "citas-service", Instant.now());
        return eventos.publicar("cita." + c.estado().name().toLowerCase(), evento);
    }

    private Mono<Void> publicarReprogramacion(Cita c) {
        EventoNegocio evento = new EventoNegocio("CITA", c.id(), c.pacienteId(), c.estado().name(),
                c.estado().name(), "citas-service", Instant.now(), "REPROGRAMADA");
        return eventos.publicar("cita.reprogramada", evento);
    }
}
