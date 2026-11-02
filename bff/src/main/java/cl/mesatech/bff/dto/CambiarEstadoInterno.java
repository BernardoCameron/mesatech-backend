package cl.mesatech.bff.dto;

// Lo que el BFF reenvia a ms-solicitudes, con quien hace el cambio (claims del JWT).
public record CambiarEstadoInterno(
        String estado,
        String comentario,
        String autorId,
        String autorNombre,
        String rol
) {
}
