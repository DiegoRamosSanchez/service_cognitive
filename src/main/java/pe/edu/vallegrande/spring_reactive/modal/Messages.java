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
@Table("messages")
public class Messages {
    @Id
    private Long id;
    private Long conversationId;
    private String query;
    private String response;
    private LocalDateTime sentAt = LocalDateTime.now();
    private String active;
}