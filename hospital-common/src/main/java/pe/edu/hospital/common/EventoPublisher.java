package pe.edu.hospital.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Publica eventos sin bloquear el hilo reactivo: el envío (API bloqueante de Spring AMQP)
 * se ejecuta en el scheduler boundedElastic. Si el broker falla, se registra el error y el
 * flujo de negocio continúa (limitación documentada; mejora: patrón Outbox).
 */
@Component
@ConditionalOnClass(RabbitTemplate.class)
public class EventoPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventoPublisher.class);
    private final RabbitTemplate rabbit;

    public EventoPublisher(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    public Mono<Void> publicar(String routingKey, EventoNegocio evento) {
        return Mono.<Void>fromRunnable(() -> rabbit.convertAndSend(Mensajeria.EXCHANGE, routingKey, evento))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(v -> log.info("Evento publicado: {} -> {}", routingKey, evento.recursoId()))
                .onErrorResume(e -> {
                    log.warn("No se pudo publicar el evento {}: {}", routingKey, e.getMessage());
                    return Mono.empty();
                });
    }
}
