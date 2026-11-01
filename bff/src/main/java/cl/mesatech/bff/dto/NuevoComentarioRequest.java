package cl.mesatech.bff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NuevoComentarioRequest(
        @NotBlank @Size(max = 2000) String texto
) {
}
