package pe.edu.vallegrande.spring_reactive.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import pe.edu.vallegrande.spring_reactive.modal.AiQuery;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AiQueryRepository extends ReactiveCrudRepository<AiQuery, Long> {

    Flux<AiQuery> findAllByOrderById();

    @Query("DELETE FROM ai_query WHERE id = :id")
    Mono<Void> deleteQueryById(Long id);
    
}