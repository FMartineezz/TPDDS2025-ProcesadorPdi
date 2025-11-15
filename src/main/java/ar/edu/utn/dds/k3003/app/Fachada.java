package ar.edu.utn.dds.k3003.app;

import ar.edu.utn.dds.k3003.clients.SearchProxy;
import ar.edu.utn.dds.k3003.facades.FachadaProcesadorPdI;
import ar.edu.utn.dds.k3003.repository.InMemoryPdIRepo;
import ar.edu.utn.dds.k3003.model.PdI;
import lombok.extern.slf4j.Slf4j;
import ar.edu.utn.dds.k3003.facades.FachadaSolicitudes;
import ar.edu.utn.dds.k3003.facades.dtos.PdIDTO;
import ar.edu.utn.dds.k3003.repository.PdIRepository;
import lombok.Setter;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.net.URI;
import java.util.*;

@Slf4j
@Service
@Transactional
public class Fachada implements FachadaProcesadorPdI {

    @Setter
    private PdIRepository Repository;
    private FachadaSolicitudes fachadaSolicitudes;
    private MeterRegistry meterRegistry;
    private ar.edu.utn.dds.k3003.model.ProcesadorPdI procesador;
    private SearchProxy searchProxy;
    private final Timer pdiProcessTimer;

    @Autowired
    public Fachada(PdIRepository pdIRepository,
                   FachadaSolicitudes fachadaSolicitudes,
                   MeterRegistry meterRegistry,
                   ar.edu.utn.dds.k3003.model.ProcesadorPdI procesador,
                   SearchProxy searchProxy) {
        this.Repository= pdIRepository;
        this.fachadaSolicitudes = fachadaSolicitudes;
        this.meterRegistry = meterRegistry;
        this.procesador = procesador;
        this.searchProxy = searchProxy;
        this.pdiProcessTimer = meterRegistry.timer("dds.pdi.procesar.tiempo");
    }

    @Override
    public PdIDTO procesar(PdIDTO pdIDTO) {
        return pdiProcessTimer.record(() -> {
        try{
        ValidacionFachadaSolicitudes(pdIDTO);
        PdI pdI = convertirADomino(pdIDTO);

        //Buscar si ya fue procesado
        List<PdI> pdisPorHecho = this.Repository.findByHechoId(pdI.getHechoId());
        Optional<PdI> yaExistente = pdisPorHecho.stream()
                .filter(existe -> sonIgualesSinId(existe, pdI))
                .findFirst();

        //Si ya existe lo convierte a dto y devuelve el ya existente, sino procesa el PdI nuevo y devuelve ese
       // return yaExistente.map(this::convertirADto).orElseGet(() -> procesarNuveoPdI(pdIDTO));
            return yaExistente.map(p -> {
                meterRegistry.counter("dds.pdi.procesar", "status", "reused").increment();
                indexarEnBuscador(convertirADto(p));
                return convertirADto(p);
            }).orElseGet(() -> {
                // Si no existe → proceso uno nuevo y marco la métrica como new
                PdIDTO nuevo = procesarNuveoPdI(pdIDTO);
                meterRegistry.counter("dds.pdi.procesar", "status", "new").increment();
                indexarEnBuscador(nuevo);
                return nuevo;
            });

        } catch (Exception e) {
            meterRegistry.counter("dds.pdi.procesar", "status", "error").increment();
            throw e;
        }
        });
    }

    @Override
    public PdIDTO buscarPdIPorId(String pdiId) throws NoSuchElementException {

        val pdiOptional = this.Repository.findById(Long.parseLong(pdiId));
        if(pdiOptional.isEmpty()){
            throw new NoSuchElementException(pdiId + " No Existe ");
        }
        val pdi = pdiOptional.get();
        return convertirADto(pdi);
    }

    @Override
    public List<PdIDTO> buscarPorHecho(String hecho) {
        List<PdI> encontrados = this.Repository.findByHechoId(hecho);
        if (encontrados.isEmpty()) {
            throw new NoSuchElementException("No se encontró PdI con hecho: " + hecho);
        }
        return encontrados.stream().map(this::convertirADto).toList();
    }

