package ar.edu.utn.dds.k3003.mensajeria.events;

import java.time.LocalDateTime;
import java.util.List;

public record PdiProcesarEvento(
        String messageId,
        String hechoId,
        String descripcion,
        String lugar,
        LocalDateTime momento,
        String contenido,
        List<String> etiquetas,
        String resultadoOcr,
        String urlImagen
) { }
