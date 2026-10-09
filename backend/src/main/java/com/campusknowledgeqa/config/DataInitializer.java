package com.campusknowledgeqa.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusknowledgeqa.entity.KbCategory;
import com.campusknowledgeqa.entity.SysUser;
import com.campusknowledgeqa.mapper.KbCategoryMapper;
import com.campusknowledgeqa.mapper.SysUserMapper;
import com.campusknowledgeqa.service.PermissionService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** 启动时初始化默认账号、分类与权限（不存在才创建） */
@Component
public class DataInitializer implements CommandLineRunner {

    private final SysUserMapper userMapper;
    private final KbCategoryMapper categoryMapper;
    private final PasswordEncoder passwordEncoder;
    private final PermissionService permissionService;

    public DataInitializer(SysUserMapper userMapper, KbCategoryMapper categoryMapper,
                           PasswordEncoder passwordEncoder, PermissionService permissionService) {
        this.userMapper = userMapper;
        this.categoryMapper = categoryMapper;
        this.passwordEncoder = passwordEncoder;
        this.permissionService = permissionService;
    }

    @Override
    public void run(String... args) {
        seedUser("admin", "admin123", "管理员", "ADMIN");
        seedUser("superadmin", "super123", "超级管理员", "SUPER_ADMIN");
        seedUser("student", "123456", "张三", "STUDENT");
        seedCategories();
        permissionService.seedDefaults();
    }

    private void seedUser(String username, String password, String nickname, String role) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (count != null && count > 0) {
            return;
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname);
        user.setRole(role);
        user.setStatus(1);
        userMapper.insert(user);
        System.out.println("✅ 已初始化账号：" + username + " / " + password + "（" + role + "）");
    }

    private void seedCategories() {
        Long count = categoryMapper.selectCount(null);
        if (count != null && count > 0) {
            return;
        }
        seedCategory("入学指南", "新生报到、注册、缴费等流程", 1);
        seedCategory("教务教学", "选课、考试、成绩、学籍管理", 2);
        seedCategory("奖助学金", "奖学金、助学金、贷款政策", 3);
        seedCategory("校园生活", "宿舍、食堂、社团、校园卡", 4);
        seedCategory("就业创业", "实习、招聘、就业指导", 5);
        System.out.println("✅ 已初始化默认分类");
    }

    private void seedCategory(String name, String description, int sort) {
        KbCategory category = new KbCategory();
        category.setName(name);
        category.setDescription(description);
        category.setSort(sort);
        categoryMapper.insert(category);
    }
}
