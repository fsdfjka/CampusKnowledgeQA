package com.campusknowledgeqa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusknowledgeqa.dto.CreateUserRequest;
import com.campusknowledgeqa.entity.SysUser;
import com.campusknowledgeqa.mapper.SysUserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 后台用户管理（仅超级管理员） */
@Service
public class UserService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Set<String> ROLES = Set.of("STUDENT", "ADMIN", "SUPER_ADMIN");
    private static final String PWD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(SysUserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /** 用户分页列表（角色/状态/关键词筛选） */
    public Map<String, Object> list(String role, Integer status, String keyword, long page, long size) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(role != null && !role.isBlank(), SysUser::getRole, role)
                .eq(status != null, SysUser::getStatus, status)
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(SysUser::getUsername, keyword)
                        .or()
                        .like(SysUser::getNickname, keyword))
                .orderByDesc(SysUser::getId);

        Page<SysUser> result = userMapper.selectPage(new Page<>(page, size), wrapper);
        List<Map<String, Object>> list = new ArrayList<>();
        for (SysUser user : result.getRecords()) {
            list.add(toView(user));
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", list);
        data.put("total", result.getTotal());
        data.put("page", page);
        data.put("size", size);
        return data;
    }

    @Transactional
    public Map<String, Object> create(CreateUserRequest request) {
        String username = request.username().trim();
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (exists != null && exists > 0) {
            throw new IllegalArgumentException("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setNickname(blank(request.nickname()) ? username : request.nickname().trim());
        user.setRole(normalizeRole(request.role()));
        user.setStudentNo(request.studentNo());
        user.setCollege(request.college());
        user.setMajor(request.major());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setStatus(1);
        userMapper.insert(user);
        return toView(userMapper.selectById(user.getId()));
    }

    @Transactional
    public boolean setStatus(Long id, Integer status) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        user.setStatus(status != null && status == 1 ? 1 : 0);
        userMapper.updateById(user);
        return true;
    }

    /** 重置为随机密码并返回明文（仅此一次展示） */
    @Transactional
    public Map<String, Object> resetPassword(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        String plain = randomPassword(8);
        user.setPassword(passwordEncoder.encode(plain));
        userMapper.updateById(user);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("password", plain);
        return data;
    }

    private Map<String, Object> toView(SysUser user) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", user.getId());
        view.put("username", user.getUsername());
        view.put("nickname", user.getNickname());
        view.put("role", user.getRole());
        view.put("studentNo", user.getStudentNo());
        view.put("college", user.getCollege());
        view.put("major", user.getMajor());
        view.put("email", user.getEmail());
        view.put("phone", user.getPhone());
        view.put("status", user.getStatus());
        view.put("createdAt", user.getCreatedAt() == null ? null : FMT.format(user.getCreatedAt()));
        return view;
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "STUDENT";
        }
        String value = role.trim().toUpperCase();
        return ROLES.contains(value) ? value : "STUDENT";
    }

    private String randomPassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(PWD_CHARS.charAt(RANDOM.nextInt(PWD_CHARS.length())));
        }
        return sb.toString();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
