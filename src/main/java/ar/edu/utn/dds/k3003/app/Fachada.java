package ar.edu.utn.dds.k3003.app;

import ar.edu.utn.dds.k3003.facades.FachadaProcesadorPdI;
import ar.edu.utn.dds.k3003.repository.InMemoryPdIRepo;
import ar.edu.utn.dds.k3003.model.PdI;
import ar.edu.utn.dds.k3003.model.ProcesadorPdI;
import ar.edu.utn.dds.k3003.facades.FachadaSolicitudes;
import ar.edu.utn.dds.k3003.facades.dtos.PdIDTO;
import ar.edu.utn.dds.k3003.repository.PdIRepository;
import lombok.Setter;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.*;

@Service
@Transactional
public class Fachada implements FachadaProcesadorPdI {

    @Setter
    private PdIRepository Repository;
    private FachadaSolicitudes fachadaSolicitudes;
    private MeterRegistry meterRegistry;

    @Autowired
    public Fachada(PdIRepository pdIRepository, FachadaSolicitudes fachadaSolicitudes) {
        this.Repository= pdIRepository;
        this.fachadaSolicitudes = fachadaSolicitudes;
        this.meterRegistry = meterRegistry;
    }

    @Override
    /*public PdIDTO procesar(PdIDTO pdIDTO) {
        try {
            // Validación con solicitudes
            ValidacionFachadaSolicitudes(pdIDTO);

            // Si ya existe
            Optional<PdI> yaExistente = Repository.findByHechoId(pdIDTO.hechoId())
                    .stream()
                    .filter(p -> sonIgualesSinId(p, convertirADomino(pdIDTO)))
                    .findFirst();

            if (yaExistente.isPresent()) {
                meterRegistry.counter("dds.pdi.procesar", "status", "reused").increment();
                return convertirADto(yaExistente.get());
            }

            // Nuevo
            PdIDTO procesado = procesarNuveoPdI(pdIDTO);
            meterRegistry.counter("dds.pdi.procesar", "status", "new").increment();
            return procesado;

        } catch (IllegalStateException e) {
            meterRegistry.counter("dds.pdi.procesar", "status", "error").increment();
            throw e;
        }
    }*/
    public PdIDTO procesar(PdIDTO pdIDTO) throws IllegalStateException {
        ValidacionFachadaSolicitudes(pdIDTO);
        PdI pdI = convertirADomino(pdIDTO);

        //Buscar si ya fue procesado
        List<PdI> pdisPorHecho = this.Repository.findByHechoId(pdI.getHechoId());
        Optional<PdI> yaExistente = pdisPorHecho.stream()
                .filter(existe -> sonIgualesSinId(existe, pdI))
                .findFirst();

        //Si ya existe lo convierte a dto y devuelve el ya existente, sino procesa el PdI nuevo y devuelve ese
        return yaExistente.map(this::convertirADto).orElseGet(() -> procesarNuveoPdI(pdIDTO));
        /*if (yaExistente.isPresent()) {
            // Contador para PDI reusados
            meterRegistry.counter("dds.pdi.procesar", "status", "reused").increment();
            return convertirADto(yaExistente.get());
        } else {
            // Contador para PDI nuevos
            meterRegistry.counter("dds.pdi.procesar", "status", "new").increment();
            return procesarNuveoPdI(pdIDTO);
        }*/
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
        ProcesadorPdI procesador = new ProcesadorPdI();
        PdI dominio = convertirADomino(entrada);
        PdI PdIprocesado = procesador.procesar(dominio);
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
    private PdIDTO convertirADto(PdI pdi){
        return new PdIDTO(String.valueOf(pdi.getId()), pdi.getHechoId(), pdi.getDescripcion(),
                pdi.getLugar(), pdi.getMomento(),pdi.getContenido(),pdi.getEtiquetas());
    }

    private PdI convertirADomino(PdIDTO pdiDTO){
        return new PdI(Long.parseLong(pdiDTO.id()),pdiDTO.hechoId(), pdiDTO.descripcion(),
                pdiDTO.lugar(), pdiDTO.momento(),pdiDTO.contenido(),
                new ArrayList<>(pdiDTO.etiquetas()));
    }

    @Override
    public void eliminarTodos() {
        Repository.deleteAll();
    }

}
