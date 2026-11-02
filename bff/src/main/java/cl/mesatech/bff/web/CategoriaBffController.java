package cl.mesatech.bff.web;

import cl.mesatech.bff.client.CatalogoClient;
import cl.mesatech.bff.client.SolicitudesClient;
import cl.mesatech.bff.dto.NuevaCategoriaRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// Rutas de categorias. El BFF solo coordina: crea en Catalogo y consulta
// la copia local en Solicitudes; no publica ni consume mensajes.
@RestController
public class CategoriaBffController {

    private final CatalogoClient catalogoClient;
    private final SolicitudesClient solicitudesClient;

    public CategoriaBffController(CatalogoClient catalogoClient, SolicitudesClient solicitudesClient) {
        this.catalogoClient = catalogoClient;
        this.solicitudesClient = solicitudesClient;
    }

    // Solo el Administrador crea categorias (tabla 4.1 de la EP1: gestionar catalogo)
    @PostMapping("/v1/categorias")
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<Map> crear(@Valid @RequestBody NuevaCategoriaRequest body) {
        Map creada = catalogoClient.crearCategoria(body);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    // Vista del Administrador: categorias de Catalogo y si ya llegaron a Solicitudes
    @GetMapping("/v1/categorias")
    @PreAuthorize("hasRole('Administrador')")
    public List<Map<String, Object>> listarParaAdmin() {
        Set<Object> disponibles = new HashSet<>();
        try {
            for (Map<String, Object> local : solicitudesClient.listarCategorias()) {
                disponibles.add(local.get("categoriaId"));
            }
        } catch (RuntimeException e) {
            // Solicitudes caido o detenido: ninguna categoria se puede dar por disponible
        }

        return catalogoClient.listarCategorias().stream()
                .map(c -> Map.<String, Object>of(
                        "categoriaId", c.get("categoriaId"),
                        "nombre", c.get("nombre"),
                        "disponible", disponibles.contains(c.get("categoriaId"))))
                .toList();
    }

    // Reenvia el evento de una categoria que no llego a Solicitudes
    @PostMapping("/v1/categorias/{id}/republicar")
    @PreAuthorize("hasRole('Administrador')")
    public Map<String, Object> republicar(@PathVariable UUID id) {
        return catalogoClient.republicarCategoria(id);
    }

    // Categorias que se pueden usar al crear una solicitud (copia local)
    @GetMapping("/v1/categorias/disponibles")
    @PreAuthorize("hasAnyRole('Cliente','Operador','Administrador')")
    public List<Map<String, Object>> listarDisponibles() {
        return solicitudesClient.listarCategorias();
    }
}
