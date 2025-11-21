package ar.edu.utn.dds.k3003.mensajeria;

import ar.edu.utn.dds.k3003.facades.FachadaProcesadorPdI;
import ar.edu.utn.dds.k3003.facades.dtos.PdIDTO;
import ar.edu.utn.dds.k3003.mensajeria.events.PdiProcesarEvento;
import ar.edu.utn.dds.k3003.repository.ProcessedMessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@Conditional(RabbitConfig.RabbitEnabled.class)
public class PdiWorker {

    private final Channel ch;
    private final ProcessedMessageRepository msgRepo;
    private final FachadaProcesadorPdI fachadaProcesadorPdI;   // ⬅️ usamos la fachada
    private final ObjectMapper mapper;

    private final Counter msgRecibidos, msgProcesados, msgFallidos;
    private final DistributionSummary lagMs;

    @Value("${MSG_DLX:eventos.dlx}") private String dlx;
    @Value("${MSG_Q_PREFIX:procesador.}") private String qPrefix;
    @Value("${MSG_Q_SUFFIX:.in}") private String qSuffix;
    @Value("${MSG_EXCHANGE:eventos}") private String exchange;
    @Value("${MSG_RK_PDI_PROCESAR:pdi.procesar}") private String rkProcesar;

    private final Map<String, String> consumerTags = new ConcurrentHashMap<>();

    public PdiWorker(Channel ch,
                     ProcessedMessageRepository msgRepo,
                     FachadaProcesadorPdI fachadaProcesadorPdI,   // ⬅️ inyectada por Spring (impl = Fachada)
                     MeterRegistry mr) {
        this.ch = ch;
        this.msgRepo = msgRepo;
        this.fachadaProcesadorPdI = fachadaProcesadorPdI;
        this.mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.msgRecibidos  = Counter.builder("procesador.msg.recibidos").register(mr);
        this.msgProcesados = Counter.builder("procesador.msg.procesados").register(mr);
        this.msgFallidos   = Counter.builder("procesador.msg.fallidos").register(mr);
        this.lagMs         = DistributionSummary.builder("procesador.msg.lag.ms").register(mr);
    }

    @PostConstruct
    public void start() throws Exception {

        String instance = System.getenv().getOrDefault("PROCESADOR_ID", "default");

        String queue = qPrefix + "pdi.procesar" + qSuffix;

        declareQueueAndBind(queue, rkProcesar);
        startConsumer(queue);
        log.info("[mq] worker '{}' iniciado queue='{}' rk='{}'", instance, queue, rkProcesar);
    }

    private void declareQueueAndBind(String queue, String topic) throws IOException {
        String rkDlq = "dlq." + topic;
        Map<String,Object> args = Map.of(
                "x-dead-letter-exchange", dlx,
                "x-dead-letter-routing-key", rkDlq,
                "x-message-ttl", 900_000
        );
        ch.queueDeclare(queue, true, false, false, args);
        ch.queueBind(queue, exchange, topic);
        ch.queueDeclare(queue + ".dlq", true, false, false, null);
        ch.queueBind(queue + ".dlq", dlx, rkDlq);
    }

    private void startConsumer(String queue) throws IOException {
        ch.basicQos(1);
        String tag = ch.basicConsume(queue, false, new DefaultConsumer(ch) {
            @Override
            public void handleDelivery(String consumerTag, Envelope env, AMQP.BasicProperties props, byte[] body) throws IOException {
                msgRecibidos.increment();
                String messageId = props.getMessageId() != null ? props.getMessageId() : UUID.randomUUID().toString();

                try {
                    if (props.getTimestamp() != null) {
                        long lag = Duration.between(props.getTimestamp().toInstant(), Instant.now()).toMillis();
                        lagMs.record(lag);
                    }

                    // idempotencia
                    if (msgRepo.existsById(messageId)) {
                        ch.basicAck(env.getDeliveryTag(), false);
                        return;
                    }

                    // Deserializo el evento
                    PdiProcesarEvento ev = mapper.readValue(body, PdiProcesarEvento.class);

                    // Lo traduzco a PdIDTO (igual que el controller síncrono)
                    PdIDTO dto = new PdIDTO(
                            null,                 // id lo genera la DB
                            ev.hechoId(),
                            ev.descripcion(),
                            ev.lugar(),
                            ev.momento(),
                            ev.contenido(),
                            ev.etiquetas(),
                            ev.resultadoOcr(),
                            ev.urlImagen()
                    );

                    // Reuso la lógica de la fachada: valida, procesa, persiste
                    fachadaProcesadorPdI.procesar(dto);

                    // Marco el mensaje como procesado
                    msgRepo.save(new ProcessedMessage(messageId));
                    msgProcesados.increment();
                    ch.basicAck(env.getDeliveryTag(), false);
                } catch (Exception ex) {
                    log.error("[worker] fallo procesando mensaje {}", messageId, ex);
                    msgFallidos.increment();
                    if (env.isRedeliver()) ch.basicReject(env.getDeliveryTag(), false);
                    else ch.basicNack(env.getDeliveryTag(), false, true);
                }
            }
        });
        consumerTags.put(queue, tag);
    }
}
