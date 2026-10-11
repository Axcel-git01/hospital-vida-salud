package pe.edu.hospital.atenciones;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import pe.edu.hospital.common.RecursoNoEncontradoException;
import pe.edu.hospital.common.ServicioNoDisponibleException;
import reactor.core.publisher.Mono;

/** Contrato: GET /api/citas/{id} -> 200 | 404. Timeout 3 s; indisponibilidad -> 503. */
@Component
public class CitasClient {

    private final WebClient webClient;

    public CitasClient(@Qualifier("citasWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<CitaDto> obtener(Long citaId) {
        return webClient.get().uri("/api/citas/{id}", citaId)
                .retrieve()
                .onStatus(s -> s.value() == 404,
                        r -> Mono.error(new RecursoNoEncontradoException("Cita " + citaId + " no existe")))
                .bodyToMono(CitaDto.class)
                .timeout(Duration.ofSeconds(3))
                .onErrorMap(e -> !(e instanceof RecursoNoEncontradoException),
                        e -> new ServicioNoDisponibleException("El servicio de citas no está disponible"));
    }
}
