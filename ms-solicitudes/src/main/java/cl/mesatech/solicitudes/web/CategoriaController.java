package cl.mesatech.solicitudes.web;

import cl.mesatech.solicitudes.model.CategoriaLocal;
import cl.mesatech.solicitudes.repository.CategoriaLocalRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

// Categorias locales (copia recibida por RabbitMQ). Solo las llama el BFF.
@RestController
@RequestMapping("/internal/categorias")
public class CategoriaController {

    private final CategoriaLocalRepository repository;

    public CategoriaController(CategoriaLocalRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Map<String, Object>> listar() {
        return repository.findAllByOrderByNombreAsc().stream()
                .map(c -> Map.<String, Object>of(
                        "categoriaId", c.getCategoriaId(),
                        "nombre", c.getNombre()))
                .toList();
    }
}
