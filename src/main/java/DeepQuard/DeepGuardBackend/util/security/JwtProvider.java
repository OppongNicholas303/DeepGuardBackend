package DeepQuard.DeepGuardBackend.util.security;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtProvider {

    private final String secret;
    private final String issuer;
    private final int accessTokenMinutes;

    public JwtProvider(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.issuer}") String issuer,
                      @Value("${app.jwt.expiration:900000}") int expirationMs) {
        this.secret = secret;
        this.issuer = issuer;
        this.accessTokenMinutes = expirationMs / 60000; // Convert ms to minutes
    }

    public String generateAccessToken(UUID userId, String role) throws JOSEException {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(userId.toString())
                .claim("role", role)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(accessTokenMinutes * 60L)))
                .jwtID(UUID.randomUUID().toString())
                .build();

        JWSSigner signer = new MACSigner(secret.getBytes());
        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    public boolean validateToken(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            JWSVerifier verifier = new MACVerifier(secret.getBytes());
            if (!jwt.verify(verifier)) return false;
            Date exp = jwt.getJWTClaimsSet().getExpirationTime();
            return exp.after(Date.from(Instant.now()));
        } catch (Exception e) {
            return false;
        }
    }

    public String getSubject(String token) throws Exception {
        SignedJWT jwt = SignedJWT.parse(token);
        return jwt.getJWTClaimsSet().getSubject();
    }
}
