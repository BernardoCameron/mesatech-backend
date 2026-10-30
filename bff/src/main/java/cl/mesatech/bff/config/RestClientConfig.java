package cl.mesatech.bff.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    @Qualifier("solicitudesRestClient")
    public RestClient solicitudesRestClient(@Value("${mesatech.ms-solicitudes.base-url}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }

    @Bean
    @Qualifier("catalogoRestClient")
    public RestClient catalogoRestClient(@Value("${mesatech.ms-catalogo.base-url}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }
}
