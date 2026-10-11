package pe.edu.hospital.laboratorio;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface SolicitudRepository extends ReactiveCrudRepository<SolicitudLaboratorio, Long> {
    Flux<SolicitudLaboratorio> findByPacienteId(Long pacienteId);
}
