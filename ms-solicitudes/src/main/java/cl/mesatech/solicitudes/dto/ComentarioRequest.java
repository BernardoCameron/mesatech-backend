package cl.mesatech.solicitudes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Lo envia el BFF con la identidad ya resuelta desde el JWT.
// staff = true si es Operador o Administrador.
public record ComentarioRequest(
        @NotBlank @Size(max = 2000) String texto,
        @NotBlank String autorId,
        @NotBlank String autorNombre,
        @NotBlank String rol,
        boolean staff
) {
}
