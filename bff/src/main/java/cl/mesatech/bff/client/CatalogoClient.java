package cl.mesatech.bff.client;

import cl.mesatech.bff.dto.NuevaCategoriaRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

// Unico componente del BFF que conoce la URL de ms-catalogo.
@Component
public class CatalogoClient {

    private final RestClient restClient;

    public CatalogoClient(@Qualifier("catalogoRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public Map<String, Object> crearCategoria(NuevaCategoriaRequest body) {
        return restClient.post()
                .uri("/internal/categorias")
                .body(body)
                .retrieve()
                .body(Map.class);
    }

    public Map<String, Object> republicarCategoria(UUID id) {
        return restClient.post()
                .uri("/internal/categorias/{id}/republicar", id)
                .retrieve()
                .body(Map.class);
    }

    public List<Map<String, Object>> listarCategorias() {
        return restClient.get()
                .uri("/internal/categorias")
                .retrieve()
                .body(List.class);
    }
}
