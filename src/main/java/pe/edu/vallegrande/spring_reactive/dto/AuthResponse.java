package pe.edu.vallegrande.spring_reactive.dto;

public record AuthResponse(
    Long id,
    String name,
    String email,
    String message
) {}