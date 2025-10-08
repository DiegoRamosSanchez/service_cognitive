package pe.edu.vallegrande.spring_reactive.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.spring_reactive.modal.Users;
import pe.edu.vallegrande.spring_reactive.repository.UsersRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UsersRepository usersRepository;

    public Flux<Users> getAllUsers() {
        return usersRepository.findAllActive();
    }

    public Mono<Users> createUser(Users user) {
        user.setActive("A");
        user.setCreatedAt(LocalDateTime.now());
        return usersRepository.save(user);
    }

    public Mono<Users> findById(Long userId) {
        return usersRepository.findById(userId)
                .filter(user -> "A".equals(user.getActive()));
    }
}