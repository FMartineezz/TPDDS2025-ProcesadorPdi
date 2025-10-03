package ar.edu.utn.dds.k3003.model.analyzers;

import ar.edu.utn.dds.k3003.clients.ApiEtiquetadorService;
import ar.edu.utn.dds.k3003.model.PdI;
import ar.edu.utn.dds.k3003.model.PdiAnalyzer;
import ar.edu.utn.dds.k3003.model.puertos.EtiquetadorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Component
@Order(20) // después del OCR
public class LabelingAnalyzer implements PdiAnalyzer {
    private static final Logger log = LoggerFactory.getLogger(LabelingAnalyzer.class);

    private final EtiquetadorService labeling;

    public LabelingAnalyzer(EtiquetadorService labeling) {
        this.labeling = labeling;
    }

    @Override
    public void apply(PdI pdi) {
        String url = pdi.getUrlImagen();
        if (url == null || url.isBlank()) return; // sin imagen, no hay etiquetado

        try {
            List<String> labels = labeling.labelsFromUrl(url);
            if (labels != null && !labels.isEmpty()) {
                if (pdi.getEtiquetas() == null) pdi.setEtiquetas(new ArrayList<>());

                // Reemplazo por las detectadas (orden estable + sin duplicados)
                LinkedHashSet<String> únicas = new LinkedHashSet<>();
                for (String l : labels) {
                    if (l != null && !l.isBlank()) únicas.add(l.trim());
                }
                pdi.getEtiquetas().clear();
                pdi.getEtiquetas().addAll(únicas);
            }
        } catch (Exception e) {
            log.warn("Etiquetado falló para URL {}: {}", url, e.toString());
            // no propago: el ProcesadorPdI hará fallback si corresponde
        }
    }
}
