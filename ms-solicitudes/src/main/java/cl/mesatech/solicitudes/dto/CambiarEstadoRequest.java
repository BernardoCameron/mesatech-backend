package cl.mesatech.solicitudes.dto;

import cl.mesatech.solicitudes.model.EstadoSolicitud;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoRequest(
        @NotNull EstadoSolicitud estado
) {
}
