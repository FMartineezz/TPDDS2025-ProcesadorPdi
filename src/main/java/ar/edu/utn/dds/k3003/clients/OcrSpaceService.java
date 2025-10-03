package ar.edu.utn.dds.k3003.clients;

import ar.edu.utn.dds.k3003.model.puertos.OcrService;
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
import java.util.UUID;

@Service
@Profile("deploy")
public class OcrSpaceService implements OcrService {

    private static final String ENDPOINT = "https://api.ocr.space/parse/image";
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper om = new ObjectMapper();
    private final String apiKey = System.getenv().getOrDefault("OCRSPACE_API_KEY", "");

    @Override
    public String extractTextFromUrl(String imageUrl) {
        if (apiKey.isBlank() || imageUrl == null || imageUrl.isBlank()) return null;

        try {
            String boundary = "----JavaFormBoundary" + UUID.randomUUID();
            byte[] body = buildMultipartBody(boundary, imageUrl);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT))
                    .header("apikey", apiKey) // apikey en header
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) return null;

            JsonNode root = om.readTree(res.body());
            JsonNode results = root.path("ParsedResults");
            if (results.isArray() && results.size() > 0) {
                return results.get(0).path("ParsedText").asText(null);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Construye multipart/form-data con:
     *  - url: <imageUrl>
     *  - (opcionales comentados: language / OCREngine / isOverlayRequired)
     */
    private byte[] buildMultipartBody(String boundary, String imageUrl) {
        String CRLF = "\r\n";
        StringBuilder sb = new StringBuilder();

        // campo url
        sb.append("--").append(boundary).append(CRLF);
        sb.append("Content-Disposition: form-data; name=\"url\"").append(CRLF).append(CRLF);
        sb.append(imageUrl).append(CRLF);

        // (opcional) idioma, por ejemplo "spa" o "eng"
        // sb.append("--").append(boundary).append(CRLF);
        // sb.append("Content-Disposition: form-data; name=\"language\"").append(CRLF).append(CRLF);
        // sb.append("spa").append(CRLF);

        // (opcional) motor OCR
        // sb.append("--").append(boundary).append(CRLF);
        // sb.append("Content-Disposition: form-data; name=\"OCREngine\"").append(CRLF).append(CRLF);
        // sb.append("2").append(CRLF);

        // (opcional) overlay
        // sb.append("--").append(boundary).append(CRLF);
        // sb.append("Content-Disposition: form-data; name=\"isOverlayRequired\"").append(CRLF).append(CRLF);
        // sb.append("false").append(CRLF);

        // cierre
        sb.append("--").append(boundary).append("--").append(CRLF);

        return sb.toString().getBytes(StandardCharsets.UTF_8);
        // Si necesitás archivos binarios en el futuro, armá el body con ByteArrayOutputStream en lugar de StringBuilder.
    }
}
