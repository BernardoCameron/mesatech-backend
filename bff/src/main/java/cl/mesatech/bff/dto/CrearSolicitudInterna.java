package cl.mesatech.bff.dto;

// Lo que el BFF reenvia a ms-solicitudes, ya con la identidad resuelta
// desde los claims del JWT (oid, name).
public record CrearSolicitudInterna(
        String titulo,
        String descripcion,
        String categoria,
        String prioridad,
        String usuarioId,
        String usuarioNombre
) {
}
