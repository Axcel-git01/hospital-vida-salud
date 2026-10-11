package pe.edu.hospital.atenciones;

import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import pe.edu.hospital.common.RecursoNoEncontradoException;
import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class AtencionControllerTest {

    @Autowired
    WebTestClient client;
    @Autowired
    AtencionRepository repo;
    @MockBean
    CitasClient citas;
    @MockBean
    EventoPublisher eventos;

    @BeforeEach
    void preparar() {
        repo.deleteAll().block();
        when(citas.obtener(3L)).thenReturn(Mono.just(new CitaDto(3L, 1L, "CONFIRMADA")));
        when(citas.obtener(4L)).thenReturn(Mono.just(new CitaDto(4L, 1L, "PENDIENTE")));
        when(citas.obtener(404L)).thenReturn(Mono.error(new RecursoNoEncontradoException("Cita 404 no existe")));
        when(eventos.publicar(anyString(), any(EventoNegocio.class))).thenReturn(Mono.empty());
    }

    @Test
    void registraYConsultaAtencion() {
        Atencion creada = client.post().uri("/api/atenciones")
                .bodyValue(Map.of("citaId", 3, "area", "Consulta externa")).exchange()
                .expectStatus().isCreated().expectBody(Atencion.class).returnResult().getResponseBody();
        assertNotNull(creada);
        client.get().uri("/api/atenciones/{id}", creada.id()).exchange()
                .expectStatus().isOk().expectBody().jsonPath("$.citaId").isEqualTo(3);
    }

    @Test
    void citaNoConfirmadaDevuelve422() {
        client.post().uri("/api/atenciones").bodyValue(Map.of("citaId", 4, "area", "Consulta externa"))
                .exchange().expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void citaInexistenteDevuelve404() {
        client.post().uri("/api/atenciones").bodyValue(Map.of("citaId", 404, "area", "Consulta externa"))
                .exchange().expectStatus().isNotFound();
    }

    @Test
    void atencionDuplicadaDevuelve409() {
        client.post().uri("/api/atenciones").bodyValue(Map.of("citaId", 3, "area", "Consulta externa"))
                .exchange().expectStatus().isCreated();
        client.post().uri("/api/atenciones").bodyValue(Map.of("citaId", 3, "area", "Consulta externa"))
                .exchange().expectStatus().isEqualTo(HttpStatus.CONFLICT);
    }
}
