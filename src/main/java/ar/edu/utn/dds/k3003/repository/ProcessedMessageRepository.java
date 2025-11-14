package ar.edu.utn.dds.k3003.repository;

import ar.edu.utn.dds.k3003.mensajeria.ProcessedMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessage, String> {}
