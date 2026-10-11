package pe.edu.hospital.pacientes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

/** Prueba de integración del endpoint: HTTP real + WebFlux + R2DBC sobre H2 en memoria. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class PacienteControllerTest {

    @Autowired
    WebTestClient client;
    @Autowired
    PacienteRepository repo;

    @BeforeEach
    void limpiar() {
        repo.deleteAll().block();
    }

    private Map<String, Object> cuerpo(String documento) {
        return Map.of("tipoDocumento", "DNI", "numeroDocumento", documento, "nombres", "Ana",
                "apellidos", "Prueba", "fechaNacimiento", "1990-01-01", "telefono", "999000111",
                "correo", "ana@prueba.test");
    }

    @Test
    void registraYConsultaPaciente() {
        Paciente creado = client.post().uri("/api/pacientes").bodyValue(cuerpo("10000001")).exchange()
                .expectStatus().isCreated()
                .expectBody(Paciente.class).returnResult().getResponseBody();
        assertNotNull(creado);
        assertEquals("ACTIVO", creado.estado());

        client.get().uri("/api/pacientes/{id}", creado.id()).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.numeroDocumento").isEqualTo("10000001");
    }

    @Test
    void rechazaPacienteDuplicadoConConflicto() {
        client.post().uri("/api/pacientes").bodyValue(cuerpo("10000002")).exchange().expectStatus().isCreated();
        client.post().uri("/api/pacientes").bodyValue(cuerpo("10000002")).exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void pacienteInexistenteDevuelve404() {
        client.get().uri("/api/pacientes/{id}", 9999).exchange().expectStatus().isNotFound();
    }

    @Test
    void cuerpoInvalidoDevuelve400() {
        client.post().uri("/api/pacientes").bodyValue(Map.of("tipoDocumento", "DNI")).exchange()
                .expectStatus().isBadRequest();
    }
}
