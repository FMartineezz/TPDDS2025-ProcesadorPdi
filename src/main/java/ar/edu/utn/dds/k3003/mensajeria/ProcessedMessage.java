package ar.edu.utn.dds.k3003.mensajeria;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Setter @Getter
@Entity @Table(name = "processed_message")
public class ProcessedMessage {
    @Id
    @Column(name = "message_id", nullable = false, updatable = false, length = 100)
    private String messageId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt = Instant.now();

    public ProcessedMessage() {}
    public ProcessedMessage(String messageId) { this.messageId = messageId; }
}
