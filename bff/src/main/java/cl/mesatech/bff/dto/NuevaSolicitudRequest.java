package cl.mesatech.bff.dto;

import jakarta.validation.constraints.NotBlank;

// Lo que envia React: la identidad del usuario NO viaja aqui, se toma del JWT.
public record NuevaSolicitudRequest(
        @NotBlank String titulo,
        @NotBlank String descripcion,
        @NotBlank String categoria,
        @NotBlank String prioridad
) {
}
