package cl.mesatech.bff.dto;

// Comentario con la identidad ya resuelta desde el JWT. staff = Operador o Administrador.
public record ComentarioInterno(
        String texto,
        String autorId,
        String autorNombre,
        String rol,
        boolean staff
) {
}
