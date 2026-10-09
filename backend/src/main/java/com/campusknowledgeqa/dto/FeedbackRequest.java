package com.campusknowledgeqa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 问答反馈请求：点赞/点踩，reason 可选 */
public record FeedbackRequest(
        @NotNull(message = "记录ID不能为空") Long recordId,
        @NotBlank(message = "反馈类型不能为空") String type,
        @Size(max = 500, message = "反馈说明不能超过500字") String reason) {
}
