package pe.edu.hospital.notificaciones;

import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pe.edu.hospital.common.EventoNegocio;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);
    private final NotificacionRepository repo;

    public NotificacionService(NotificacionRepository repo) {
        this.repo = repo;
    }

    /** Convierte un evento en una notificación simulada (se guarda en BD y se escribe en el log, sin datos personales). */
    public Mono<Notificacion> procesar(EventoNegocio e) {
        Notificacion n = new Notificacion(null, e.recurso(), e.recursoId(), e.pacienteId(), mensaje(e),
                LocalDateTime.now(), "LOG");
        return repo.save(n).doOnNext(g ->
                log.info("Notificación simulada [{} #{}]: {}", g.recurso(), g.recursoId(), g.mensaje()));
    }

    public Flux<Notificacion> listar(Long pacienteId) {
        return pacienteId == null ? repo.findAll() : repo.findByPacienteId(pacienteId);
    }

    static String mensaje(EventoNegocio e) {
        return switch (e.recurso()) {
            case "CITA" -> mensajeCita(e);
            case "PAGO" -> "Su pago fue registrado";
            case "LABORATORIO" -> "Su solicitud de laboratorio cambió a " + e.estadoNuevo();
            default -> "Cambio de estado en " + e.recurso() + ": " + e.estadoNuevo();
        };
    }

    private static String mensajeCita(EventoNegocio e) {
        if ("REPROGRAMADA".equals(e.detalle())) {
            return "Su cita fue reprogramada";
        }
        return switch (e.estadoNuevo()) {
            case "PENDIENTE" -> "Su cita fue registrada";
            case "CONFIRMADA" -> "Su cita fue confirmada";
            case "CANCELADA" -> "Su cita fue cancelada";
            default -> "Su cita cambió de estado a " + e.estadoNuevo();
        };
    }
}
