package pe.edu.hospital.notificaciones;

import java.time.Duration;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import pe.edu.hospital.common.EventoNegocio;

/**
 * Consumidor del evento. Se ejecuta en el hilo del listener de RabbitMQ (no en el pipeline
 * WebFlux), por lo que esperar el resultado reactivo aquí permite confirmar (ack) el mensaje
 * solo si se guardó correctamente.
 */
@Component
public class NotificacionListener {

    private final NotificacionService service;

    public NotificacionListener(NotificacionService service) {
        this.service = service;
    }

    @RabbitListener(queues = MensajeriaNotificaciones.COLA)
    public void recibir(EventoNegocio evento) {
        service.procesar(evento).block(Duration.ofSeconds(10));
    }
}
