package pe.edu.vallegrande.spring_reactive.modal;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("users")

public class Users {
    @Id
    private Long id;
    private String name;
    private String email;
    private LocalDateTime createdAt = LocalDateTime.now();
    private String active;
}