package pe.edu.vallegrande.spring_reactive.service;

import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.spring_reactive.modal.Users;
import pe.edu.vallegrande.spring_reactive.modal.Conversation;
import pe.edu.vallegrande.spring_reactive.modal.Messages;
import pe.edu.vallegrande.spring_reactive.repository.UsersRepository;
import pe.edu.vallegrande.spring_reactive.repository.ConversationRepository;
import pe.edu.vallegrande.spring_reactive.repository.MessagesRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final UsersRepository usersRepository;
    private final ConversationRepository conversationRepository;
    private final MessagesRepository messagesRepository;

    private final OkHttpClient client = new OkHttpClient();

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    // Métodos para Users
    public Flux<Users> getAllUsers() {
        return usersRepository.findAllActive();
    }

    public Mono<Users> createUser(Users user) {
        user.setActive("A");
        user.setCreatedAt(LocalDateTime.now());
        return usersRepository.save(user);
    }

    // Métodos para Conversations
    public Mono<Conversation> startConversation(Long userId) {
        return usersRepository.findById(userId)
                .filter(user -> "A".equals(user.getActive()))
                .flatMap(user -> {
                    Conversation conversation = new Conversation();
                    conversation.setUserId(userId);
                    conversation.setStartTime(LocalDateTime.now());
                    conversation.setActive("A");
                    return conversationRepository.save(conversation);
                });
    }

    public Mono<Conversation> endConversation(Long conversationId) {
        return conversationRepository.findById(conversationId)
                .filter(conv -> "A".equals(conv.getActive()))
                .flatMap(conv -> {
                    conv.setEndTime(LocalDateTime.now());
                    return conversationRepository.save(conv);
                });
    }

    // Métodos para Messages con integración del servicio cognitivo
    public Mono<Messages> processMessage(Long conversationId, String query) {
        return conversationRepository.findById(conversationId)
                .filter(conv -> "A".equals(conv.getActive()))
                .flatMap(conv -> Mono.fromCallable(() -> callAiService(query))
                        .map(this::extractResponseText)
                        .flatMap(response -> {
                            Messages message = new Messages();
                            message.setConversationId(conversationId);
                            message.setQuery(query);
                            message.setResponse(response);
                            message.setSentAt(LocalDateTime.now());
                            message.setActive("A");
                            return messagesRepository.save(message);
                        }));
    }

    public Flux<Messages> getConversationHistory(Long conversationId) {
        return messagesRepository.findByConversationIdAndActive(conversationId);
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

    // Nuevo método para actualizar un mensaje existente
    public Mono<Messages> updateMessage(Long messageId, String newQuery) {
        return messagesRepository.findById(messageId)
                .filter(msg -> "A".equals(msg.getActive()))
                .flatMap(existingMessage -> 
                    conversationRepository.findById(existingMessage.getConversationId())
                        .filter(conv -> "A".equals(conv.getActive()))
                        .flatMap(conv -> 
                            Mono.fromCallable(() -> callAiService(newQuery))
                                .map(this::extractResponseText)
                                .flatMap(newResponse -> {
                                    existingMessage.setQuery(newQuery);
                                    existingMessage.setResponse(newResponse);
                                    existingMessage.setSentAt(LocalDateTime.now());
                                    return messagesRepository.save(existingMessage);
                                })
                        )
                )
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Message not found or inactive: " + messageId)));
    }

    // Método para actualizar varios mensajes en una conversación
    public Flux<Messages> updateConversationMessages(Long conversationId, String newQuery) {
        return messagesRepository.findByConversationIdAndActive(conversationId)
                .flatMap(message -> updateMessage(message.getId(), newQuery));
    }
    
}