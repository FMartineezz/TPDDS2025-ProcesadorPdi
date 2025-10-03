package ar.edu.utn.dds.k3003.model;


import jakarta.persistence.*;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


@Data
@Entity
public class PdI {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String hechoId;
    private String descripcion;
    private String lugar;
    private LocalDateTime momento;
    private String contenido;
    @ElementCollection
    private List<String> etiquetas;
    private String urlImagen;
    @Lob
    @Column(name = "resultado_ocr", columnDefinition = "TEXT")
    private String resultadoOcr;


    public PdI(){}

    public PdI(Long id,String hechoId, String descripcion, String lugar, LocalDateTime momento, String contenido, List<String> etiquetas, String resultadoOcr, String urlImagen) {
        this.id = id;
        this.hechoId = hechoId;
        this.descripcion = descripcion;
        this.lugar = lugar;
        this.momento = momento;
        this.contenido = contenido;
        this.etiquetas = etiquetas;
        this.resultadoOcr = resultadoOcr;
        this.urlImagen = urlImagen;
    }

    public PdI(String hechoId, String descripcion, String lugar,
               LocalDateTime momento, String contenido, List<String> etiquetas, String resultadoOcr, String urlImagen) {
        this.hechoId = hechoId;
        this.descripcion = descripcion;
        this.lugar = lugar;
        this.momento = momento;
        this.contenido = contenido;
        this.etiquetas = etiquetas;
        this.resultadoOcr = resultadoOcr;
        this.urlImagen = urlImagen;
    }

    @Deprecated
    public void agregarEtiqueta(){
        etiquetas.add("Etiquetado");
    }
}
