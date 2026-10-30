package cl.mesatech.catalogo.web;

import cl.mesatech.catalogo.model.ItemCatalogo;
import cl.mesatech.catalogo.model.TipoItem;
import cl.mesatech.catalogo.repository.ItemCatalogoRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

// Endpoint interno: solo lo llama el BFF. La gestion (crear/editar/borrar)
// para el rol Administrador se agrega en la siguiente iteracion.
@RestController
@RequestMapping("/internal/catalogo")
public class CatalogoController {

    private final ItemCatalogoRepository repository;

    public CatalogoController(ItemCatalogoRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public Map<String, List<String>> listar() {
        List<String> categorias = repository.findByTipo(TipoItem.CATEGORIA).stream()
                .map(ItemCatalogo::getNombre).toList();
        List<String> prioridades = repository.findByTipo(TipoItem.PRIORIDAD).stream()
                .map(ItemCatalogo::getNombre).toList();
        return Map.of("categorias", categorias, "prioridades", prioridades);
    }
}