    @Override
    public void setFachadaSolicitudes(FachadaSolicitudes fachadaSolicitudes) {
        this.fachadaSolicitudes = fachadaSolicitudes;
    }

//Metodos privados para mas claridad
    private void ValidacionFachadaSolicitudes(PdIDTO entrada){
        if (this.fachadaSolicitudes == null) {
            throw new IllegalStateException("FachadaSolicitudes no fue inyectada");
        }
        if (!this.fachadaSolicitudes.estaActivo(entrada.hechoId())) {
            throw new IllegalStateException("La solicitud no está activa");
        }
    }

    private PdIDTO procesarNuveoPdI(PdIDTO entrada){
        fachadaSolicitudes.estaActivo(entrada.hechoId()); // Si esta línea no lanza una excepción expresada en el Proxy, el hecho está activo
        //ProcesadorPdI procesador = new ProcesadorPdI();
        PdI dominio = convertirADomino(entrada);
        PdI PdIprocesado = this.procesador.procesar(dominio);
        this.Repository.save(PdIprocesado);
        return convertirADto(PdIprocesado);
    }

    private boolean sonIgualesSinId(PdI a, PdI b) {
        return a.getHechoId().equals(b.getHechoId()) &&
                a.getDescripcion().equals(b.getDescripcion()) &&
                a.getLugar().equals(b.getLugar()) &&
                a.getMomento().equals(b.getMomento()) &&
                a.getContenido().equals(b.getContenido());
    }

//Metodos privados para omision de logica repetida
   /* private PdIDTO convertirADto(PdI pdi){
        return new PdIDTO(String.valueOf(pdi.getId()), pdi.getHechoId(), pdi.getDescripcion(),
                pdi.getLugar(), pdi.getMomento(),pdi.getContenido(),pdi.getEtiquetas());
    }*/
    private PdIDTO convertirADto(PdI pdi){
               return new PdIDTO(
                                String.valueOf(pdi.getId()),
                                pdi.getHechoId(),
                                pdi.getDescripcion(),
                                pdi.getLugar(),
                                pdi.getMomento(),
                                pdi.getContenido(),
                                pdi.getEtiquetas(),
                                pdi.getResultadoOcr(),
                                pdi.getUrlImagen()
                        );
         }
    /*private PdI convertirADomino(PdIDTO pdiDTO){
        return new PdI(Long.parseLong(pdiDTO.id()),pdiDTO.hechoId(), pdiDTO.descripcion(),
                pdiDTO.lugar(), pdiDTO.momento(),pdiDTO.contenido(),
                new ArrayList<>(pdiDTO.etiquetas()));
    }*/
    private PdI convertirADomino(PdIDTO pdiDTO) {
        boolean tieneId = pdiDTO.id() != null && !pdiDTO.id().isBlank();
        if (tieneId) {
            return new PdI(
                    Long.parseLong(pdiDTO.id()),
                    pdiDTO.hechoId(),
                    pdiDTO.descripcion(),
                    pdiDTO.lugar(),
                    pdiDTO.momento(),
                    pdiDTO.contenido(),
                    new ArrayList<>(pdiDTO.etiquetas()),
                    pdiDTO.resultadoOcr(),
                    pdiDTO.urlImagen()
            );
        } else {
            return new PdI(
                    pdiDTO.hechoId(),
                    pdiDTO.descripcion(),
                    pdiDTO.lugar(),
                    pdiDTO.momento(),
                    pdiDTO.contenido(),
                    new ArrayList<>(pdiDTO.etiquetas()),
                    pdiDTO.resultadoOcr(),
                    pdiDTO.urlImagen()
            );
        }
    }

    @Override
    public void eliminarTodos() {
        Repository.deleteAll();
    }


    private void indexarEnBuscador(PdIDTO pdi) {
        try {
            searchProxy.indexPdi(pdi);
        } catch (Exception ex) {
             log.warn("No se pudo indexar PDI {} en buscador", pdi.id(), ex);

        }
    }

}
