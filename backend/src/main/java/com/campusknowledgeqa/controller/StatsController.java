package com.campusknowledgeqa.controller;

import com.campusknowledgeqa.common.Result;
import com.campusknowledgeqa.service.StatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 后台数据统计（仅管理员及以上，见 SecurityConfig /admin/**） */
@RestController
@RequestMapping("/admin/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    /** 总览：文档总数 / 分类数量 / 总提问量 / 好评率 */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.success(statsService.overview());
    }

    /** 每日提问量（近 days 天） */
    @GetMapping("/daily")
    public Result<Map<String, Object>> daily(@RequestParam(defaultValue = "7") int days) {
        return Result.success(statsService.daily(days));
    }

    /** 热门问题 TOP N */
    @GetMapping("/hot")
    public Result<List<Map<String, Object>>> hot(@RequestParam(defaultValue = "6") int limit,
                                                 @RequestParam(defaultValue = "30") int days) {
        return Result.success(statsService.hot(limit, days));
    }

    /** 反馈分布（好评/差评/未评价） */
    @GetMapping("/feedback")
    public Result<List<Map<String, Object>>> feedback() {
        return Result.success(statsService.feedback());
    }
}
