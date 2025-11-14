package ar.edu.utn.dds.k3003.clients;

import ar.edu.utn.dds.k3003.facades.dtos.PdIDTO;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface SearchRetrofitClient {

    // POST /webhooks/pdis
    @POST("/events/pdiUpsert")
    Call<Void> indexPdi(@Body PdIDTO pdi);
}
