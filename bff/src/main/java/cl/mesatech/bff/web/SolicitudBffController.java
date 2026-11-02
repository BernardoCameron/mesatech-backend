package cl.mesatech.bff.web;

import cl.mesatech.bff.client.SolicitudesClient;
import cl.mesatech.bff.dto.CambiarEstadoInterno;
import cl.mesatech.bff.dto.CambiarEstadoRequest;
import cl.mesatech.bff.dto.ComentarioInterno;
import cl.mesatech.bff.dto.CrearSolicitudInterna;
import cl.mesatech.bff.dto.NuevaSolicitudRequest;
import cl.mesatech.bff.dto.NuevoComentarioRequest;
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

    // Cambios de estado de una solicitud con su fecha (linea de tiempo)
    @GetMapping("/v1/solicitudes/{id}/historial")
    @PreAuthorize("hasAnyRole('Operador','Administrador')")
    public List<Map<String, Object>> historial(@PathVariable UUID id) {
        return client.historial(id);
    }

    // Cantidades y tiempos promedio (respuesta y MTTR) para el dashboard
    @GetMapping("/v1/metricas")
    @PreAuthorize("hasAnyRole('Operador','Administrador')")
    public Map<String, Object> metricas() {
        return client.metricas();
    }

    // Solo Operador y Administrador cambian estado.
    @PatchMapping("/v1/solicitudes/{id}/estado")
    @PreAuthorize("hasAnyRole('Operador','Administrador')")
    public Map cambiarEstado(@PathVariable UUID id, @Valid @RequestBody CambiarEstadoRequest body,
                             JwtAuthenticationToken auth) {
        Jwt jwt = auth.getToken();
        CambiarEstadoInterno interno = new CambiarEstadoInterno(
                body.estado(), body.comentario(),
                jwt.getClaimAsString("oid"), jwt.getClaimAsString("name"), rolDe(auth));
        return client.cambiarEstado(id, interno);
    }

    // Detalle: solicitud + historial + comentarios. Un Cliente solo ve las suyas
    // (lo valida ms-solicitudes con el usuarioId del token).
    @GetMapping("/v1/solicitudes/{id}")
    @PreAuthorize("hasAnyRole('Cliente','Operador','Administrador')")
    public Map<String, Object> detalle(@PathVariable UUID id, JwtAuthenticationToken auth) {
        return client.detalle(id, auth.getToken().getClaimAsString("oid"), esStaff(auth));
    }

    @PostMapping("/v1/solicitudes/{id}/comentarios")
    @PreAuthorize("hasAnyRole('Cliente','Operador','Administrador')")
    public ResponseEntity<Map> comentar(@PathVariable UUID id, @Valid @RequestBody NuevoComentarioRequest body,
                                        JwtAuthenticationToken auth) {
        Jwt jwt = auth.getToken();
        ComentarioInterno interno = new ComentarioInterno(
                body.texto(), jwt.getClaimAsString("oid"), jwt.getClaimAsString("name"),
                rolDe(auth), esStaff(auth));
        return ResponseEntity.status(HttpStatus.CREATED).body(client.comentar(id, interno));
    }

    private boolean esStaff(JwtAuthenticationToken auth) {
        return !rolDe(auth).equals("Cliente");
    }

    // El rol mas alto que trae el token
    private String rolDe(JwtAuthenticationToken auth) {
        boolean admin = false;
        boolean operador = false;
        for (var autoridad : auth.getAuthorities()) {
            if (autoridad.getAuthority().equals("ROLE_Administrador")) admin = true;
            if (autoridad.getAuthority().equals("ROLE_Operador")) operador = true;
        }
        if (admin) return "Administrador";
        if (operador) return "Operador";
        return "Cliente";
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
