package cl.mesatech.catalogo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "catalogo_items")
@Getter
@Setter
@NoArgsConstructor
public class ItemCatalogo {

    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoItem tipo;

    @Column(nullable = false, length = 60)
    private String nombre;

    public ItemCatalogo(TipoItem tipo, String nombre) {
        this.tipo = tipo;
        this.nombre = nombre;
    }
}
