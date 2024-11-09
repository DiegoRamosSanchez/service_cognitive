package pe.edu.vallegrande.spring_reactive.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.edu.vallegrande.spring_reactive.modal.AiQuery;
import pe.edu.vallegrande.spring_reactive.modal.QueryRequest;
import pe.edu.vallegrande.spring_reactive.service.AiQueryService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/queries")
@CrossOrigin(origins = "*")
public class AiQueryRest {

    @Autowired
    private AiQueryService aiQueryService;

    @GetMapping
    public Flux<AiQuery> getAllQueries() {
        return aiQueryService.getAllQueriesOrderedById();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<AiQuery> createQuery(@RequestBody QueryRequest queryRequest) {
        return aiQueryService.createQuery(queryRequest.getQuery())
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Query creation failed")));
    }

    @PutMapping("/{id}")
    public Mono<AiQuery> updateQuery(@PathVariable Long id, @RequestBody QueryRequest queryRequest) {
        return aiQueryService.updateQuery(id, queryRequest.getQuery())
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Query update failed")));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteQuery(@PathVariable Long id) {
        return aiQueryService.deleteQueryById(id);
    }
}