                                                                                                                                                                                                                                                                package ar.edu.utn.dds.k3003.facades;


                                                                                                                                                                                                                                                                import ar.edu.utn.dds.k3003.facades.dtos.PdIDTO;
                                                                                                                                                                                                                                                                import ar.edu.utn.dds.k3003.model.PdI;

                                                                                                                                                                                                                                                                import java.util.List;
                                                                                                                                                                                                                                                                import java.util.NoSuchElementException;

                                                                                                                                                                                                                                                                public interface FachadaProcesadorPdI {
                                                                                                                                                                                                                                                                    PdIDTO procesar(PdIDTO var1) throws IllegalStateException;

                                                                                                                                                                                                                                                                    PdIDTO buscarPdIPorId(String var1) throws NoSuchElementException;

                                                                                                                                                                                                                                                                    List<PdIDTO> buscarPorHecho(String var1) throws NoSuchElementException;

                                                                                                                                                                                                                                                                    void setFachadaSolicitudes(FachadaSolicitudes var1);

                                                                                                                                                                                                                                                                    void eliminarTodos();

                                                                                                                                                                                                                                                                    //void indexarEnBuscador(PdIDTO var1)


                                                                                                                                                                                                                                                                }
