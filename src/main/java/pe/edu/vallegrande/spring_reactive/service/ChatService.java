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
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final UsersRepository usersRepository;
    private final ConversationRepository conversationRepository;
    private final MessagesRepository messagesRepository;

    // Cliente OkHttp con timeouts configurados
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    // Métodos para Users
    public Flux<Users> getAllUsers() {
        return usersRepository.findAllActive();
    }

    // Métodos para crear Users
    public Mono<Users> createUser(Users user) {
        user.setActive("A");
        user.setCreatedAt(LocalDateTime.now());
        return usersRepository.save(user);
    }

    // Método para obtener todas las conversaciones activas
    public Flux<Conversation> getAllActiveConversationsByUserId(Long userId) {
        return conversationRepository.findByUserIdAndActive(userId);
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

    private String generateConversationTitle(String firstMessage) {
        String prompt = "Genera un título breve y descriptivo (máximo 50 caracteres) para una conversación que comienza con este mensaje: \""
                + firstMessage + "\". Responde solo con el título, sin comillas ni explicaciones adicionales.";

        String response = callAiService(prompt);
        String title = extractResponseText(response);

        // Limitar a 50 caracteres
        if (title.length() > 50) {
            title = title.substring(0, 47) + "...";
        }

        return title;
    }

    // Métodos para Messages con integración del servicio cognitivo
    public Mono<Messages> processMessage(Long conversationId, String query) {
        return conversationRepository.findById(conversationId)
                .filter(conv -> "A".equals(conv.getActive()))
                .flatMap(conv ->
                        // Verificar si es el primer mensaje
                        messagesRepository.findByConversationIdAndActive(conversationId)
                                .collectList()
                                .flatMap(existingMessages -> {
                                    boolean isFirstMessage = existingMessages.isEmpty();

                                    return Mono.fromCallable(() -> callAiService(query))
                                            .map(this::extractResponseText)
                                            .flatMap(response -> {
                                                Messages message = new Messages();
                                                message.setConversationId(conversationId);
                                                message.setQuery(query);
                                                message.setResponse(response);
                                                message.setSentAt(LocalDateTime.now());
                                                message.setActive("A");

                                                // Si es el primer mensaje, generar título
                                                if (isFirstMessage) {
                                                    String title = generateConversationTitle(query);
                                                    conv.setTitle(title);
                                                    return conversationRepository.save(conv)
                                                            .then(messagesRepository.save(message));
                                                }

                                                return messagesRepository.save(message);
                                            })
                                            .onErrorResume(e -> {
                                                // Manejo de errores: guardar mensaje de error
                                                Messages errorMessage = new Messages();
                                                errorMessage.setConversationId(conversationId);
                                                errorMessage.setQuery(query);
                                                errorMessage.setResponse("Error al procesar el mensaje: " + e.getMessage());
                                                errorMessage.setSentAt(LocalDateTime.now());
                                                errorMessage.setActive("A");
                                                return messagesRepository.save(errorMessage);
                                            });
                                }));
    }

    public Flux<Messages> getConversationHistory(Long conversationId) {
        return messagesRepository.findByConversationIdAndActive(conversationId);
    }

    private String callAiService(String queryText) {
        MediaType mediaType = MediaType.get("application/json; charset=utf-8");
        String escapedQueryText = queryText.replace("\"", "\\\"").replace("\n", "\\n");
        String jsonBody = "{ \"contents\": [{ \"parts\": [{ \"text\": \"" + escapedQueryText + "\" }] }] }";
        
        RequestBody body = RequestBody.create(mediaType, jsonBody);

        Request request = new Request.Builder()
                .url(apiUrl + "?key=" + apiKey)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Sin detalles";
                throw new IOException("Error HTTP " + response.code() + ": " + response.message() + " - " + errorBody);
            }
            
            String responseBody = response.body() != null ? response.body().string() : null;
            
            // Validar que la respuesta no sea nula o vacía
            if (responseBody == null || responseBody.trim().isEmpty()) {
                throw new IOException("Respuesta vacía del servidor");
            }
            
            // Validar que sea un JSON válido
            if (!responseBody.trim().startsWith("{") && !responseBody.trim().startsWith("[")) {
                throw new IOException("Respuesta no es un JSON válido: " + responseBody);
            }
            
            return responseBody;
            
        } catch (IOException e) {
            System.err.println("Error en callAiService: " + e.getMessage());
            e.printStackTrace();
            // Retornar un JSON de error válido
            return "{\"error\": \"" + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    private String extractResponseText(String responseText) {
        try {
            JSONObject jsonResponse = new JSONObject(responseText);
            
            // Verificar si hay un error
            if (jsonResponse.has("error")) {
                String errorMsg = jsonResponse.getString("error");
                System.err.println("Error en la respuesta de la API: " + errorMsg);
                return "Error al comunicarse con el servicio de IA: " + errorMsg;
            }
            
            // Extraer el texto de la respuesta
            if (jsonResponse.has("candidates")) {
                JSONArray candidates = jsonResponse.getJSONArray("candidates");
                if (candidates.length() > 0) {
                    JSONObject firstCandidate = candidates.getJSONObject(0);
                    
                    if (firstCandidate.has("content")) {
                        JSONObject content = firstCandidate.getJSONObject("content");
                        
                        if (content.has("parts")) {
                            JSONArray parts = content.getJSONArray("parts");
                            if (parts.length() > 0) {
                                JSONObject firstPart = parts.getJSONObject(0);
                                if (firstPart.has("text")) {
                                    return firstPart.getString("text").trim();
                                }
                            }
                        }
                    }
                }
            }
        } catch (JSONException e) {
            System.err.println("Error al parsear JSON: " + e.getMessage());
            System.err.println("Respuesta recibida: " + responseText);
            e.printStackTrace();
            return "Error al procesar la respuesta del servicio de IA. Por favor, intenta nuevamente.";
        }
        
        return "No se pudo obtener una respuesta del servicio de IA.";
    }

    // Nuevo método para actualizar un mensaje existente
    public Mono<Messages> updateMessage(Long messageId, String newQuery) {
        return messagesRepository.findById(messageId)
                .filter(msg -> "A".equals(msg.getActive()))
                .flatMap(existingMessage -> conversationRepository.findById(existingMessage.getConversationId())
                        .filter(conv -> "A".equals(conv.getActive()))
                        .flatMap(conv -> Mono.fromCallable(() -> callAiService(newQuery))
                                .map(this::extractResponseText)
                                .flatMap(newResponse -> {
                                    existingMessage.setQuery(newQuery);
                                    existingMessage.setResponse(newResponse);
                                    existingMessage.setSentAt(LocalDateTime.now());
                                    return messagesRepository.save(existingMessage);
                                })
                                .onErrorResume(e -> {
                                    existingMessage.setQuery(newQuery);
                                    existingMessage.setResponse("Error al actualizar: " + e.getMessage());
                                    existingMessage.setSentAt(LocalDateTime.now());
                                    return messagesRepository.save(existingMessage);
                                })))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Message not found or inactive: " + messageId)));
    }

    // Método para actualizar varios mensajes en una conversación
    public Flux<Messages> updateConversationMessages(Long conversationId, String newQuery) {
        return messagesRepository.findByConversationIdAndActive(conversationId)
                .flatMap(message -> updateMessage(message.getId(), newQuery));
    }

    // Método para eliminar lógicamente una conversación
    public Mono<Void> logicalDeleteConversation(Long conversationId) {
        return conversationRepository.logicalDelete(conversationId);
    }
}