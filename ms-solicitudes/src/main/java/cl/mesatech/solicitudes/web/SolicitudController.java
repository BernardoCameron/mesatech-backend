package cl.mesatech.solicitudes.web;

import cl.mesatech.solicitudes.dto.CambiarEstadoRequest;
import cl.mesatech.solicitudes.dto.ComentarioRequest;
import cl.mesatech.solicitudes.dto.CrearSolicitudRequest;
import cl.mesatech.solicitudes.model.ComentarioSolicitud;
import cl.mesatech.solicitudes.model.HistorialEstado;
import cl.mesatech.solicitudes.model.Solicitud;
import cl.mesatech.solicitudes.service.SolicitudService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

// Endpoints internos: solo los llama el BFF (nunca el frontend directo).
// La identidad del usuario ya fue validada rio arriba por el BFF via JWT;
// aqui solo se recibe el resultado (usuarioId / usuarioNombre / filtro).
@RestController
@RequestMapping("/internal/solicitudes")
public class SolicitudController {

    private final SolicitudService service;

    public SolicitudController(SolicitudService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Solicitud> crear(@Valid @RequestBody CrearSolicitudRequest request) {
        Solicitud creada = service.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @GetMapping
    public List<Solicitud> listar(@RequestParam(required = false) String usuarioId) {
        if (usuarioId != null && !usuarioId.isBlank()) {
            return service.listarPorUsuario(usuarioId);
        }
        return service.listarTodas();
    }

    // Cambios de estado de una solicitud con su fecha
    @GetMapping("/{id}/historial")
    public List<HistorialEstado> historial(@PathVariable UUID id) {
        return service.historial(id);
    }

    // Cantidades y tiempos promedio para el dashboard
    @GetMapping("/metricas")
    public Map<String, Object> metricas() {
        return service.metricas();
    }

    // Solicitud + historial + comentarios. staff=false: solo ve las suyas.
    @GetMapping("/{id}/detalle")
    public Map<String, Object> detalle(@PathVariable UUID id, @RequestParam String usuarioId,
                                       @RequestParam boolean staff) {
        return service.detalle(id, usuarioId, staff);
    }

    @PostMapping("/{id}/comentarios")
    public ResponseEntity<ComentarioSolicitud> comentar(@PathVariable UUID id,
                                                        @Valid @RequestBody ComentarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.comentar(id, request));
    }

    @PatchMapping("/{id}/estado")
    public Solicitud cambiarEstado(@PathVariable UUID id, @Valid @RequestBody CambiarEstadoRequest request) {
        return service.cambiarEstado(id, request);
    }
}
