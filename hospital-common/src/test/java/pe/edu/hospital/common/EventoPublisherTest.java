package pe.edu.hospital.common;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class EventoPublisherTest {

    @Mock
    RabbitTemplate rabbit;
    @InjectMocks
    EventoPublisher publisher;

    private final EventoNegocio evento =
            new EventoNegocio("CITA", 1L, 10L, "PENDIENTE", "CONFIRMADA", "citas-service", Instant.now());

    @Test
    void publicaEnElExchangeConLaRoutingKeyIndicada() {
        StepVerifier.create(publisher.publicar("cita.confirmada", evento)).verifyComplete();
        verify(rabbit).convertAndSend(Mensajeria.EXCHANGE, "cita.confirmada", evento);
    }

    @Test
    void siElBrokerFallaElFlujoNoSeRompe() {
        doThrow(new RuntimeException("broker caído"))
                .when(rabbit).convertAndSend(anyString(), anyString(), any(EventoNegocio.class));
        StepVerifier.create(publisher.publicar("cita.confirmada", evento)).verifyComplete();
    }
}
