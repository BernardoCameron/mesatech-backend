package cl.mesatech.catalogo.web;

import cl.mesatech.catalogo.dto.CrearCategoriaRequest;
import cl.mesatech.catalogo.dto.EventoCategoria;
import cl.mesatech.catalogo.model.ItemCatalogo;
import cl.mesatech.catalogo.model.TipoItem;
import cl.mesatech.catalogo.publisher.CategoriaPublisher;
import cl.mesatech.catalogo.repository.ItemCatalogoRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

// Endpoints internos: solo los llama el BFF (el rol Administrador se valida alla).
@RestController
@RequestMapping("/internal/categorias")
public class CategoriaController {

    private static final Logger logger = LoggerFactory.getLogger(CategoriaController.class);

    private final ItemCatalogoRepository repository;
    private final CategoriaPublisher publisher;

    public CategoriaController(ItemCatalogoRepository repository, CategoriaPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    // Guarda la categoria en RDS y publica el evento. Son dos pasos separados:
    // si la publicacion falla, la categoria igual queda guardada.
    @PostMapping
    public ResponseEntity<Map<String, Object>> crear(@Valid @RequestBody CrearCategoriaRequest request) {
        String nombre = request.nombre().trim();

        if (repository.existsByTipoAndNombre(TipoItem.CATEGORIA, nombre)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Ya existe la categoria " + nombre));
        }

        ItemCatalogo categoria = repository.save(new ItemCatalogo(TipoItem.CATEGORIA, nombre));
        logger.info("Categoria guardada: categoriaId={} nombre={}", categoria.getId(), nombre);

        return publicar(categoria, HttpStatus.CREATED);
    }

    // Vuelve a publicar el evento de una categoria que ya existe (por ejemplo si la
    // publicacion fallo antes). Puede llegar repetido al consumidor: este guarda por
    // categoriaId, asi que actualiza la misma fila y no duplica.
    @PostMapping("/{id}/republicar")
    public ResponseEntity<Map<String, Object>> republicar(@PathVariable UUID id) {
        ItemCatalogo categoria = repository.findById(id).orElse(null);
        if (categoria == null || categoria.getTipo() != TipoItem.CATEGORIA) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Categoria no encontrada"));
        }
        return publicar(categoria, HttpStatus.OK);
    }

    // Publica el evento y espera la confirmacion de RabbitMQ.
    // Si no se confirma, la categoria igual quedo guardada: se responde 502 con un mensaje claro.
    private ResponseEntity<Map<String, Object>> publicar(ItemCatalogo categoria, HttpStatus estadoOk) {
        try {
            EventoCategoria evento = publisher.publicarCategoriaCreada(categoria.getId(), categoria.getNombre());
            return ResponseEntity.status(estadoOk).body(Map.of(
                    "categoriaId", categoria.getId(),
                    "nombre", categoria.getNombre(),
                    "eventId", evento.eventId()));
        } catch (AmqpException e) {
            logger.error("Fallo la publicacion de categoriaId={}: {}", categoria.getId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                    "error", "La categoria se guardo pero no se pudo publicar el evento",
                    "categoriaId", categoria.getId()));
        }
    }

    // Categorias originales del catalogo (para que el admin vea cuales ya llegaron a Solicitudes)
    @GetMapping
    public List<Map<String, Object>> listar() {
        return repository.findByTipo(TipoItem.CATEGORIA).stream()
                .map(c -> Map.<String, Object>of("categoriaId", c.getId(), "nombre", c.getNombre()))
                .toList();
    }
}
