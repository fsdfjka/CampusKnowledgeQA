package com.campusknowledgeqa.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusknowledgeqa.annotation.OperationLog;
import com.campusknowledgeqa.common.Result;
import com.campusknowledgeqa.dto.ChangePasswordRequest;
import com.campusknowledgeqa.dto.LoginRequest;
import com.campusknowledgeqa.dto.LoginResponse;
import com.campusknowledgeqa.dto.ProfileUpdateRequest;
import com.campusknowledgeqa.dto.RegisterRequest;
import com.campusknowledgeqa.entity.SysUser;
import com.campusknowledgeqa.mapper.SysUserMapper;
import com.campusknowledgeqa.security.JwtUtil;
import com.campusknowledgeqa.service.PermissionService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final PermissionService permissionService;

    public AuthController(SysUserMapper userMapper, PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                          PermissionService permissionService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.permissionService = permissionService;
    }

    /** 学生注册 */
    @PostMapping("/register")
    @OperationLog(module = "登录", action = "注册")
    public Result<String> register(@Valid @RequestBody RegisterRequest request) {
        if (isBlank(request.username()) || isBlank(request.password())) {
            return Result.fail(4001, "用户名和密码不能为空");
        }
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.username()));
        if (count != null && count > 0) {
            return Result.fail(4002, "用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setNickname(isBlank(request.nickname()) ? request.username() : request.nickname());
        user.setStudentNo(request.studentNo());
        user.setCollege(request.college());
        user.setRole("STUDENT");
        user.setStatus(1);
        userMapper.insert(user);
        return Result.success("注册成功");
    }

    /** 登录，返回 JWT */
    @PostMapping("/login")
    @OperationLog(module = "登录", action = "登录")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        if (isBlank(request.username()) || isBlank(request.password())) {
            return Result.fail(4001, "用户名和密码不能为空");
        }
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.username()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            return Result.fail(401, "用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            return Result.fail(403, "账号已被禁用，请联系管理员");
        }
        String token = jwtUtil.generate(user);
        return Result.success(new LoginResponse(token, user.getUsername(), user.getNickname(), user.getRole()));
    }

    /** 当前登录用户信息 */
    @GetMapping("/me")
    public Result<Map<String, Object>> me(Authentication authentication) {
        SysUser user = currentUser(authentication);
        if (user == null) {
            return Result.fail(401, "未登录");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        data.put("role", user.getRole());
        data.put("studentNo", user.getStudentNo());
        data.put("college", user.getCollege());
        data.put("major", user.getMajor());
        data.put("email", user.getEmail());
        data.put("phone", user.getPhone());
        data.put("permissions", permissionService.modulesFor(user.getRole()));
        return Result.success(data);
    }

    /** 修改个人资料 */
    @PutMapping("/profile")
    public Result<Map<String, Object>> updateProfile(@Valid @RequestBody ProfileUpdateRequest request,
                                                     Authentication authentication) {
        SysUser user = currentUser(authentication);
        if (user == null) {
            return Result.fail(401, "未登录");
        }
        if (request.nickname() != null && !request.nickname().isBlank()) {
            user.setNickname(request.nickname().trim());
        }
        user.setStudentNo(request.studentNo());
        user.setCollege(request.college());
        user.setMajor(request.major());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        userMapper.updateById(user);
        return me(authentication);
    }

    /** 修改密码 */
    @PostMapping("/change-password")
    public Result<String> changePassword(@Valid @RequestBody ChangePasswordRequest request, Authentication authentication) {
        if (isBlank(request.oldPassword()) || isBlank(request.newPassword())) {
            return Result.fail(4001, "请填写完整");
        }
        SysUser user = currentUser(authentication);
        if (user == null) {
            return Result.fail(401, "未登录");
        }
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            return Result.fail(4002, "原密码错误");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userMapper.updateById(user);
        return Result.success("密码修改成功");
    }

    private SysUser currentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return null;
        }
        return userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, authentication.getName()));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
