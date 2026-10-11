package pe.edu.hospital.pagos;

import java.time.Instant;
import java.time.LocalDateTime;
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
public class PagoService {

    private static final String MSG_DUPLICADO = "El número de operación ya fue registrado";

    private final PagoRepository repo;
    private final EventoPublisher eventos;

    public PagoService(PagoRepository repo, EventoPublisher eventos) {
        this.repo = repo;
        this.eventos = eventos;
    }

    public Mono<Pago> registrar(PagoRequest r) {
        if (r.monto() == null || r.monto().signum() <= 0) {
            return Mono.error(new ReglaNegocioException("El monto del pago debe ser mayor a cero"));
        }
        return repo.existsByNumeroOperacion(r.numeroOperacion())
                .flatMap(existe -> existe
                        ? Mono.<Pago>error(new ConflictoException(MSG_DUPLICADO))
                        : repo.save(new Pago(null, r.pacienteId(), r.concepto(), r.monto(), LocalDateTime.now(),
                                r.medioPago(), r.numeroOperacion(), "REGISTRADO")))
                .flatMap(p -> eventos.publicar("pago.registrado",
                        new EventoNegocio("PAGO", p.id(), p.pacienteId(), null, p.estado(), "pagos-service",
                                Instant.now())).thenReturn(p))
                .onErrorMap(DuplicateKeyException.class, e -> new ConflictoException(MSG_DUPLICADO));
    }

    public Mono<Pago> obtener(Long id) {
        return repo.findById(id)
                .switchIfEmpty(Mono.error(new RecursoNoEncontradoException("Pago " + id + " no existe")));
    }

    public Flux<Pago> listar(Long pacienteId) {
        return pacienteId == null ? repo.findAll() : repo.findByPacienteId(pacienteId);
    }
}
