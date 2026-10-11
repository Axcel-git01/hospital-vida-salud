package pe.edu.hospital.citas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
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

/** Integración del endpoint con R2DBC/H2; pacientes-service y RabbitMQ se simulan. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class CitaControllerTest {

    @Autowired
    WebTestClient client;
    @Autowired
    CitaRepository repo;
    @MockBean
    PacientesClient pacientes;
    @MockBean
    EventoPublisher eventos;

    @BeforeEach
    void preparar() {
        repo.deleteAll().block();
        when(pacientes.obtener(1L)).thenReturn(Mono.just(new PacienteDto(1L, "Ana", "Prueba", "ACTIVO")));
        when(pacientes.obtener(404L))
                .thenReturn(Mono.error(new RecursoNoEncontradoException("Paciente 404 no existe")));
        when(eventos.publicar(anyString(), any(EventoNegocio.class))).thenReturn(Mono.empty());
    }

    private Map<String, Object> cuerpo(long pacienteId, String profesional) {
        LocalDateTime fecha = LocalDateTime.now().plusDays(3).withHour(10).withMinute(0).withSecond(0).withNano(0);
        return Map.of("pacienteId", pacienteId, "especialidad", "Medicina General",
                "fechaHora", fecha.toString(), "profesionalCodigo", profesional,
                "motivo", "Control administrativo");
    }

    @Test
    void registraConsultaYConfirmaCita() {
        Cita creada = client.post().uri("/api/citas").bodyValue(cuerpo(1L, "PROF-001")).exchange()
                .expectStatus().isCreated()
                .expectBody(Cita.class).returnResult().getResponseBody();
        assertNotNull(creada);
        assertEquals(EstadoCita.PENDIENTE, creada.estado());

        client.get().uri("/api/citas/{id}", creada.id()).exchange()
                .expectStatus().isOk().expectBody().jsonPath("$.pacienteId").isEqualTo(1);

        client.patch().uri("/api/citas/{id}/estado", creada.id()).bodyValue(Map.of("estado", "CONFIRMADA"))
                .exchange().expectStatus().isOk().expectBody().jsonPath("$.estado").isEqualTo("CONFIRMADA");
    }

    @Test
    void transicionNoPermitidaDevuelve422() {
        Cita creada = client.post().uri("/api/citas").bodyValue(cuerpo(1L, "PROF-002")).exchange()
                .expectStatus().isCreated().expectBody(Cita.class).returnResult().getResponseBody();
        assertNotNull(creada);

        client.patch().uri("/api/citas/{id}/estado", creada.id()).bodyValue(Map.of("estado", "CANCELADA"))
                .exchange().expectStatus().isOk();
        client.patch().uri("/api/citas/{id}/estado", creada.id()).bodyValue(Map.of("estado", "CONFIRMADA"))
                .exchange().expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void agendaOcupadaDevuelve409() {
        client.post().uri("/api/citas").bodyValue(cuerpo(1L, "PROF-003")).exchange().expectStatus().isCreated();
        client.post().uri("/api/citas").bodyValue(cuerpo(1L, "PROF-003")).exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void pacienteInexistenteDevuelve404() {
        client.post().uri("/api/citas").bodyValue(cuerpo(404L, "PROF-004")).exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void citaInexistenteDevuelve404() {
        client.get().uri("/api/citas/{id}", 9999).exchange().expectStatus().isNotFound();
    }

    private Cita crearCita(long pacienteId, String profesional, LocalDateTime fecha) {
        Map<String, Object> body = Map.of("pacienteId", pacienteId, "especialidad", "Medicina General",
                "fechaHora", fecha.toString(), "profesionalCodigo", profesional,
                "motivo", "Control administrativo");
        Cita c = client.post().uri("/api/citas").bodyValue(body).exchange()
                .expectStatus().isCreated().expectBody(Cita.class).returnResult().getResponseBody();
        assertNotNull(c);
        return c;
    }

    private LocalDateTime fechaBase() {
        return LocalDateTime.now().plusDays(3).withHour(10).withMinute(0).withSecond(0).withNano(0);
    }

    @Test
    void reprogramaCitaVigenteYConservaElEstado() {
        Cita creada = crearCita(1L, "PROF-010", fechaBase());
        LocalDateTime nueva = fechaBase().plusDays(2).withHour(15);

        client.patch().uri("/api/citas/{id}/reprogramar", creada.id()).bodyValue(Map.of("fechaHora", nueva.toString()))
                .exchange().expectStatus().isOk();

        Cita actual = client.get().uri("/api/citas/{id}", creada.id()).exchange()
                .expectStatus().isOk().expectBody(Cita.class).returnResult().getResponseBody();
        assertNotNull(actual);
        assertEquals(nueva, actual.fechaHora());
        assertEquals(EstadoCita.PENDIENTE, actual.estado());
    }

    @Test
    void reprogramarCitaCanceladaDevuelve422() {
        Cita creada = crearCita(1L, "PROF-011", fechaBase());
        client.patch().uri("/api/citas/{id}/estado", creada.id()).bodyValue(Map.of("estado", "CANCELADA"))
                .exchange().expectStatus().isOk();

        client.patch().uri("/api/citas/{id}/reprogramar", creada.id())
                .bodyValue(Map.of("fechaHora", fechaBase().plusDays(2).toString()))
                .exchange().expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void reprogramarAUnHorarioOcupadoDevuelve409() {
        crearCita(1L, "PROF-012", fechaBase());
        Cita otra = crearCita(1L, "PROF-012", fechaBase().plusDays(1));

        client.patch().uri("/api/citas/{id}/reprogramar", otra.id())
                .bodyValue(Map.of("fechaHora", fechaBase().toString()))
                .exchange().expectStatus().isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void reprogramarAUnaFechaPasadaDevuelve400() {
        Cita creada = crearCita(1L, "PROF-013", fechaBase());

        client.patch().uri("/api/citas/{id}/reprogramar", creada.id())
                .bodyValue(Map.of("fechaHora", LocalDateTime.now().minusDays(1).toString()))
                .exchange().expectStatus().isBadRequest();
    }

    @Test
    void reprogramarCitaInexistenteDevuelve404() {
        client.patch().uri("/api/citas/{id}/reprogramar", 9999)
                .bodyValue(Map.of("fechaHora", fechaBase().toString()))
                .exchange().expectStatus().isNotFound();
    }
}
