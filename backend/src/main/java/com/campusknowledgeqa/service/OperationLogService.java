package com.campusknowledgeqa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusknowledgeqa.entity.SysOperationLog;
import com.campusknowledgeqa.mapper.SysOperationLogMapper;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 操作日志查询 */
@Service
public class OperationLogService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SysOperationLogMapper logMapper;

    public OperationLogService(SysOperationLogMapper logMapper) {
        this.logMapper = logMapper;
    }

    /** 分页查询操作日志（模块/操作人/关键词筛选） */
    public Map<String, Object> list(String module, String username, String keyword, long page, long size) {
        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<SysOperationLog>()
                .eq(module != null && !module.isBlank(), SysOperationLog::getModule, module)
                .like(username != null && !username.isBlank(), SysOperationLog::getUsername, username)
                .like(keyword != null && !keyword.isBlank(), SysOperationLog::getDetail, keyword)
                .orderByDesc(SysOperationLog::getId);

        Page<SysOperationLog> result = logMapper.selectPage(new Page<>(page, size), wrapper);
        List<Map<String, Object>> list = new ArrayList<>();
        for (SysOperationLog log : result.getRecords()) {
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("id", log.getId());
            view.put("username", log.getUsername());
            view.put("module", log.getModule());
            view.put("action", log.getAction());
            view.put("detail", log.getDetail());
            view.put("ip", log.getIp());
            view.put("status", log.getStatus());
            view.put("errorMsg", log.getErrorMsg());
            view.put("createdAt", log.getCreatedAt() == null ? null : FMT.format(log.getCreatedAt()));
            list.add(view);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", list);
        data.put("total", result.getTotal());
        data.put("page", page);
        data.put("size", size);
        return data;
    }
}
