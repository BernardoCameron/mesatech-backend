package cl.mesatech.bff.web;

import cl.mesatech.bff.client.CatalogoClient;
import cl.mesatech.bff.dto.NuevaCategoriaRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

// Rutas de categorias. El BFF solo coordina: llama a ms-catalogo.
@RestController
public class CategoriaBffController {

    private final CatalogoClient catalogoClient;

    public CategoriaBffController(CatalogoClient catalogoClient) {
        this.catalogoClient = catalogoClient;
    }

    // Solo el Administrador crea categorias (tabla 4.1 de la EP1: gestionar catalogo)
    @PostMapping("/v1/categorias")
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<Map> crear(@Valid @RequestBody NuevaCategoriaRequest body) {
        Map creada = catalogoClient.crearCategoria(body);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    // Cualquier usuario autenticado puede ver las categorias
    @GetMapping("/v1/categorias")
    @PreAuthorize("hasAnyRole('Cliente','Operador','Administrador')")
    public List<Map<String, Object>> listar() {
        return catalogoClient.listarCategorias();
    }
}
