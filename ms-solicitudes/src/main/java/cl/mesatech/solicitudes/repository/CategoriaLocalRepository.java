package cl.mesatech.solicitudes.repository;

import cl.mesatech.solicitudes.model.CategoriaLocal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoriaLocalRepository extends JpaRepository<CategoriaLocal, UUID> {
    List<CategoriaLocal> findAllByOrderByNombreAsc();

    boolean existsByNombre(String nombre);
}
