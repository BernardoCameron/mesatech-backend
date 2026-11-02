package cl.mesatech.solicitudes.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

// Una fila por cada vez que una solicitud se crea o cambia de estado.
// De aqui salen los tiempos (respuesta, resolucion) del dashboard.
@Entity
@Table(name = "historial_estados")
@Getter
@Setter
@NoArgsConstructor
public class HistorialEstado {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID solicitudId;

    // null cuando la solicitud recien se crea
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EstadoSolicitud estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSolicitud estadoNuevo;

    @Column(nullable = false)
    private Instant fecha;

    public HistorialEstado(UUID solicitudId, EstadoSolicitud estadoAnterior,
                           EstadoSolicitud estadoNuevo, Instant fecha) {
        this.solicitudId = solicitudId;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.fecha = fecha;
    }
}
