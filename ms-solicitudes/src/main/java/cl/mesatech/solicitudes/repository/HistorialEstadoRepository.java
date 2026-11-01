package cl.mesatech.solicitudes.repository;

import cl.mesatech.solicitudes.model.HistorialEstado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HistorialEstadoRepository extends JpaRepository<HistorialEstado, UUID> {
    List<HistorialEstado> findBySolicitudIdOrderByFechaAsc(UUID solicitudId);
}
