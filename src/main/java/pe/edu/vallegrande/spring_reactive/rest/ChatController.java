package pe.edu.vallegrande.spring_reactive.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.edu.vallegrande.spring_reactive.modal.Users;
import pe.edu.vallegrande.spring_reactive.modal.Conversation;
import pe.edu.vallegrande.spring_reactive.modal.Messages;
import pe.edu.vallegrande.spring_reactive.service.ChatService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    
    private final ChatService chatService;

    // Endpoints para Users
    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Users> createUser(@RequestBody Users user) {
        return chatService.createUser(user);
    }

    @GetMapping("/users")
    public Flux<Users> getAllUsers() {
        return chatService.getAllUsers();
    }

    // Endpoint para obtener todas las conversaciones activas
    @GetMapping("/users/{userId}/conversations")
    public Flux<Conversation> getAllActiveConversationsByUserId(@PathVariable Long userId) {
        return chatService.getAllActiveConversationsByUserId(userId);
    }

    // Endpoints para Conversations
    @PostMapping("/conversations/{userId}")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Conversation> startConversation(@PathVariable Long userId) {
        return chatService.startConversation(userId);
    }

    @PutMapping("/conversations/{conversationId}/end")
    public Mono<Conversation> endConversation(@PathVariable Long conversationId) {
        return chatService.endConversation(conversationId);
    }

    // Endpoint para actualizar un mensaje específico
    @PutMapping("/messages/{messageId}")
    public Mono<Messages> updateMessage(
            @PathVariable Long messageId,
            @RequestBody MessageRequest request) {
        return chatService.updateMessage(messageId, request.query());
    }

    // Endpoint para actualizar todos los mensajes de una conversación
    @PutMapping("/conversations/{conversationId}/messages/bulk-update")
    public Flux<Messages> updateConversationMessages(
            @PathVariable Long conversationId,
            @RequestBody MessageRequest request) {
        return chatService.updateConversationMessages(conversationId, request.query());
    }

    // Endpoints para Messages
    @PostMapping("/conversations/{conversationId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Messages> sendMessage(
            @PathVariable Long conversationId,
            @RequestBody MessageRequest request) {
        return chatService.processMessage(conversationId, request.query())
                .onErrorResume(e -> {
                    // Manejo de errores: retornar un mensaje de error
                    Messages errorMessage = new Messages();
                    errorMessage.setConversationId(conversationId);
                    errorMessage.setQuery(request.query());
                    errorMessage.setResponse("Error: " + e.getMessage());
                    errorMessage.setActive("A");
                    return Mono.just(errorMessage);
                });
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public Flux<Messages> getConversationHistory(@PathVariable Long conversationId) {
        return chatService.getConversationHistory(conversationId);
    }

    // Endpoint para eliminar lógicamente una conversación
    @PutMapping("/conversations/{conversationId}/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> logicalDeleteConversation(@PathVariable Long conversationId) {
        return chatService.logicalDeleteConversation(conversationId);
    }

    // Manejador de excepciones global para el controlador
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        return Mono.just(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<ErrorResponse> handleException(Exception ex) {
        return Mono.just(new ErrorResponse("Error interno del servidor: " + ex.getMessage()));
    }
}

record MessageRequest(String query) {}

record ErrorResponse(String message) {}