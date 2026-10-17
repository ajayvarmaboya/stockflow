package com.stockflow.auth.api;

import java.util.UUID;

public class AuthResponse {
    private final String token;
    private final String tokenType;
    private final UUID userId;
    private final String username;
    private final String role;

    public AuthResponse(String token, UUID userId, String username, String role) {
        this.token = token;
        this.tokenType = "Bearer";
        this.userId = userId;
        this.username = username;
        this.role = role;
    }

    public String getToken() { return token; }
    public String getTokenType() { return tokenType; }
    public UUID getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
}
