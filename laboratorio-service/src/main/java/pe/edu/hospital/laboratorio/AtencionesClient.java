package pe.edu.hospital.laboratorio;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import pe.edu.hospital.common.RecursoNoEncontradoException;
import pe.edu.hospital.common.ServicioNoDisponibleException;
import reactor.core.publisher.Mono;

/** Contrato: GET /api/atenciones/{id} -> 200 | 404. Timeout 3 s; indisponibilidad -> 503. */
@Component
public class AtencionesClient {

    private final WebClient webClient;

    public AtencionesClient(@Qualifier("atencionesWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<AtencionDto> obtener(Long atencionId) {
        return webClient.get().uri("/api/atenciones/{id}", atencionId)
                .retrieve()
                .onStatus(s -> s.value() == 404,
                        r -> Mono.error(new RecursoNoEncontradoException("Atención " + atencionId + " no existe")))
                .bodyToMono(AtencionDto.class)
                .timeout(Duration.ofSeconds(3))
                .onErrorMap(e -> !(e instanceof RecursoNoEncontradoException),
                        e -> new ServicioNoDisponibleException("El servicio de atenciones no está disponible"));
    }
}
