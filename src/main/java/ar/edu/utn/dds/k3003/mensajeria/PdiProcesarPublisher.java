
package ar.edu.utn.dds.k3003.mensajeria;

import ar.edu.utn.dds.k3003.mensajeria.events.PdiProcesarEvento;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@Conditional(RabbitConfig.RabbitEnabled.class)
public class PdiProcesarPublisher {

    private final Channel ch;
    private final ObjectMapper mapper;
    private final Counter publicadosOk, publicadosError;

    @Value("${MSG_EXCHANGE:eventos}")      private String exchange;
    @Value("${MSG_RK_PDI_PROCESAR:pdi.procesar}") private String rk;

    public PdiProcesarPublisher(Channel ch, MeterRegistry mr) {
        this.ch = ch;
        this.mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.publicadosOk    = Counter.builder("procesador.msg.publicados.ok").register(mr);
        this.publicadosError = Counter.builder("procesador.msg.publicados.error").register(mr);
    }

    public void publicar(PdiProcesarEvento evento) {
        try {
            String messageId = evento.messageId() != null ? evento.messageId() : UUID.randomUUID().toString();
            byte[] body = mapper.writeValueAsBytes(evento);
            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                    .appId("procesador")
                    .messageId(messageId)
                    .timestamp(new Date())
                    .contentType("application/json")
                    .deliveryMode(2)
                    .headers(Map.of("origin","procesador","eventType","pdi.procesar"))
                    .build();
            ch.basicPublish(exchange, rk, props, body);
            publicadosOk.increment();
        } catch (Exception e) {
            publicadosError.increment();
        }
    }
}
