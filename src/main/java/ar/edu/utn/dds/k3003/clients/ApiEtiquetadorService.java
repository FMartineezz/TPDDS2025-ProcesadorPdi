package ar.edu.utn.dds.k3003.clients;

import ar.edu.utn.dds.k3003.model.puertos.EtiquetadorService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@Profile("deploy")
public class ApiEtiquetadorService implements EtiquetadorService{
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper om = new ObjectMapper();
    private final String apiKey = System.getenv().getOrDefault("APILAYER_API_KEY", "");

    @Override
    public List<String> labelsFromUrl(String imageUrl) {
        if (apiKey.isBlank() || imageUrl == null || imageUrl.isBlank()) return List.of();
        try {
            String url = "https://api.apilayer.com/image_labeling/url?url=" +
                    URLEncoder.encode(imageUrl, StandardCharsets.UTF_8);

            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .header("apikey", apiKey)
                    .GET()
                    .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) return List.of();

            JsonNode root = om.readTree(res.body());
            List<String> out = new ArrayList<>();

            if (root.isArray()) {
                // Respuesta real: [ { "confidence": 0.96, "label": "White" }, ... ]
                for (JsonNode item : root) {
                    String label = item.path("label").asText(null);
                    if (label != null && !label.isBlank()) out.add(label.trim());
                }
            } else {
                // Fallback por si la API cambia a { "labels": [...] }
                JsonNode labels = root.path("labels");
                if (labels.isArray()) {
                    for (JsonNode item : labels) {
                        // algunos endpoints usan "name"
                        String label = item.path("label").asText(
                                item.path("name").asText(null)
                        );
                        if (label != null && !label.isBlank()) out.add(label.trim());
                    }
                }
            }

            // Sin duplicados, orden estable
            return new ArrayList<>(new LinkedHashSet<>(out));
        } catch (Exception e) {
            return List.of();
        }
    }
}
