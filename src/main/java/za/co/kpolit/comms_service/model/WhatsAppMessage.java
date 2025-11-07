package za.co.kpolit.comms_service.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "whatsapp_message")
public class WhatsAppMessage {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String fromNumber;

    @Column(nullable = false)
    private String toNumber;

    @Column(nullable = false)
    private String direction; // INCOMING or OUTGOING

    @Column(nullable = false)
    private String messageType; // text, image, template, etc.

    @Lob
    private String content;

    @Column(nullable = false)
    private Instant timestamp;

    private String phoneNumberId;
}