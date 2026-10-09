package com.campusknowledgeqa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 32, message = "用户名长度需在 3-32 位")
        @Pattern(regexp = "^[A-Za-z0-9_]*$", message = "用户名只能包含字母、数字、下划线")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 32, message = "密码长度需在 6-32 位")
        String password,

        @Size(max = 64, message = "姓名过长")
        String nickname,

        @Size(max = 32, message = "学号过长")
        String studentNo,

        @Size(max = 128, message = "学院名过长")
        String college) {
}
