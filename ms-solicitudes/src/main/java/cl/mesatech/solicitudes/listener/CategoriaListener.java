package cl.mesatech.solicitudes.listener;

import cl.mesatech.solicitudes.dto.EventoCategoria;
import cl.mesatech.solicitudes.model.CategoriaLocal;
import cl.mesatech.solicitudes.repository.CategoriaLocalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class CategoriaListener {

    private static final Logger logger = LoggerFactory.getLogger(CategoriaListener.class);

    private final CategoriaLocalRepository repository;

    public CategoriaListener(CategoriaLocalRepository repository) {
        this.repository = repository;
    }

    // El ack (modo AUTO) se envia solo si este metodo termina sin error,
    // es decir, despues de guardar en RDS. Si falla, el mensaje se reencola.
    // Si llega repetido, el save con el mismo categoriaId actualiza la fila.
    @RabbitListener(queues = "${app.rabbitmq.queue}")
    public void recibirCategoria(EventoCategoria evento) {
        CategoriaLocal categoria = new CategoriaLocal();
        categoria.setCategoriaId(evento.categoriaId());
        categoria.setNombre(evento.nombre());
        categoria.setEventId(evento.eventId());
        categoria.setRecibidoEn(Instant.now());
        repository.save(categoria);

        logger.info("Categoria recibida y guardada: eventId={} categoriaId={} nombre={}",
                evento.eventId(), evento.categoriaId(), evento.nombre());
    }
}
