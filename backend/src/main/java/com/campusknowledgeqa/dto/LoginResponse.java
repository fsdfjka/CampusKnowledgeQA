package com.campusknowledgeqa.dto;

public record LoginResponse(String token, String username, String nickname, String role) {
}
