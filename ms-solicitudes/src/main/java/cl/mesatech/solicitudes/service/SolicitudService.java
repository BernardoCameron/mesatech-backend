package cl.mesatech.solicitudes.service;

import cl.mesatech.solicitudes.dto.CambiarEstadoRequest;
import cl.mesatech.solicitudes.dto.ComentarioRequest;
import cl.mesatech.solicitudes.dto.CrearSolicitudRequest;
import cl.mesatech.solicitudes.model.ComentarioSolicitud;
import cl.mesatech.solicitudes.model.EstadoSolicitud;
import cl.mesatech.solicitudes.model.HistorialEstado;
import cl.mesatech.solicitudes.model.Solicitud;
import cl.mesatech.solicitudes.model.TipoComentario;
import cl.mesatech.solicitudes.repository.CategoriaLocalRepository;
import cl.mesatech.solicitudes.repository.ComentarioRepository;
import cl.mesatech.solicitudes.repository.HistorialEstadoRepository;
import cl.mesatech.solicitudes.repository.SolicitudRepository;
import cl.mesatech.solicitudes.web.AccesoDenegadoException;
import cl.mesatech.solicitudes.web.DatoInvalidoException;
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
    private final ComentarioRepository comentarioRepository;
    private final CategoriaLocalRepository categoriaRepository;

    public SolicitudService(SolicitudRepository repository, HistorialEstadoRepository historialRepository,
                            ComentarioRepository comentarioRepository, CategoriaLocalRepository categoriaRepository) {
        this.repository = repository;
        this.historialRepository = historialRepository;
        this.comentarioRepository = comentarioRepository;
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional
    public Solicitud crear(CrearSolicitudRequest request) {
        // La categoria debe existir en la copia local que Solicitudes recibio desde Catalogo
        if (!categoriaRepository.existsByNombre(request.categoria())) {
            throw new DatoInvalidoException("La categoría " + request.categoria() + " aún no está disponible");
        }

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
    public Solicitud cambiarEstado(UUID id, CambiarEstadoRequest request) {
        EstadoSolicitud nuevoEstado = request.estado();
        Solicitud solicitud = repository.findById(id)
                .orElseThrow(() -> new EstadoInvalidoException("Solicitud no encontrada: " + id));

        Set<EstadoSolicitud> permitidos = TRANSICIONES_VALIDAS.getOrDefault(solicitud.getEstado(), Set.of());
        if (!permitidos.contains(nuevoEstado)) {
            throw new EstadoInvalidoException(
                    "No se puede pasar de %s a %s".formatted(solicitud.getEstado(), nuevoEstado));
        }

        // Para resolver hay que dejar escrito como se soluciono (trazabilidad)
        boolean hayComentario = request.comentario() != null && !request.comentario().isBlank();
        if (nuevoEstado == EstadoSolicitud.RESUELTA && !hayComentario) {
            throw new DatoInvalidoException("Debes indicar cómo se solucionó la solicitud.");
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

        if (hayComentario) {
            TipoComentario tipo = nuevoEstado == EstadoSolicitud.RESUELTA
                    ? TipoComentario.SOLUCION : TipoComentario.COMENTARIO;
            guardarComentario(id, request.comentario().trim(), tipo,
                    request.autorId(), request.autorNombre(), request.rol(), ahora);
        }
        return repository.save(solicitud);
    }

    // Detalle completo: la solicitud, sus cambios de estado y sus comentarios.
    // Un Cliente solo puede ver sus propias solicitudes.
    public Map<String, Object> detalle(UUID id, String usuarioId, boolean staff) {
        Solicitud solicitud = buscarConAcceso(id, usuarioId, staff);

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("solicitud", solicitud);
        resultado.put("historial", historial(id));
        resultado.put("comentarios", comentarioRepository.findBySolicitudIdOrderByFechaAsc(id));
        return resultado;
    }

    @Transactional
    public ComentarioSolicitud comentar(UUID id, ComentarioRequest request) {
        Solicitud solicitud = buscarConAcceso(id, request.autorId(), request.staff());

        if (solicitud.getEstado() == EstadoSolicitud.CERRADA || solicitud.getEstado() == EstadoSolicitud.CANCELADA) {
            throw new EstadoInvalidoException("La solicitud está " + solicitud.getEstado().name().toLowerCase()
                    + " y ya no admite comentarios");
        }

        ComentarioSolicitud comentario = guardarComentario(id, request.texto().trim(), TipoComentario.COMENTARIO,
                request.autorId(), request.autorNombre(), request.rol(), Instant.now());
        solicitud.setFechaActualizacion(comentario.getFecha());
        repository.save(solicitud);
        return comentario;
    }

    private Solicitud buscarConAcceso(UUID id, String usuarioId, boolean staff) {
        Solicitud solicitud = repository.findById(id)
                .orElseThrow(() -> new EstadoInvalidoException("Solicitud no encontrada: " + id));
        if (!staff && !solicitud.getUsuarioId().equals(usuarioId)) {
            throw new AccesoDenegadoException("No tienes acceso a esta solicitud");
        }
        return solicitud;
    }

    private ComentarioSolicitud guardarComentario(UUID solicitudId, String texto, TipoComentario tipo,
                                                  String autorId, String autorNombre, String rol, Instant fecha) {
        ComentarioSolicitud comentario = new ComentarioSolicitud();
        comentario.setSolicitudId(solicitudId);
        comentario.setTexto(texto);
        comentario.setTipo(tipo);
        comentario.setAutorId(autorId == null ? "sistema" : autorId);
        comentario.setAutorNombre(autorNombre == null ? "Sistema" : autorNombre);
        comentario.setRol(rol == null ? "Operador" : rol);
        comentario.setFecha(fecha);
        return comentarioRepository.save(comentario);
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
