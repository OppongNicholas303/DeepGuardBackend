package DeepQuard.DeepGuardBackend.util.security;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.*;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.*;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtProvider {

    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;
    private final String issuer;
    private final int accessTokenMinutes;

    public JwtProvider(ResourceLoader loader, JwtProperties props) throws Exception {
        this.issuer = props.getIssuer();
        this.accessTokenMinutes = props.getAccessTokenValidityMins();
        // load keys from classpath or file
        this.privateKey = loadPrivateKey(loader.getResource(props.getPrivateKeyFile()).getInputStream());
        this.publicKey = loadPublicKey(loader.getResource(props.getPublicKeyFile()).getInputStream());
    }

    private RSAPrivateKey loadPrivateKey(InputStream in) throws Exception {
        String pem = new String(in.readAllBytes(), StandardCharsets.UTF_8)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+","");
        byte[] decoded = java.util.Base64.getDecoder().decode(pem);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) kf.generatePrivate(keySpec);
    }

    private RSAPublicKey loadPublicKey(InputStream in) throws Exception {
        String pem = new String(in.readAllBytes(), StandardCharsets.UTF_8)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+","");
        byte[] decoded = java.util.Base64.getDecoder().decode(pem);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) kf.generatePublic(spec);
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

        JWSSigner signer = new RSASSASigner(privateKey);
        SignedJWT signedJWT = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).build(), claims);
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    public boolean validateToken(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            JWSVerifier verifier = new RSASSAVerifier(publicKey);
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
