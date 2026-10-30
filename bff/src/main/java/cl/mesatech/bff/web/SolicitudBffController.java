package cl.mesatech.bff.web;

import cl.mesatech.bff.client.SolicitudesClient;
import cl.mesatech.bff.dto.CambiarEstadoRequest;
import cl.mesatech.bff.dto.CrearSolicitudInterna;
import cl.mesatech.bff.dto.NuevaSolicitudRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Punto de entrada de negocio del BFF. Cada metodo:
 * 1) ya paso la revalidacion de JWT (issuer/audience/firma/expiracion) hecha
 *    por Spring Security antes de llegar aqui,
 * 2) aplica autorizacion por rol con @PreAuthorize sobre el claim "roles",
 * 3) llama al microservicio de dominio (nunca a la base de datos).
 */
@RestController
public class SolicitudBffController {

    private final SolicitudesClient client;

    public SolicitudBffController(SolicitudesClient client) {
        this.client = client;
    }

    // Los 3 roles pueden crear solicitudes (tabla 4.1 de la EP1: Cliente
    // obligatorio, Operador/Administrador opcional).
    @PostMapping("/v1/solicitudes")
    @PreAuthorize("hasAnyRole('Cliente','Operador','Administrador')")
    public ResponseEntity<Map> crear(@Valid @RequestBody NuevaSolicitudRequest body, JwtAuthenticationToken auth) {
        Jwt jwt = auth.getToken();
        CrearSolicitudInterna interna = new CrearSolicitudInterna(
                body.titulo(), body.descripcion(), body.categoria(), body.prioridad(),
                jwt.getClaimAsString("oid"), jwt.getClaimAsString("name"));
        Map creada = client.crear(interna);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    // Cualquier usuario autenticado ve solo sus propias solicitudes.
    @GetMapping("/v1/solicitudes/mias")
    @PreAuthorize("hasAnyRole('Cliente','Operador','Administrador')")
    public List<Map<String, Object>> misSolicitudes(JwtAuthenticationToken auth) {
        return client.listarPorUsuario(auth.getToken().getClaimAsString("oid"));
    }

    // v2 de "mis solicitudes": agrega filtro opcional por estado y el campo
    // calculado diasAbiertas. Se mantiene v1 disponible (sin filtro ni campo
    // nuevo) para no romper a un cliente que ya integro contra v1.
    @GetMapping("/v2/solicitudes/mias")
    @PreAuthorize("hasAnyRole('Cliente','Operador','Administrador')")
    public List<Map<String, Object>> misSolicitudesV2(
            @RequestParam(required = false) String estado, JwtAuthenticationToken auth) {
        List<Map<String, Object>> solicitudes = client.listarPorUsuario(auth.getToken().getClaimAsString("oid"));
        return solicitudes.stream()
                .filter(s -> estado == null || estado.equalsIgnoreCase(String.valueOf(s.get("estado"))))
                .map(this::conDiasAbiertas)
                .toList();
    }

    // Solo Operador y Administrador ven todas las solicitudes.
    @GetMapping("/v1/solicitudes")
    @PreAuthorize("hasAnyRole('Operador','Administrador')")
    public List<Map<String, Object>> todas() {
        return client.listarTodas();
    }

    // Solo Operador y Administrador cambian estado.
    @PatchMapping("/v1/solicitudes/{id}/estado")
    @PreAuthorize("hasAnyRole('Operador','Administrador')")
    public Map cambiarEstado(@PathVariable UUID id, @Valid @RequestBody CambiarEstadoRequest body) {
        return client.cambiarEstado(id, body.estado());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> conDiasAbiertas(Map<String, Object> solicitud) {
        Map<String, Object> copia = new java.util.HashMap<>(solicitud);
        Object fechaCreacion = solicitud.get("fechaCreacion");
        if (fechaCreacion != null) {
            Instant creada = Instant.parse(fechaCreacion.toString());
            copia.put("diasAbiertas", java.time.Duration.between(creada, Instant.now()).toDays());
        }
        return copia;
    }
}
