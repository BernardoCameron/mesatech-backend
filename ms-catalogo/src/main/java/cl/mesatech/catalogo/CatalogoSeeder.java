package cl.mesatech.catalogo;

import cl.mesatech.catalogo.model.ItemCatalogo;
import cl.mesatech.catalogo.model.TipoItem;
import cl.mesatech.catalogo.repository.ItemCatalogoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

// Carga las prioridades la primera vez que arranca el microservicio. Las
// categorias ya no se siembran: las crea el Administrador y se propagan a
// Solicitudes por RabbitMQ (una categoria sembrada aqui nunca llegaria alla).
@Component
public class CatalogoSeeder implements CommandLineRunner {

    private static final List<String> PRIORIDADES = List.of("Baja", "Media", "Alta");

    private final ItemCatalogoRepository repository;

    public CatalogoSeeder(ItemCatalogoRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
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
