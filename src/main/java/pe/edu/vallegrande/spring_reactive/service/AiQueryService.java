package pe.edu.vallegrande.spring_reactive.service;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.spring_reactive.modal.AiQuery;
import pe.edu.vallegrande.spring_reactive.repository.AiQueryRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.io.IOException;
import java.time.LocalDate;

@Service
public class AiQueryService {

    @Autowired
    private AiQueryRepository aiQueryRepository;

    private final OkHttpClient client = new OkHttpClient();

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    public Flux<AiQuery> getAllQueriesOrderedById() {
        return aiQueryRepository.findAllByOrderById();
    }

    public Mono<AiQuery> createQuery(String queryText) {
        return Mono.fromCallable(() -> callAiService(queryText))
                .map(this::extractResponseText)
                .flatMap(responseText -> {
                    AiQuery aiQuery = new AiQuery();
                    aiQuery.setQuery(queryText);
                    aiQuery.setResponse(responseText);
                    aiQuery.setTime(LocalDate.now());
                    return aiQueryRepository.save(aiQuery);
                });
    }

    // Actualizar consulta existente por ID y obtener nueva respuesta
    public Mono<AiQuery> updateQuery(Long id, String newQueryText) {
        return aiQueryRepository.findById(id)
                .flatMap(existingQuery -> Mono.fromCallable(() -> callAiService(newQueryText))
                        .map(this::extractResponseText)
                        .flatMap(newResponse -> {
                            existingQuery.setQuery(newQueryText);
                            existingQuery.setResponse(newResponse);
                            existingQuery.setTime(LocalDate.now());
                            return aiQueryRepository.save(existingQuery);
                        }))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Query not found with id: " + id)));
    }

    private String callAiService(String queryText) {
        MediaType mediaType = MediaType.get("application/json; charset=utf-8");
        String escapedQueryText = queryText.replace("\"", "\\\"");
        String jsonBody = "{ \"contents\": [{ \"parts\": [{ \"text\": \"" + escapedQueryText + "\" }] }] }";
        @SuppressWarnings("deprecation")
        RequestBody body = RequestBody.create(mediaType, jsonBody);

        Request request = new Request.Builder()
                .url(apiUrl + "?key=" + apiKey)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            return response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    private String extractResponseText(String responseText) {
        try {
            JSONObject jsonResponse = new JSONObject(responseText);
            JSONArray candidates = jsonResponse.getJSONArray("candidates");
            if (candidates.length() > 0) {
                JSONObject firstCandidate = candidates.getJSONObject(0);
                JSONObject content = firstCandidate.getJSONObject("content");
                JSONArray parts = content.getJSONArray("parts");
                if (parts.length() > 0) {
                    return parts.getJSONObject(0).getString("text").trim();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "No se pudo extraer la respuesta";
    }

    // Método para eliminar un registro por ID usando la consulta personalizada
    public Mono<Void> deleteQueryById(Long id) {
        return aiQueryRepository.deleteQueryById(id);
    }
    
}