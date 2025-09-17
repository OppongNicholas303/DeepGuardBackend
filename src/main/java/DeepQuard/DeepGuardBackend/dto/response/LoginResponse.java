package DeepQuard.DeepGuardBackend.dto.response;
public record LoginResponse(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresInSeconds
) {}

