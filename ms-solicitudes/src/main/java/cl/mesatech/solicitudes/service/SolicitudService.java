package cl.mesatech.solicitudes.service;

import cl.mesatech.solicitudes.dto.CrearSolicitudRequest;
import cl.mesatech.solicitudes.model.EstadoSolicitud;
import cl.mesatech.solicitudes.model.HistorialEstado;
import cl.mesatech.solicitudes.model.Solicitud;
import cl.mesatech.solicitudes.repository.HistorialEstadoRepository;
import cl.mesatech.solicitudes.repository.SolicitudRepository;
import cl.mesatech.solicitudes.web.EstadoInvalidoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
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
    private final HistorialEstadoRepository historialRepository;

    public SolicitudService(SolicitudRepository repository, HistorialEstadoRepository historialRepository) {
        this.repository = repository;
        this.historialRepository = historialRepository;
    }

    @Transactional
    public Solicitud crear(CrearSolicitudRequest request) {
        Solicitud solicitud = new Solicitud();
        solicitud.setTitulo(request.titulo());
        solicitud.setDescripcion(request.descripcion());
        solicitud.setCategoria(request.categoria());
        solicitud.setPrioridad(request.prioridad());
        solicitud.setUsuarioId(request.usuarioId());
        solicitud.setUsuarioNombre(request.usuarioNombre());
        solicitud.setEstado(EstadoSolicitud.CREADA);
        solicitud.setFechaActualizacion(solicitud.getFechaCreacion());
        Solicitud guardada = repository.save(solicitud);

        // Primera fila del historial: la solicitud se crea
        historialRepository.save(new HistorialEstado(
                guardada.getId(), null, EstadoSolicitud.CREADA, guardada.getFechaCreacion()));
        return guardada;
    }

    public List<Solicitud> listarPorUsuario(String usuarioId) {
        return repository.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId);
    }

    public List<Solicitud> listarTodas() {
        return repository.findAllByOrderByFechaCreacionDesc();
    }

    @Transactional
    public Solicitud cambiarEstado(UUID id, EstadoSolicitud nuevoEstado) {
        Solicitud solicitud = repository.findById(id)
                .orElseThrow(() -> new EstadoInvalidoException("Solicitud no encontrada: " + id));

        Set<EstadoSolicitud> permitidos = TRANSICIONES_VALIDAS.getOrDefault(solicitud.getEstado(), Set.of());
        if (!permitidos.contains(nuevoEstado)) {
            throw new EstadoInvalidoException(
                    "No se puede pasar de %s a %s".formatted(solicitud.getEstado(), nuevoEstado));
        }

        EstadoSolicitud estadoAnterior = solicitud.getEstado();
        Instant ahora = Instant.now();

        solicitud.setEstado(nuevoEstado);
        solicitud.setFechaActualizacion(ahora);

        // Fechas de cada hito, para calcular tiempos despues
        if (nuevoEstado == EstadoSolicitud.ASIGNADA && solicitud.getFechaAsignacion() == null) {
            solicitud.setFechaAsignacion(ahora);
        }
        if (nuevoEstado == EstadoSolicitud.RESUELTA) {
            solicitud.setFechaResolucion(ahora);
        }
        if (nuevoEstado == EstadoSolicitud.CERRADA) {
            solicitud.setFechaCierre(ahora);
        }

        historialRepository.save(new HistorialEstado(id, estadoAnterior, nuevoEstado, ahora));
        return repository.save(solicitud);
    }

    // Cambios de estado de una solicitud, del mas antiguo al mas nuevo
    public List<HistorialEstado> historial(UUID id) {
        Solicitud solicitud = repository.findById(id)
                .orElseThrow(() -> new EstadoInvalidoException("Solicitud no encontrada: " + id));

        List<HistorialEstado> filas = historialRepository.findBySolicitudIdOrderByFechaAsc(id);
        if (filas.isEmpty()) {
            // Solicitud creada antes de existir el historial: se registra su creacion
            historialRepository.save(new HistorialEstado(
                    id, null, EstadoSolicitud.CREADA, solicitud.getFechaCreacion()));
            filas = historialRepository.findBySolicitudIdOrderByFechaAsc(id);
        }
        return filas;
    }

    // Datos listos para el dashboard: cantidades y tiempos promedio (en segundos)
    public Map<String, Object> metricas() {
        List<Solicitud> todas = repository.findAll();

        Map<String, Long> porEstado = new LinkedHashMap<>();
        for (EstadoSolicitud estado : EstadoSolicitud.values()) {
            porEstado.put(estado.name(), 0L);
        }
        Map<String, Long> porCategoria = new LinkedHashMap<>();

        long sumaRespuesta = 0;
        long conRespuesta = 0;
        long sumaResolucion = 0;
        long resueltas = 0;

        for (Solicitud s : todas) {
            porEstado.merge(s.getEstado().name(), 1L, Long::sum);
            porCategoria.merge(s.getCategoria(), 1L, Long::sum);

            // Tiempo de respuesta: desde que se crea hasta que se asigna
            if (s.getFechaAsignacion() != null) {
                sumaRespuesta += Duration.between(s.getFechaCreacion(), s.getFechaAsignacion()).getSeconds();
                conRespuesta++;
            }
            // MTTR: desde que se crea hasta que se resuelve
            if (s.getFechaResolucion() != null) {
                sumaResolucion += Duration.between(s.getFechaCreacion(), s.getFechaResolucion()).getSeconds();
                resueltas++;
            }
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("total", todas.size());
        resultado.put("porEstado", porEstado);
        resultado.put("porCategoria", porCategoria);
        resultado.put("conRespuesta", conRespuesta);
        resultado.put("tiempoRespuestaPromedioSegundos", conRespuesta == 0 ? null : sumaRespuesta / conRespuesta);
        resultado.put("resueltas", resueltas);
        resultado.put("mttrSegundos", resueltas == 0 ? null : sumaResolucion / resueltas);
        return resultado;
    }
}
