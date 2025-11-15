package ar.edu.utn.dds.k3003.clients;

import ar.edu.utn.dds.k3003.facades.dtos.PdIDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

@Service
@Profile("deploy")
public class SearchProxy {

    private final String endpoint;
    private final SearchRetrofitClient service;

    public SearchProxy(ObjectMapper objectMapper) {
        // URL del SearchService (variable de entorno)
        var env = System.getenv();
        this.endpoint = env.getOrDefault("URL_BUSQUEDA", "https://busquedaservice.onrender.com");

        var retrofit = new Retrofit.Builder()
                .baseUrl(endpoint.endsWith("/") ? endpoint : endpoint + "/")
                .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                .build();

        this.service = retrofit.create(SearchRetrofitClient.class);
    }

    /**
     * Enviá el PDI recién creado/procesado para indexarlo.
     * No rompemos el flujo si falla: log y seguimos (indexado “best-effort”).
     */
    @SneakyThrows
    public void indexPdi(PdIDTO pdi) {
        try {
            Response<Void> resp = service.indexPdi(pdi).execute();
            if (!resp.isSuccessful()) {
                System.err.println("[SearchProxy] Falló indexado PDI: HTTP " + resp.code());
            }
        } catch (Exception ex) {
            System.err.println("[SearchProxy] Error conectando a SearchService: " + ex.getMessage());
        }
    }
}
