package cl.mesatech.catalogo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearCategoriaRequest(
        @NotBlank @Size(max = 60) String nombre
) {
}
