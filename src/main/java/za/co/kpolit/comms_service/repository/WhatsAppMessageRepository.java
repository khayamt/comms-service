package za.co.kpolit.comms_service.repository;

import za.co.kpolit.comms_service.model.WhatsAppMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WhatsAppMessageRepository extends JpaRepository<WhatsAppMessage, UUID> {
}
