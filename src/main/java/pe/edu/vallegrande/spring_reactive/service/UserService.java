package pe.edu.vallegrande.spring_reactive.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.spring_reactive.dto.AuthResponse;
import pe.edu.vallegrande.spring_reactive.dto.LoginRequest;
import pe.edu.vallegrande.spring_reactive.dto.RegisterRequest;
import pe.edu.vallegrande.spring_reactive.modal.Users;
import pe.edu.vallegrande.spring_reactive.repository.UsersRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;

    public Flux<Users> getAllUsers() {
        return usersRepository.findAllActive();
    }

    public Mono<Users> createUser(Users user) {
        user.setActive("A");
        user.setCreatedAt(LocalDateTime.now());
        // Encriptar la contraseña antes de guardar
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return usersRepository.save(user);
    }

    public Mono<Users> findById(Long userId) {
        return usersRepository.findById(userId)
                .filter(user -> "A".equals(user.getActive()));
    }

    // Método para registrar un nuevo usuario
    public Mono<AuthResponse> register(RegisterRequest request) {
        return usersRepository.existsByEmail(request.email())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new IllegalArgumentException("El email ya está registrado"));
                    }
                    
                    Users newUser = new Users();
                    newUser.setName(request.name());
                    newUser.setEmail(request.email());
                    newUser.setPassword(passwordEncoder.encode(request.password()));
                    newUser.setActive("A");
                    newUser.setCreatedAt(LocalDateTime.now());
                    
                    return usersRepository.save(newUser)
                            .map(user -> new AuthResponse(
                                user.getId(),
                                user.getName(),
                                user.getEmail(),
                                "Usuario registrado exitosamente"
                            ));
                });
    }

    // Método para login (autenticación)
    public Mono<AuthResponse> login(LoginRequest request) {
        return usersRepository.findByEmailAndActive(request.email())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Email o contraseña incorrectos")))
                .flatMap(user -> {
                    if (passwordEncoder.matches(request.password(), user.getPassword())) {
                        return Mono.just(new AuthResponse(
                            user.getId(),
                            user.getName(),
                            user.getEmail(),
                            "Login exitoso"
                        ));
                    } else {
                        return Mono.error(new IllegalArgumentException("Email o contraseña incorrectos"));
                    }
                });
    }

    // Método para cambiar contraseña
    public Mono<Users> changePassword(Long userId, String oldPassword, String newPassword) {
        return usersRepository.findById(userId)
                .filter(user -> "A".equals(user.getActive()))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Usuario no encontrado")))
                .flatMap(user -> {
                    if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                        return Mono.error(new IllegalArgumentException("La contraseña actual es incorrecta"));
                    }
                    
                    user.setPassword(passwordEncoder.encode(newPassword));
                    return usersRepository.save(user);
                });
    }

    // Método para eliminar lógicamente un usuario
    public Mono<Void> logicalDelete(Long userId) {
        return usersRepository.logicalDelete(userId);
    }
}