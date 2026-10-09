package com.campusknowledgeqa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 后台新增用户请求 */
public record CreateUserRequest(
        @NotBlank(message = "用户名不能为空") String username,
        String nickname,
        @NotBlank(message = "密码不能为空") @Size(min = 6, message = "密码至少 6 位") String password,
        String role,
        String studentNo,
        String college,
        String major,
        String email,
        String phone) {
}
