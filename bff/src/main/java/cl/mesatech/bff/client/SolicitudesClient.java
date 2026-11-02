package cl.mesatech.bff.client;

import cl.mesatech.bff.dto.CambiarEstadoRequest;
import cl.mesatech.bff.dto.CrearSolicitudInterna;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

// Unico componente del BFF que conoce la URL de ms-solicitudes. El BFF
// nunca toca la base de datos: siempre pasa por este cliente HTTP.
@Component
public class SolicitudesClient {

    private final RestClient restClient;

    public SolicitudesClient(@Qualifier("solicitudesRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public Map<String, Object> crear(CrearSolicitudInterna body) {
        return restClient.post()
                .uri("/internal/solicitudes")
                .body(body)
                .retrieve()
                .body(Map.class);
    }

    public List<Map<String, Object>> listarPorUsuario(String usuarioId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/internal/solicitudes")
                        .queryParam("usuarioId", usuarioId).build())
                .retrieve()
                .body(List.class);
    }

    public List<Map<String, Object>> listarTodas() {
        return restClient.get()
                .uri("/internal/solicitudes")
                .retrieve()
                .body(List.class);
    }

    public List<Map<String, Object>> historial(UUID id) {
        return restClient.get()
                .uri("/internal/solicitudes/{id}/historial", id)
                .retrieve()
                .body(List.class);
    }

    public Map<String, Object> metricas() {
        return restClient.get()
                .uri("/internal/solicitudes/metricas")
                .retrieve()
                .body(Map.class);
    }

    public Map<String, Object> cambiarEstado(UUID id, String estado) {
        return restClient.patch()
                .uri("/internal/solicitudes/{id}/estado", id)
                .body(new CambiarEstadoRequest(estado))
                .retrieve()
                .body(Map.class);
    }
}
