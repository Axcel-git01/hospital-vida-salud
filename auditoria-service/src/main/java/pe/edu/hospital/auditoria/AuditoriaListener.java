package pe.edu.hospital.auditoria;

import java.time.Duration;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import pe.edu.hospital.common.EventoNegocio;

@Component
public class AuditoriaListener {

    private final AuditoriaService service;

    public AuditoriaListener(AuditoriaService service) {
        this.service = service;
    }

    @RabbitListener(queues = MensajeriaAuditoria.COLA)
    public void recibir(EventoNegocio evento) {
        service.procesar(evento).block(Duration.ofSeconds(10));
    }
}
