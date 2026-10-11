package pe.edu.hospital.notificaciones;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.hospital.common.EventoNegocio;
import reactor.core.publisher.Mono;

/** Verifica el consumo del evento CitaConfirmada por parte del listener. */
@ExtendWith(MockitoExtension.class)
class NotificacionListenerTest {

    @Mock
    NotificacionService service;
    @InjectMocks
    NotificacionListener listener;

    @Test
    void alRecibirCitaConfirmadaSeProcesaLaNotificacion() {
        EventoNegocio evento = new EventoNegocio("CITA", 5L, 10L, "PENDIENTE", "CONFIRMADA", "citas-service", Instant.now());
        when(service.procesar(evento)).thenReturn(Mono.just(
                new Notificacion(1L, "CITA", 5L, 10L, "Su cita fue confirmada", null, "LOG")));

        listener.recibir(evento);

        verify(service).procesar(evento);
    }
}
