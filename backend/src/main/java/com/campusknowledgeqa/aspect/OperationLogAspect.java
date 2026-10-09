package com.campusknowledgeqa.aspect;

import com.campusknowledgeqa.annotation.OperationLog;
import com.campusknowledgeqa.common.Result;
import com.campusknowledgeqa.entity.SysOperationLog;
import com.campusknowledgeqa.mapper.SysOperationLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 操作日志切面：拦截带 @OperationLog 的方法，记录成功/失败到 sys_operation_log。
 * - 抛异常或返回 Result.code != 200 视为失败
 * - 未登录（如登录接口）时，尝试从请求参数取 username
 */
@Aspect
@Component
public class OperationLogAspect {

    private static final int DETAIL_MAX = 1000;
    private static final int ERROR_MAX = 500;
    /** 脱敏：请求参数中的密码字段 */
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("(\"[^\"]*[Pp]assword\"\\s*:\\s*\")[^\"]*(\")");

    private final SysOperationLogMapper logMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OperationLogAspect(SysOperationLogMapper logMapper) {
        this.logMapper = logMapper;
    }

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable t) {
            safeLog(joinPoint, operationLog, false, t.getMessage());
            throw t;
        }

        boolean success = true;
        String errorMsg = null;
        if (result instanceof Result<?> r) {
            success = r.getCode() != null && r.getCode() == 200;
            if (!success) {
                errorMsg = r.getMsg();
            }
        }
        safeLog(joinPoint, operationLog, success, errorMsg);
        return result;
    }

    private void safeLog(ProceedingJoinPoint joinPoint, OperationLog operationLog,
                         boolean success, String errorMsg) {
        try {
            Map<String, Object> params = collectParams(joinPoint);

            SysOperationLog log = new SysOperationLog();
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                log.setUsername(authentication.getName());
                Object details = authentication.getDetails();
                if (details instanceof Number number) {
                    log.setUserId(number.longValue());
                }
            }
            if (log.getUsername() == null && params.get("username") instanceof String name) {
                log.setUsername(name);
            }

            log.setModule(operationLog.module());
            log.setAction(operationLog.action());
            log.setIp(currentIp());
            log.setDetail(toJson(params));
            log.setStatus(success ? 1 : 0);
            log.setErrorMsg(truncate(errorMsg, ERROR_MAX));
            logMapper.insert(log);
        } catch (Exception e) {
            System.err.println("⚠️ 操作日志记录失败：" + e.getMessage());
        }
    }

    /** 收集方法参数（跳过 Servlet 对象、文件、认证信息） */
    private Map<String, Object> collectParams(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] names = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        Map<String, Object> params = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg == null || arg instanceof HttpServletRequest || arg instanceof HttpServletResponse
                    || arg instanceof MultipartFile || arg instanceof Authentication) {
                continue;
            }
            String name = (names != null && i < names.length) ? names[i] : ("arg" + i);
            params.put(name, arg);
        }
        return params;
    }

    private String toJson(Map<String, Object> params) {
        try {
            String json = objectMapper.writeValueAsString(params);
            json = PASSWORD_PATTERN.matcher(json).replaceAll("$1***$2");
            return truncate(json, DETAIL_MAX);
        } catch (Exception e) {
            return truncate(String.valueOf(params), DETAIL_MAX);
        }
    }

    private String currentIp() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return null;
            }
            HttpServletRequest request = attributes.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                int comma = forwarded.indexOf(',');
                return comma > 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() > max ? value.substring(0, max) : value;
    }
}
