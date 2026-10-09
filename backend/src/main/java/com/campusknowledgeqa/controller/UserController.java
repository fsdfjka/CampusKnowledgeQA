package com.campusknowledgeqa.controller;

import com.campusknowledgeqa.annotation.OperationLog;
import com.campusknowledgeqa.common.Result;
import com.campusknowledgeqa.dto.CreateUserRequest;
import com.campusknowledgeqa.dto.UserStatusRequest;
import com.campusknowledgeqa.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 后台用户管理（仅超级管理员） */
@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** 用户列表分页查询 */
    @GetMapping
    public Result<Map<String, Object>> list(@RequestParam(required = false) String role,
                                            @RequestParam(required = false) Integer status,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "10") long size) {
        return Result.success(userService.list(role, status, keyword, page, size));
    }

    /** 新增用户 */
    @PostMapping
    @OperationLog(module = "用户管理", action = "新增用户")
    public Result<Map<String, Object>> create(@Valid @RequestBody CreateUserRequest request) {
        return Result.success(userService.create(request));
    }

    /** 启用 / 禁用账号 */
    @PutMapping("/{id}/status")
    @OperationLog(module = "用户管理", action = "修改状态")
    public Result<String> status(@PathVariable Long id, @RequestBody UserStatusRequest request) {
        userService.setStatus(id, request == null ? null : request.status());
        return Result.success("操作成功");
    }

    /** 重置用户密码（返回随机密码） */
    @PutMapping("/{id}/reset-password")
    @OperationLog(module = "用户管理", action = "重置密码")
    public Result<Map<String, Object>> resetPassword(@PathVariable Long id) {
        return Result.success(userService.resetPassword(id));
    }
}
