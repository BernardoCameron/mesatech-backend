package cl.mesatech.catalogo.dto;

import java.util.UUID;

// Contrato del evento. fechaEvento va en ISO-8601 UTC (ej: 2026-10-27T15:30:00Z).
public record EventoCategoria(
        String eventId,
        String tipoEvento,
        String fechaEvento,
        UUID categoriaId,
        String nombre
) {
}
