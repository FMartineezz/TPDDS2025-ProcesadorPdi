package ar.edu.utn.dds.k3003.model.puertos;

import java.util.List;

public interface EtiquetadorService {
    List<String> labelsFromUrl(String imageUrl);
}
