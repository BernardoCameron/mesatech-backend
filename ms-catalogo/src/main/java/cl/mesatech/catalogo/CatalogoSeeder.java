package cl.mesatech.catalogo;

import cl.mesatech.catalogo.model.ItemCatalogo;
import cl.mesatech.catalogo.model.TipoItem;
import cl.mesatech.catalogo.repository.ItemCatalogoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

// Carga el catalogo minimo la primera vez que arranca el microservicio,
// para que el frontend tenga datos reales desde el dia uno.
@Component
public class CatalogoSeeder implements CommandLineRunner {

    private static final List<String> CATEGORIAS = List.of("Hardware", "Software", "Red", "Accesos");
    private static final List<String> PRIORIDADES = List.of("Baja", "Media", "Alta");

    private final ItemCatalogoRepository repository;

    public CatalogoSeeder(ItemCatalogoRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        seed(TipoItem.CATEGORIA, CATEGORIAS);
        seed(TipoItem.PRIORIDAD, PRIORIDADES);
    }

    private void seed(TipoItem tipo, List<String> nombres) {
        for (String nombre : nombres) {
            if (!repository.existsByTipoAndNombre(tipo, nombre)) {
                repository.save(new ItemCatalogo(tipo, nombre));
            }
        }
    }
}
