package com.edunexus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private Integer userId;
    private String username;
    private String email;
    private String fullName;
    private String role;
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
}
