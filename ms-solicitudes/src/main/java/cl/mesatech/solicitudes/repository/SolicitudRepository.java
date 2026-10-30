package cl.mesatech.solicitudes.repository;

import cl.mesatech.solicitudes.model.Solicitud;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SolicitudRepository extends JpaRepository<Solicitud, UUID> {
    List<Solicitud> findByUsuarioIdOrderByFechaCreacionDesc(String usuarioId);

    List<Solicitud> findAllByOrderByFechaCreacionDesc();
}
