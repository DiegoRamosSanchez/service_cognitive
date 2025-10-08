package pe.edu.vallegrande.spring_reactive.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.edu.vallegrande.spring_reactive.dto.*;
import pe.edu.vallegrande.spring_reactive.modal.Users;
import pe.edu.vallegrande.spring_reactive.service.UserService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;

    // Endpoint para registrar un nuevo usuario
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    // Endpoint para login
    @PostMapping("/login")
    public Mono<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }

    // Endpoint para crear usuario (mantener compatibilidad)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Users> createUser(@RequestBody Users user) {
        return userService.createUser(user);
    }

    // Endpoint para obtener todos los usuarios
    @GetMapping
    public Flux<Users> getAllUsers() {
        return userService.getAllUsers();
    }

    // Endpoint para obtener un usuario por ID
    @GetMapping("/{userId}")
    public Mono<Users> getUserById(@PathVariable Long userId) {
        return userService.findById(userId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Usuario no encontrado")));
    }

    // Endpoint para cambiar contraseña
    @PutMapping("/{userId}/change-password")
    public Mono<SuccessResponse> changePassword(
            @PathVariable Long userId,
            @RequestBody ChangePasswordRequest request) {
        return userService.changePassword(userId, request.oldPassword(), request.newPassword())
                .map(user -> new SuccessResponse("Contraseña actualizada exitosamente"));
    }

    // Endpoint para eliminar lógicamente un usuario
    @PutMapping("/{userId}/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteUser(@PathVariable Long userId) {
        return userService.logicalDelete(userId);
    }

    // Manejador de excepciones global para el controlador
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        return Mono.just(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<ErrorResponse> handleException(Exception ex) {
        return Mono.just(new ErrorResponse("Error interno del servidor: " + ex.getMessage()));
    }
}