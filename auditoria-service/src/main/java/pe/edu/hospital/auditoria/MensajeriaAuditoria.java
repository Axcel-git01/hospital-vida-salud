package pe.edu.hospital.auditoria;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Auditoría escucha todos los eventos del exchange (routing key "#"). */
@Configuration
public class MensajeriaAuditoria {

    public static final String COLA = "auditoria.eventos";

    @Bean
    public Queue colaAuditoria() {
        return QueueBuilder.durable(COLA).build();
    }

    @Bean
    public Binding bindingAuditoria(Queue colaAuditoria, TopicExchange hospitalExchange) {
        return BindingBuilder.bind(colaAuditoria).to(hospitalExchange).with("#");
    }
}
