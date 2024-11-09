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
@Table("conversations")
public class Conversation {
    @Id
    private Long id;
    private Long userId;
    private LocalDateTime startTime = LocalDateTime.now();
    private LocalDateTime endTime;
    private String active;
}