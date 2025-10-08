package pe.edu.vallegrande.spring_reactive.dto;

public record UserResponse(
    Long id,
    String name,
    String email,
    String createdAt,
    String active
) {}