# Evento de categoria (RabbitMQ)

ms-catalogo publica un evento cada vez que se crea una categoria. ms-solicitudes lo consume y guarda una
copia local para validar las solicitudes sin llamar a ms-catalogo.

| Dato | Valor |
|---|---|
| Virtual host | `mesatech` |
| Exchange | `catalogo.exchange` (direct, durable) |
| Routing key | `categoria.creada` |
| Cola | `solicitudes.catalogo.queue` (durable, sin auto-delete) |
| Publicador | usuario `catalogo_publicador` (solo escribe en el exchange) |
| Consumidor | usuario `solicitudes_consumidor` (solo lee la cola) |

## Mensaje (JSON)

```json
{
  "eventId": "3f0c2c6e-5d57-4c0e-9f43-3a6c1b7a9a10",
  "tipoEvento": "CATEGORIA_CREADA",
  "fechaEvento": "2026-11-02T15:04:05Z",
  "categoriaId": "b6a1c3e2-8d0f-4a52-9c1e-0f6f2d7e5b44",
  "nombre": "Impresoras"
}
```

| Campo | Tipo | Detalle |
|---|---|---|
| `eventId` | UUID | identifica el evento |
| `tipoEvento` | texto | siempre `CATEGORIA_CREADA` |
| `fechaEvento` | texto | fecha y hora en ISO-8601, UTC |
| `categoriaId` | UUID | id de la categoria en ms-catalogo |
| `nombre` | texto | nombre de la categoria |

## Comportamiento

- El mensaje es persistente y el publicador espera la confirmacion del broker (10 s). Si no llega, el
  POST de ms-catalogo responde 502 y la categoria queda pendiente; se reenvia con `POST /v1/categorias/{id}/republicar`.
- El consumidor confirma (ack) solo despues de guardar en su RDS. Si falla, el mensaje vuelve a la cola.
- Si ms-solicitudes esta apagado, los mensajes quedan en la cola y se consumen al volver.
