package pe.edu.hospital.notificaciones;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Cola propia de notificaciones, enlazada al exchange topic con routing keys cita.* y pago.*. */
@Configuration
public class MensajeriaNotificaciones {

    public static final String COLA = "notificaciones.eventos";

    @Bean
    public Queue colaNotificaciones() {
        return QueueBuilder.durable(COLA).build();
    }

    @Bean
    public Binding bindingCitas(Queue colaNotificaciones, TopicExchange hospitalExchange) {
        return BindingBuilder.bind(colaNotificaciones).to(hospitalExchange).with("cita.*");
    }

    @Bean
    public Binding bindingPagos(Queue colaNotificaciones, TopicExchange hospitalExchange) {
        return BindingBuilder.bind(colaNotificaciones).to(hospitalExchange).with("pago.*");
    }

    @Bean
    public Binding bindingLaboratorio(Queue colaNotificaciones, TopicExchange hospitalExchange) {
        return BindingBuilder.bind(colaNotificaciones).to(hospitalExchange).with("laboratorio.*");
    }
}
