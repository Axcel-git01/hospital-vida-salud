package pe.edu.hospital.laboratorio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    @LoadBalanced
    WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    WebClient atencionesWebClient(WebClient.Builder builder,
                                  @Value("${atenciones.base-url:http://atenciones-service}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }
}
