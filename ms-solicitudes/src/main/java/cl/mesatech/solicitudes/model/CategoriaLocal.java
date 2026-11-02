package cl.mesatech.solicitudes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

// Copia local de las categorias de Catalogo. El id es el categoriaId original
// (no se genera uno nuevo) para poder relacionarla con el registro de origen.
@Entity
@Table(name = "categorias_locales")
@Getter
@Setter
@NoArgsConstructor
public class CategoriaLocal {

    @Id
    private UUID categoriaId;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(nullable = false, length = 60)
    private String eventId;

    @Column(nullable = false)
    private Instant recibidoEn;
}
