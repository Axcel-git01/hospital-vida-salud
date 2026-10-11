package pe.edu.hospital.auditoria;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.hospital.common.EventoNegocio;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AuditoriaServiceTest {

    @Mock
    RegistroAuditoriaRepository repo;
    @InjectMocks
    AuditoriaService service;

    @Test
    void registraEstadoAnteriorNuevoYOrigen() {
        when(repo.save(any(RegistroAuditoria.class)))
                .thenAnswer(inv -> Mono.just((RegistroAuditoria) inv.getArgument(0)));
        EventoNegocio evento = new EventoNegocio("CITA", 5L, 10L, "PENDIENTE", "CONFIRMADA", "citas-service", Instant.now());

        StepVerifier.create(service.procesar(evento))
                .assertNext(r -> {
                    assertEquals("PENDIENTE", r.estadoAnterior());
                    assertEquals("CONFIRMADA", r.estadoNuevo());
                    assertEquals("citas-service", r.origen());
                })
                .verifyComplete();
    }

    @Test
    void historialConsultaPorRecursoEnMayusculas() {
        when(repo.findByRecursoAndRecursoIdOrderByFechaEventoAsc("CITA", 5L)).thenReturn(Flux.empty());

        StepVerifier.create(service.historial("cita", 5L)).verifyComplete();
        verify(repo).findByRecursoAndRecursoIdOrderByFechaEventoAsc("CITA", 5L);
    }

    @Test
    void registraElDetalleDeUnaReprogramacion() {
        when(repo.save(any(RegistroAuditoria.class)))
                .thenAnswer(inv -> Mono.just((RegistroAuditoria) inv.getArgument(0)));
        EventoNegocio evento = new EventoNegocio("CITA", 5L, 10L, "CONFIRMADA", "CONFIRMADA", "citas-service",
                Instant.now(), "REPROGRAMADA");

        StepVerifier.create(service.procesar(evento))
                .assertNext(r -> assertEquals("REPROGRAMADA", r.detalle()))
                .verifyComplete();
    }
}
