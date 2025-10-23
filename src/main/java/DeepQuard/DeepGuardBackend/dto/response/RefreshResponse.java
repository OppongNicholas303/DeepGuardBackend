package DeepQuard.DeepGuardBackend.dto.response;

public record RefreshResponse(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresInSeconds
) {}

