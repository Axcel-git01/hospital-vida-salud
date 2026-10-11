package pe.edu.hospital.citas;

import java.time.LocalDateTime;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CitaRepository extends ReactiveCrudRepository<Cita, Long> {
    Flux<Cita> findByPacienteId(Long pacienteId);

    Mono<Boolean> existsByProfesionalCodigoAndFechaHoraAndEstadoNot(
            String profesionalCodigo, LocalDateTime fechaHora, EstadoCita estado);

    Mono<Boolean> existsByPacienteIdAndFechaHoraAndEstadoNot(
            Long pacienteId, LocalDateTime fechaHora, EstadoCita estado);

    /** Variantes que excluyen la propia cita (para reprogramar sin chocar consigo misma). */
    Mono<Boolean> existsByProfesionalCodigoAndFechaHoraAndEstadoNotAndIdNot(
            String profesionalCodigo, LocalDateTime fechaHora, EstadoCita estado, Long id);

    Mono<Boolean> existsByPacienteIdAndFechaHoraAndEstadoNotAndIdNot(
            Long pacienteId, LocalDateTime fechaHora, EstadoCita estado, Long id);
}
