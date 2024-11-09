package pe.edu.vallegrande.spring_reactive.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import pe.edu.vallegrande.spring_reactive.modal.Users;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UsersRepository extends ReactiveCrudRepository<Users, Long> {

    @Query("SELECT * FROM users WHERE active = 'A'")
    public Flux<Users> findAllActive();  // Asegúrate de que sea público

    @Query("SELECT * FROM users WHERE email = :email AND active = 'A'")
    public Mono<Users> findByEmailAndActive(String email);

    @Query("UPDATE users SET active = 'I' WHERE id = :id")
    public Mono<Void> logicalDelete(Long id);
}
