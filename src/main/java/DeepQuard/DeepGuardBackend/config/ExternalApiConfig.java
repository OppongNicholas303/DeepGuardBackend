package DeepQuard.DeepGuardBackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ExternalApiConfig {
    
    @Value("${app.ai.together.base-url:https://api.together.xyz/v1}")
    private String togetherAiBaseUrl;
    
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
    
    @Bean
    public WebClient togetherAiClient(WebClient.Builder builder) {
        return builder
                .baseUrl(togetherAiBaseUrl)
                .build();
    }
    
    @Bean
    public WebClient breachApiClient(WebClient.Builder builder) {
        return builder
                .baseUrl("https://haveibeenpwned.com/api/v3")
                .build();
    }
}

