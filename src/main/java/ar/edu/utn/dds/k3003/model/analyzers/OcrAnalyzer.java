package ar.edu.utn.dds.k3003.model.analyzers;

import ar.edu.utn.dds.k3003.model.PdI;
import ar.edu.utn.dds.k3003.model.PdiAnalyzer;
import ar.edu.utn.dds.k3003.model.puertos.OcrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10) // corre antes que el etiquetado
public class OcrAnalyzer implements PdiAnalyzer {
    private static final Logger log = LoggerFactory.getLogger(OcrAnalyzer.class);

    private final OcrService ocr;

    public OcrAnalyzer(OcrService ocr) {
        this.ocr = ocr;
    }

    @Override
    public void apply(PdI pdi) {
        String url = pdi.getUrlImagen();
        if (url == null || url.isBlank()) return;                 // no hay imagen -> no hago OCR
        if (pdi.getResultadoOcr() != null && !pdi.getResultadoOcr().isBlank()) return; // ya seteado

        try {
            String texto = ocr.extractTextFromUrl(url);
            if (texto != null && !texto.isBlank()) {
                pdi.setResultadoOcr(texto);
            }
        } catch (Exception e) {
            log.warn("OCR falló para URL {}: {}", url, e.toString());
            // no propago: el ProcesadorPdI hará fallback si corresponde
        }
    }
}

