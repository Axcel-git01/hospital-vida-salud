package pe.edu.hospital.common;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;

/** Protege el contrato JSON del evento entre productores y consumidores. */
class EventoNegocioTest {

    private final MessageConverter converter = new MensajeriaConfig().jsonMessageConverter();

    @Test
    void sobreviveElViajePorElConversorJsonConDetalle() {
        EventoNegocio evento = new EventoNegocio("CITA", 1L, 10L, "CONFIRMADA", "CONFIRMADA",
                "citas-service", Instant.parse("2030-01-01T10:00:00Z"), "REPROGRAMADA");

        Message mensaje = converter.toMessage(evento, new MessageProperties());

        assertEquals(evento, converter.fromMessage(mensaje));
    }

    @Test
    void unMensajeAnteriorSinDetalleSeLeeConDetalleNulo() {
        String json = "{\"recurso\":\"CITA\",\"recursoId\":1,\"pacienteId\":10,\"estadoAnterior\":null,"
                + "\"estadoNuevo\":\"CONFIRMADA\",\"origen\":\"citas-service\",\"fecha\":\"2030-01-01T10:00:00Z\"}";
        MessageProperties props = new MessageProperties();
        props.setContentType("application/json");
        props.setHeader("__TypeId__", EventoNegocio.class.getName());

        EventoNegocio leido = (EventoNegocio) converter.fromMessage(new Message(json.getBytes(UTF_8), props));

        assertEquals("CONFIRMADA", leido.estadoNuevo());
        assertNull(leido.detalle());
    }

    @Test
    void elConstructorSimpleDejaElDetalleVacio() {
        EventoNegocio evento = new EventoNegocio("PAGO", 1L, 10L, null, "REGISTRADO", "pagos-service", Instant.now());

        assertNull(evento.detalle());
    }
}
