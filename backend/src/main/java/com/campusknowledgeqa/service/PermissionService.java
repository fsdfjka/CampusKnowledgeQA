package com.campusknowledgeqa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusknowledgeqa.entity.SysRolePermission;
import com.campusknowledgeqa.mapper.SysRolePermissionMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** RBAC 角色权限：角色 -> 可访问模块 */
@Service
public class PermissionService {

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";

    /** 全部功能模块 */
    public static final List<String> ALL_MODULES = List.of(
            "dashboard", "documents", "qa", "feedback", "stats", "sensitive",
            "users", "roles", "logs", "settings");

    private final SysRolePermissionMapper rolePermissionMapper;

    public PermissionService(SysRolePermissionMapper rolePermissionMapper) {
        this.rolePermissionMapper = rolePermissionMapper;
    }

    public List<String> modulesFor(String role) {
        if (role == null || role.isBlank()) {
            return List.of();
        }
        return rolePermissionMapper.selectList(
                        new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRole, role))
                .stream().map(SysRolePermission::getModule).toList();
    }

    /** 全部角色权限（ADMIN / SUPER_ADMIN） */
    public Map<String, List<String>> all() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put(ROLE_ADMIN, new ArrayList<>(modulesFor(ROLE_ADMIN)));
        map.put(ROLE_SUPER_ADMIN, new ArrayList<>(modulesFor(ROLE_SUPER_ADMIN)));
        return map;
    }

    /** 更新某角色的模块集合 */
    public void update(String role, List<String> modules) {
        if (!ROLE_ADMIN.equals(role) && !ROLE_SUPER_ADMIN.equals(role)) {
            throw new IllegalArgumentException("不支持的角色：" + role);
        }
        rolePermissionMapper.delete(
                new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRole, role));
        if (modules == null) {
            return;
        }
        for (String module : modules) {
            if (!ALL_MODULES.contains(module)) {
                continue;
            }
            SysRolePermission entity = new SysRolePermission();
            entity.setRole(role);
            entity.setModule(module);
            rolePermissionMapper.insert(entity);
        }
    }

    /** 初始化默认权限（仅当表为空时） */
    public void seedDefaults() {
        Long count = rolePermissionMapper.selectCount(null);
        if (count != null && count > 0) {
            return;
        }
        for (String module : List.of("dashboard", "documents", "qa", "feedback", "stats", "sensitive")) {
            insert(ROLE_ADMIN, module);
        }
        for (String module : ALL_MODULES) {
            insert(ROLE_SUPER_ADMIN, module);
        }
        System.out.println("✅ 已初始化默认角色权限");
    }

    private void insert(String role, String module) {
        SysRolePermission entity = new SysRolePermission();
        entity.setRole(role);
        entity.setModule(module);
        rolePermissionMapper.insert(entity);
    }
}
