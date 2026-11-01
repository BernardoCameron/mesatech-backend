package cl.mesatech.solicitudes.dto;

import java.util.UUID;

// Mismo contrato JSON que publica ms-catalogo.
public record EventoCategoria(
        String eventId,
        String tipoEvento,
        String fechaEvento,
        UUID categoriaId,
        String nombre
) {
}
