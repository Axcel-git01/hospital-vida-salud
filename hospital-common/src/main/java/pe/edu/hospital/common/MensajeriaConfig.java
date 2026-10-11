package pe.edu.hospital.common;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Exchange topic y conversor JSON; solo se activa en servicios que incluyen Spring AMQP. */
@Configuration
@ConditionalOnClass(RabbitTemplate.class)
public class MensajeriaConfig {

    @Bean
    public TopicExchange hospitalExchange() {
        return new TopicExchange(Mensajeria.EXCHANGE, true, false);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultJackson2JavaTypeMapper mapper = new DefaultJackson2JavaTypeMapper();
        mapper.setTrustedPackages("pe.edu.hospital.common");
        converter.setJavaTypeMapper(mapper);
        return converter;
    }
}
