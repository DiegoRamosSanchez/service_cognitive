package pe.edu.vallegrande.spring_reactive.modal;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("ai_query")
public class AiQuery {

    @Id
    private Long id;
    private String query;
    private String response;
    private LocalDate time;
}