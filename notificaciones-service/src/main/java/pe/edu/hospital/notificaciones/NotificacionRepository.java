package pe.edu.hospital.notificaciones;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface NotificacionRepository extends ReactiveCrudRepository<Notificacion, Long> {
    Flux<Notificacion> findByPacienteId(Long pacienteId);
}
