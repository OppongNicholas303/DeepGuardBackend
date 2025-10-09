package DeepQuard.DeepGuardBackend.controller;

import DeepQuard.DeepGuardBackend.model.User;
import DeepQuard.DeepGuardBackend.repository.UserRepository;
import DeepQuard.DeepGuardBackend.service.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
class DetectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @Test
    @WithMockUser
    void testAnalyzeFile() throws Exception {
        UUID userId = UUID.randomUUID();
        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setEmail("test@example.com");

        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.of(mockUser));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        UserPrincipal userPrincipal = new UserPrincipal(userId, "test@example.com", "password", "Test", "User", "USER", true, "ACTIVE", null);

        mockMvc.perform(multipart("/api/v1/detection/analyze-file")
                        .file(file)
                        .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Analysis started"));
    }

    @Test
    @WithMockUser
    void testGetProgress() throws Exception {
        UUID analysisId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/detection/progress/{id}", analysisId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Progress retrieved"));
    }
}