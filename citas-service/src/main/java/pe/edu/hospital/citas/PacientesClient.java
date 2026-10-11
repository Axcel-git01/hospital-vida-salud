package pe.edu.hospital.citas;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import pe.edu.hospital.common.RecursoNoEncontradoException;
import pe.edu.hospital.common.ServicioNoDisponibleException;
import reactor.core.publisher.Mono;

/**
 * Comunicación síncrona con pacientes-service.
 * Contrato: GET /api/pacientes/{id} -> 200 con el paciente | 404 si no existe.
 * Timeout: 3 s. Sin reintentos. Si el servicio cae o excede el tiempo -> 503 hacia el cliente.
 */
@Component
public class PacientesClient {

    private final WebClient webClient;

    public PacientesClient(@Qualifier("pacientesWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<PacienteDto> obtener(Long pacienteId) {
        return webClient.get().uri("/api/pacientes/{id}", pacienteId)
                .retrieve()
                .onStatus(status -> status.value() == 404,
                        resp -> Mono.error(new RecursoNoEncontradoException("Paciente " + pacienteId + " no existe")))
                .bodyToMono(PacienteDto.class)
                .timeout(Duration.ofSeconds(3))
                .onErrorMap(e -> !(e instanceof RecursoNoEncontradoException),
                        e -> new ServicioNoDisponibleException("El servicio de pacientes no está disponible"));
    }
}
