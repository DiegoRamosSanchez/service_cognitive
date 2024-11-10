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
@CrossOrigin( origins = "*" )
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
        return chatService.processMessage(conversationId, request.query());
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public Flux<Messages> getConversationHistory(@PathVariable Long conversationId) {
        return chatService.getConversationHistory(conversationId);
    }
}

record MessageRequest(String query) {}