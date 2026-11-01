package cl.mesatech.bff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// comentario: obligatorio al resolver (como se soluciono); ms-solicitudes lo valida.
public record CambiarEstadoRequest(
        @NotBlank String estado,
        @Size(max = 2000) String comentario
) {
}
