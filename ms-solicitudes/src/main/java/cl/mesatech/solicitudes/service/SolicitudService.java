package cl.mesatech.solicitudes.service;

import cl.mesatech.solicitudes.dto.CrearSolicitudRequest;
import cl.mesatech.solicitudes.model.EstadoSolicitud;
import cl.mesatech.solicitudes.model.Solicitud;
import cl.mesatech.solicitudes.repository.SolicitudRepository;
import cl.mesatech.solicitudes.web.EstadoInvalidoException;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class SolicitudService {

    // Flujo minimo: CREADA -> ASIGNADA -> EN_PROCESO -> RESUELTA -> CERRADA,
    // con CANCELADA disponible desde cualquier estado no terminal.
    private static final Map<EstadoSolicitud, Set<EstadoSolicitud>> TRANSICIONES_VALIDAS = new EnumMap<>(EstadoSolicitud.class);

    static {
        TRANSICIONES_VALIDAS.put(EstadoSolicitud.CREADA, EnumSet.of(EstadoSolicitud.ASIGNADA, EstadoSolicitud.CANCELADA));
        TRANSICIONES_VALIDAS.put(EstadoSolicitud.ASIGNADA, EnumSet.of(EstadoSolicitud.EN_PROCESO, EstadoSolicitud.CANCELADA));
        TRANSICIONES_VALIDAS.put(EstadoSolicitud.EN_PROCESO, EnumSet.of(EstadoSolicitud.RESUELTA, EstadoSolicitud.CANCELADA));
        TRANSICIONES_VALIDAS.put(EstadoSolicitud.RESUELTA, EnumSet.of(EstadoSolicitud.CERRADA));
        TRANSICIONES_VALIDAS.put(EstadoSolicitud.CERRADA, EnumSet.noneOf(EstadoSolicitud.class));
        TRANSICIONES_VALIDAS.put(EstadoSolicitud.CANCELADA, EnumSet.noneOf(EstadoSolicitud.class));
    }

    private final SolicitudRepository repository;

    public SolicitudService(SolicitudRepository repository) {
        this.repository = repository;
    }

    public Solicitud crear(CrearSolicitudRequest request) {
        Solicitud solicitud = new Solicitud();
        solicitud.setTitulo(request.titulo());
        solicitud.setDescripcion(request.descripcion());
        solicitud.setCategoria(request.categoria());
        solicitud.setPrioridad(request.prioridad());
        solicitud.setUsuarioId(request.usuarioId());
        solicitud.setUsuarioNombre(request.usuarioNombre());
        solicitud.setEstado(EstadoSolicitud.CREADA);
        return repository.save(solicitud);
    }

    public List<Solicitud> listarPorUsuario(String usuarioId) {
        return repository.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId);
    }

    public List<Solicitud> listarTodas() {
        return repository.findAllByOrderByFechaCreacionDesc();
    }

    public Solicitud cambiarEstado(UUID id, EstadoSolicitud nuevoEstado) {
        Solicitud solicitud = repository.findById(id)
                .orElseThrow(() -> new EstadoInvalidoException("Solicitud no encontrada: " + id));

        Set<EstadoSolicitud> permitidos = TRANSICIONES_VALIDAS.getOrDefault(solicitud.getEstado(), Set.of());
        if (!permitidos.contains(nuevoEstado)) {
            throw new EstadoInvalidoException(
                    "No se puede pasar de %s a %s".formatted(solicitud.getEstado(), nuevoEstado));
        }

        solicitud.setEstado(nuevoEstado);
        return repository.save(solicitud);
    }
}
