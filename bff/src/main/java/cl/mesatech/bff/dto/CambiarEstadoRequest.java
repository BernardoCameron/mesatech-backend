package cl.mesatech.bff.dto;

import jakarta.validation.constraints.NotBlank;

public record CambiarEstadoRequest(
        @NotBlank String estado
) {
}
