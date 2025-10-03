package ar.edu.utn.dds.k3003.model;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class ProcesadorPdI {

    /*public PdI procesar(PdI pdI) {
        pdI.agregarEtiqueta();
        return pdI;
    }*/
    private final List<PdiAnalyzer> analyzers;

    // Spring inyecta todos los beans que implementen PdiAnalyzer
    public ProcesadorPdI(List<PdiAnalyzer> analyzers) {
        // orden estable si usás @Order en los analizadores
        List<PdiAnalyzer> ordered = new ArrayList<>(Objects.requireNonNullElse(analyzers, List.of()));
        AnnotationAwareOrderComparator.sort(ordered);
        this.analyzers = List.copyOf(ordered);
    }

    public PdI procesar(PdI pdi) {
        if (pdi == null) throw new IllegalArgumentException("PdI no puede ser null");

        // pipeline extensible: cada analizador puede enriquecer/modificar el PDI
        for (PdiAnalyzer analyzer : analyzers) {
            analyzer.apply(pdi);
        }

        // Back-compat: si no hubo analizadores o no setearon nada, mantené el comportamiento anterior
        /*boolean sinResultados =
                (pdi.getResultadoOcr() == null || pdi.getResultadoOcr().isBlank()) &&
                        (pdi.getEtiquetas() == null || pdi.getEtiquetas().isEmpty());

        if (sinResultados) {
            pdi.agregarEtiqueta();
        }*/

        return pdi;
    }
}
