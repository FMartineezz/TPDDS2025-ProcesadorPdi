package ar.edu.utn.dds.k3003.controller;

import ar.edu.utn.dds.k3003.app.FakeFachadaSolicitudes;
import ar.edu.utn.dds.k3003.clients.SearchProxy;
import ar.edu.utn.dds.k3003.facades.FachadaProcesadorPdI;
import ar.edu.utn.dds.k3003.facades.dtos.PdIDTO;
import ar.edu.utn.dds.k3003.mensajeria.PdiProcesarPublisher;
import ar.edu.utn.dds.k3003.mensajeria.events.PdiProcesarEvento;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/pdis")
public class PdiController {

    private final FachadaProcesadorPdI fachadaProcesadorPdI;
    private final SearchProxy searchProxy;

    // 🔹 Puede o no existir, según RabbitEnabled
    @Autowired(required = false)
    private PdiProcesarPublisher publisher;

    @Autowired
    public PdiController(FachadaProcesadorPdI fachadaProcesadorPdI,
                         SearchProxy searchProxy) {
        this.fachadaProcesadorPdI = fachadaProcesadorPdI;
        this.searchProxy = searchProxy;
    }

    @GetMapping
    public ResponseEntity<List<PdIDTO>> listarPdis(@RequestParam(required = true) String hecho) {
        return ResponseEntity.ok(fachadaProcesadorPdI.buscarPorHecho(hecho));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PdIDTO> obtenerPdiPorId(@PathVariable String id){
        return ResponseEntity.ok(fachadaProcesadorPdI.buscarPdIPorId(id));
    }

    @PostMapping
    public ResponseEntity<?> crearPdi(@RequestBody PdIDTO pdi,
                                      @RequestParam(name = "async", defaultValue = "false") boolean async) {
        try {
            if (!async) {
                // ----- MODO SÍNCRONO -----
                PdIDTO creado = fachadaProcesadorPdI.procesar(pdi);

                return ResponseEntity
                        .created(URI.create("/pdis/" + creado.id()))
                        .body(creado);
            } else {
                // ----- MODO ASÍNCRONO -----
                if (publisher == null) {
                    // No hay Rabbit / publisher disponible
                    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                            .body(Map.of(
                                    "error", "Procesamiento async no disponible en este entorno",
                                    "detail", "No hay bean PdiProcesarPublisher (Rabbit deshabilitado)"
                            ));
                }

                String messageId = UUID.randomUUID().toString();

                PdiProcesarEvento evento = new PdiProcesarEvento(
                        messageId,
                        pdi.hechoId(),
                        pdi.descripcion(),
                        pdi.lugar(),
                        pdi.momento(),          // LocalDateTime, puede ser null
                        pdi.contenido(),
                        pdi.etiquetas(),
                        pdi.resultadoOcr(),
                        pdi.urlImagen()
                );

                publisher.publicar(evento);

                return ResponseEntity.accepted().body(Map.of(
                        "status", "enqueued",
                        "messageId", messageId,
                        "hechoId", pdi.hechoId()
                ));
            }
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "No se pudo crear/procesar el PDI",
                    "detail", ex.getMessage()
            ));
        }
    }

    @DeleteMapping
    public ResponseEntity<Void> eliminarTodos() {
        fachadaProcesadorPdI.eliminarTodos();
        return ResponseEntity.noContent().build(); // 204 No Content
    }
}
