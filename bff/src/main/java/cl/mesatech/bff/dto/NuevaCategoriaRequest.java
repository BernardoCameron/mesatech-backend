package cl.mesatech.bff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NuevaCategoriaRequest(
        @NotBlank @Size(max = 60) String nombre
) {
}
