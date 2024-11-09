package pe.edu.vallegrande.spring_reactive.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        // Establece la URL directamente
        String serverUrl = "https://potential-succotash-vxvg69jxrwp3wrjj-8080.app.github.dev";

        return new OpenAPI()
            .info(new Info()
                .title("Tu API")
                .version("1.0")
                .description("Documentación de la API"))
            .servers(List.of(new Server().url(serverUrl)));
    }
}
