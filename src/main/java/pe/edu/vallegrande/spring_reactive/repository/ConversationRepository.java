package pe.edu.vallegrande.spring_reactive.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import pe.edu.vallegrande.spring_reactive.modal.Conversation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ConversationRepository extends ReactiveCrudRepository<Conversation, Long> {

    @Query("SELECT * FROM conversations WHERE active = 'A'")
    Flux<Conversation> findAllActive();

    @Query("SELECT * FROM conversations WHERE user_id = :userId AND active = 'A'")
    Flux<Conversation> findByUserIdAndActive(Long userId);

    @Query("UPDATE conversations SET active = 'I' WHERE id = :id")
    Mono<Void> logicalDelete(Long id);
    
}