package com.campusknowledgeqa.dto;

/** 反馈处理状态请求：handled 为空时默认标记为已处理 */
public record HandleFeedbackRequest(Boolean handled) {
}
