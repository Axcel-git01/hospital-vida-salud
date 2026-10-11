package pe.edu.hospital.notificaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.hospital.common.EventoNegocio;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    @Mock
    NotificacionRepository repo;
    @InjectMocks
    NotificacionService service;

    private EventoNegocio evento(String recurso, String nuevo) {
        return new EventoNegocio(recurso, 5L, 10L, null, nuevo, "test", Instant.now());
    }

    @Test
    void citaConfirmadaGeneraNotificacion() {
        when(repo.save(any(Notificacion.class))).thenAnswer(inv -> Mono.just((Notificacion) inv.getArgument(0)));

        StepVerifier.create(service.procesar(evento("CITA", "CONFIRMADA")))
                .assertNext(n -> {
                    assertTrue(n.mensaje().contains("confirmada"));
                    assertEquals(10L, n.pacienteId());
                    assertEquals("LOG", n.canal());
                })
                .verifyComplete();
    }

    @Test
    void pagoRegistradoGeneraNotificacion() {
        when(repo.save(any(Notificacion.class))).thenAnswer(inv -> Mono.just((Notificacion) inv.getArgument(0)));

        StepVerifier.create(service.procesar(evento("PAGO", "REGISTRADO")))
                .assertNext(n -> assertTrue(n.mensaje().contains("pago")))
                .verifyComplete();
    }

    @Test
    void citaReprogramadaGeneraNotificacionPropia() {
        when(repo.save(any(Notificacion.class))).thenAnswer(inv -> Mono.just((Notificacion) inv.getArgument(0)));
        EventoNegocio evento = new EventoNegocio("CITA", 5L, 10L, "CONFIRMADA", "CONFIRMADA", "citas-service",
                Instant.now(), "REPROGRAMADA");

        StepVerifier.create(service.procesar(evento))
                .assertNext(n -> assertTrue(n.mensaje().contains("reprogramada")))
                .verifyComplete();
    }
}
