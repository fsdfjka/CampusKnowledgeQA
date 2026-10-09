package com.campusknowledgeqa.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @Size(max = 64, message = "姓名过长") String nickname,
        @Size(max = 32, message = "学号过长") String studentNo,
        @Size(max = 128, message = "学院名过长") String college,
        @Size(max = 128, message = "专业名过长") String major,
        @Email(message = "邮箱格式不正确") @Size(max = 128, message = "邮箱过长") String email,
        @Pattern(regexp = "^$|^[0-9+\\-]{5,20}$", message = "手机号格式不正确") String phone) {
}
