package cl.mesatech.solicitudes.dto;

import cl.mesatech.solicitudes.model.EstadoSolicitud;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// comentario: obligatorio al pasar a RESUELTA (como se soluciono); opcional en los demas cambios.
// autorId / autorNombre / rol: quien hace el cambio (los pone el BFF).
public record CambiarEstadoRequest(
        @NotNull EstadoSolicitud estado,
        @Size(max = 2000) String comentario,
        String autorId,
        String autorNombre,
        String rol
) {
}
