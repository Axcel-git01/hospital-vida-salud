package pe.edu.hospital.pagos;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import pe.edu.hospital.common.EventoNegocio;
import pe.edu.hospital.common.EventoPublisher;
import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class PagoControllerTest {

    @Autowired
    WebTestClient client;
    @Autowired
    PagoRepository repo;
    @MockBean
    EventoPublisher eventos;

    @BeforeEach
    void preparar() {
        repo.deleteAll().block();
        when(eventos.publicar(anyString(), any(EventoNegocio.class))).thenReturn(Mono.empty());
    }

    private Map<String, Object> cuerpo(Object monto, String operacion) {
        return Map.of("pacienteId", 1, "concepto", "Consulta de ejemplo", "monto", monto,
                "medioPago", "TARJETA", "numeroOperacion", operacion);
    }

    @Test
    void registraPagoValido() {
        client.post().uri("/api/pagos").bodyValue(cuerpo(50.5, "OP-A1")).exchange()
                .expectStatus().isCreated().expectBody().jsonPath("$.estado").isEqualTo("REGISTRADO");
    }

    @Test
    void montoCeroONegativoDevuelve400() {
        client.post().uri("/api/pagos").bodyValue(cuerpo(0, "OP-B1")).exchange().expectStatus().isBadRequest();
        client.post().uri("/api/pagos").bodyValue(cuerpo(-10, "OP-B2")).exchange().expectStatus().isBadRequest();
    }

    @Test
    void operacionDuplicadaDevuelve409() {
        client.post().uri("/api/pagos").bodyValue(cuerpo(20, "OP-C1")).exchange().expectStatus().isCreated();
        client.post().uri("/api/pagos").bodyValue(cuerpo(20, "OP-C1")).exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void pagoInexistenteDevuelve404() {
        client.get().uri("/api/pagos/{id}", 9999).exchange().expectStatus().isNotFound();
    }
}
