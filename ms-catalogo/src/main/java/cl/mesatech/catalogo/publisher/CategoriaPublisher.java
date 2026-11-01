package cl.mesatech.catalogo.publisher;

import cl.mesatech.catalogo.dto.EventoCategoria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class CategoriaPublisher {

    private static final Logger logger = LoggerFactory.getLogger(CategoriaPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing-key}")
    private String routingKey;

    public CategoriaPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    // Publica el evento y espera la confirmacion del broker.
    // Si no se confirma lanza AmqpException (la categoria ya quedo guardada en RDS).
    public EventoCategoria publicarCategoriaCreada(UUID categoriaId, String nombre) {
        EventoCategoria evento = new EventoCategoria(
                UUID.randomUUID().toString(),
                "CATEGORIA_CREADA",
                Instant.now().toString(),
                categoriaId,
                nombre);

        CorrelationData correlationData = new CorrelationData(evento.eventId());

        rabbitTemplate.convertAndSend(exchange, routingKey, evento, message -> {
            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            message.getMessageProperties().setMessageId(evento.eventId());
            return message;
        }, correlationData);

        try {
            CorrelationData.Confirm confirm = correlationData.getFuture().get(10, TimeUnit.SECONDS);

            if (!confirm.ack()) {
                throw new AmqpException("RabbitMQ no confirmo la publicacion: " + confirm.reason());
            }
            if (correlationData.getReturned() != null) {
                throw new AmqpException("RabbitMQ no pudo dirigir el mensaje a una cola");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AmqpException("Se interrumpio la espera de confirmacion", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new AmqpException("No se pudo confirmar la publicacion en RabbitMQ", e);
        }

        logger.info("Evento publicado: eventId={} categoriaId={} nombre={}",
                evento.eventId(), categoriaId, nombre);
        return evento;
    }
}
