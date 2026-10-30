package cl.mesatech.solicitudes.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "solicitudes")
@Getter
@Setter
@NoArgsConstructor
public class Solicitud {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(nullable = false, length = 2000)
    private String descripcion;

    @Column(nullable = false, length = 60)
    private String categoria;

    @Column(nullable = false, length = 20)
    private String prioridad;

    // Identificador del usuario solicitante: claim "oid" del token de Entra ID.
    @Column(nullable = false, length = 64)
    private String usuarioId;

    @Column(nullable = false, length = 160)
    private String usuarioNombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSolicitud estado = EstadoSolicitud.CREADA;

    @Column(nullable = false, updatable = false)
    private Instant fechaCreacion = Instant.now();
}
