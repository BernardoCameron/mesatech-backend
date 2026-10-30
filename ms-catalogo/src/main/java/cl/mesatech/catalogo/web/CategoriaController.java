package cl.mesatech.catalogo.web;

import cl.mesatech.catalogo.dto.CrearCategoriaRequest;
import cl.mesatech.catalogo.model.ItemCatalogo;
import cl.mesatech.catalogo.model.TipoItem;
import cl.mesatech.catalogo.repository.ItemCatalogoRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

// Endpoints internos: solo los llama el BFF (el rol Administrador se valida alla).
@RestController
@RequestMapping("/internal/categorias")
public class CategoriaController {

    private final ItemCatalogoRepository repository;

    public CategoriaController(ItemCatalogoRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> crear(@Valid @RequestBody CrearCategoriaRequest request) {
        String nombre = request.nombre().trim();

        if (repository.existsByTipoAndNombre(TipoItem.CATEGORIA, nombre)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Ya existe la categoria " + nombre));
        }

        ItemCatalogo categoria = repository.save(new ItemCatalogo(TipoItem.CATEGORIA, nombre));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "categoriaId", categoria.getId(),
                "nombre", nombre));
    }

    @GetMapping
    public List<Map<String, Object>> listar() {
        return repository.findByTipo(TipoItem.CATEGORIA).stream()
                .map(c -> Map.<String, Object>of("categoriaId", c.getId(), "nombre", c.getNombre()))
                .toList();
    }
}
