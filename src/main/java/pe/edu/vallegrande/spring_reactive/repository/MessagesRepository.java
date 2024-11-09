package pe.edu.vallegrande.spring_reactive.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import pe.edu.vallegrande.spring_reactive.modal.Messages;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MessagesRepository extends ReactiveCrudRepository<Messages, Long> {

    @Query("SELECT * FROM messages WHERE active = 'A'")
    Flux<Messages> findAllActive();

    @Query("SELECT * FROM messages WHERE conversation_id = :conversationId AND active = 'A'")
    Flux<Messages> findByConversationIdAndActive(Long conversationId);

    @Query("UPDATE messages SET active = 'I' WHERE id = :id")
    Mono<Void> logicalDelete(Long id);

    @Query("SELECT * FROM messages WHERE query ILIKE '%' || :query || '%' AND active = 'A'")
    Flux<Messages> findByQueryContainingAndActive(String query);

    @Query("SELECT * FROM messages WHERE conversation_id = :conversationId AND active = 'A' ORDER BY sent_at DESC LIMIT 1")
    Mono<Messages> findTopByConversationIdAndActiveOrderBySentAtDesc(Long conversationId);
    
}