package cl.mesatech.solicitudes.repository;

import cl.mesatech.solicitudes.model.ComentarioSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComentarioRepository extends JpaRepository<ComentarioSolicitud, UUID> {
    List<ComentarioSolicitud> findBySolicitudIdOrderByFechaAsc(UUID solicitudId);
}
