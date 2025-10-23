package com.documentanalysis.util.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    private String privateKeyFile = "classpath:keys/jwt_private.pem";
    private String publicKeyFile = "classpath:keys/jwt_public.pem";
    private int accessTokenValidityMins = 15;
    private int refreshTokenValidityDays = 30;
    private String issuer = "deepguard";
    private String audience = "deepguard-users";

    // Getters and setters
    public String getPrivateKeyFile() {
        return privateKeyFile;
    }

    public void setPrivateKeyFile(String privateKeyFile) {
        this.privateKeyFile = privateKeyFile;
    }

    public String getPublicKeyFile() {
        return publicKeyFile;
    }

    public void setPublicKeyFile(String publicKeyFile) {
        this.publicKeyFile = publicKeyFile;
    }

    public int getAccessTokenValidityMins() {
        return accessTokenValidityMins;
    }

    public void setAccessTokenValidityMins(int accessTokenValidityMins) {
        this.accessTokenValidityMins = accessTokenValidityMins;
    }

    public int getRefreshTokenValidityDays() {
        return refreshTokenValidityDays;
    }

    public void setRefreshTokenValidityDays(int refreshTokenValidityDays) {
        this.refreshTokenValidityDays = refreshTokenValidityDays;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }
}