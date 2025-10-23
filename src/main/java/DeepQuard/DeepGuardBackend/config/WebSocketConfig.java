package DeepQuard.DeepGuardBackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new AnalysisProgressHandler(), "/ws/analysis")
                .setAllowedOrigins("*");
    }
    
    public static class AnalysisProgressHandler extends TextWebSocketHandler {
        @Override
        public void afterConnectionEstablished(WebSocketSession session) throws Exception {
            System.out.println("WebSocket connection established: " + session.getId());
        }
        
        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
            // Handle incoming messages if needed
            session.sendMessage(new TextMessage("Progress update: Analysis in progress..."));
        }
        
        public void sendProgress(WebSocketSession session, int progress, String status) {
            try {
                String message = String.format("{\"progress\": %d, \"status\": \"%s\"}", progress, status);
                session.sendMessage(new TextMessage(message));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
