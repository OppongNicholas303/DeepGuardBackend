package DeepQuard.DeepGuardBackend.service.auth;

import DeepQuard.DeepGuardBackend.dto.response.LoginResponse;

public interface AuthImpl {
    LoginResponse login(String email, String rawPassword, String ip, String userAgent);
}
