package pe.edu.hospital.citas;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    /** El host "pacientes-service" se resuelve con Eureka + balanceo de carga del lado del cliente. */
    @Bean
    @LoadBalanced
    WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    WebClient pacientesWebClient(WebClient.Builder builder,
                                 @Value("${pacientes.base-url:http://pacientes-service}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }
}
