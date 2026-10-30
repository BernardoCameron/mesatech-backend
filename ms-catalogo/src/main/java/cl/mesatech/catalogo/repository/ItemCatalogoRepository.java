package cl.mesatech.catalogo.repository;

import cl.mesatech.catalogo.model.ItemCatalogo;
import cl.mesatech.catalogo.model.TipoItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemCatalogoRepository extends JpaRepository<ItemCatalogo, java.util.UUID> {
    List<ItemCatalogo> findByTipo(TipoItem tipo);

    boolean existsByTipoAndNombre(TipoItem tipo, String nombre);
}
