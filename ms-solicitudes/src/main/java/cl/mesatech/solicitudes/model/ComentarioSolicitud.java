package cl.mesatech.solicitudes.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "comentarios_solicitud")
@Getter
@Setter
@NoArgsConstructor
public class ComentarioSolicitud {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID solicitudId;

    @Column(nullable = false, length = 64)
    private String autorId;

    @Column(nullable = false, length = 160)
    private String autorNombre;

    @Column(nullable = false, length = 20)
    private String rol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoComentario tipo;

    @Column(nullable = false, length = 2000)
    private String texto;

    @Column(nullable = false)
    private Instant fecha;
}
