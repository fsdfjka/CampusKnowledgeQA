package com.campusknowledgeqa.common;

import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 参数校验失败（@Valid） */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(msg -> msg != null && !msg.isBlank())
                .findFirst()
                .orElse("参数校验失败");
        return Result.fail(4001, message);
    }

    /** 参数/业务校验异常 */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> handleIllegalArgument(IllegalArgumentException e) {
        return Result.fail(4001, e.getMessage());
    }

    /** 方法级鉴权失败（@PreAuthorize） */
    @ExceptionHandler(AccessDeniedException.class)
    public Result<?> handleAccessDenied(AccessDeniedException e) {
        return Result.fail(403, "无权访问该资源");
    }

    /** AI 接口异常（鉴权/欠费等） */
    @ExceptionHandler(NonTransientAiException.class)
    public Result<?> handleAiException(NonTransientAiException e) {
        return aiError(e.getMessage());
    }

    /** 兜底 */
    @ExceptionHandler(Exception.class)
    public Result<?> handleGlobalException(Exception e) {
        if (isArrearage(e.getMessage())) {
            return Result.fail(5003, "大模型服务余额不足（账号欠费），请充值后再试");
        }
        e.printStackTrace();
        return Result.fail(5000, "服务器内部错误：" + e.getMessage());
    }

    private Result<?> aiError(String message) {
        String msg = message == null ? "" : message;
        if (msg.contains("InvalidApiKey")) {
            return Result.fail(5001, "百炼API密钥无效，请检查APIKEY配置");
        }
        if (isArrearage(msg)) {
            return Result.fail(5003, "大模型服务余额不足（账号欠费），请充值后再试");
        }
        return Result.fail(5002, "大模型接口调用失败：" + msg);
    }

    private boolean isArrearage(String message) {
        if (message == null) {
            return false;
        }
        return message.contains("Arrearage")
                || message.contains("overdue-payment")
                || message.contains("good standing");
    }
}
