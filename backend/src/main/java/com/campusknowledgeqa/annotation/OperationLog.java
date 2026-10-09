package com.campusknowledgeqa.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 标记需要记录操作日志的管理接口（由 OperationLogAspect 写入 sys_operation_log） */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /** 模块名，如「用户管理」 */
    String module();

    /** 操作动作，如「重置密码」 */
    String action();
}
