package pe.edu.hospital.laboratorio;

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
class SolicitudControllerTest {

    @Autowired
    WebTestClient client;
    @Autowired
    SolicitudRepository repo;
    @MockBean
    AtencionesClient atenciones;
    @MockBean
    EventoPublisher eventos;

    @BeforeEach
    void preparar() {
        repo.deleteAll().block();
        when(atenciones.obtener(9L)).thenReturn(Mono.just(new AtencionDto(9L, 1L, "REGISTRADA")));
        when(atenciones.obtener(404L)).thenReturn(Mono.error(new RecursoNoEncontradoException("Atención 404 no existe")));
        when(eventos.publicar(anyString(), any(EventoNegocio.class))).thenReturn(Mono.empty());
    }

    @Test
    void recorreElCicloDeEstadosConResultadoFicticio() {
        SolicitudLaboratorio creada = client.post().uri("/api/laboratorio")
                .bodyValue(Map.of("atencionId", 9, "tipoExamen", "Examen de demostración")).exchange()
                .expectStatus().isCreated().expectBody(SolicitudLaboratorio.class).returnResult().getResponseBody();
        assertNotNull(creada);

        client.patch().uri("/api/laboratorio/{id}/estado", creada.id()).bodyValue(Map.of("estado", "EN_PROCESO"))
                .exchange().expectStatus().isOk();
        client.patch().uri("/api/laboratorio/{id}/estado", creada.id())
                .bodyValue(Map.of("estado", "COMPLETADO", "resultadoFicticio", "Valor de prueba"))
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.resultadoFicticio").isEqualTo("Valor de prueba");
    }

    @Test
    void transicionInvalidaDevuelve422() {
        SolicitudLaboratorio creada = client.post().uri("/api/laboratorio")
                .bodyValue(Map.of("atencionId", 9, "tipoExamen", "Examen de demostración")).exchange()
                .expectStatus().isCreated().expectBody(SolicitudLaboratorio.class).returnResult().getResponseBody();
        assertNotNull(creada);

        client.patch().uri("/api/laboratorio/{id}/estado", creada.id()).bodyValue(Map.of("estado", "COMPLETADO"))
                .exchange().expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void atencionInexistenteDevuelve404() {
        client.post().uri("/api/laboratorio").bodyValue(Map.of("atencionId", 404, "tipoExamen", "X"))
                .exchange().expectStatus().isNotFound();
    }
}
