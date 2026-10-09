package com.campusknowledgeqa.controller;

import com.campusknowledgeqa.annotation.OperationLog;
import com.campusknowledgeqa.common.Result;
import com.campusknowledgeqa.dto.HandleFeedbackRequest;
import com.campusknowledgeqa.service.FeedbackService;
import com.campusknowledgeqa.service.OperationLogService;
import com.campusknowledgeqa.service.PermissionService;
import com.campusknowledgeqa.service.QaRecordService;
import com.campusknowledgeqa.service.SensitiveWordService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 后台管理接口（仅管理员及以上） */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final PermissionService permissionService;
    private final QaRecordService qaRecordService;
    private final FeedbackService feedbackService;
    private final SensitiveWordService sensitiveWordService;
    private final OperationLogService operationLogService;

    public AdminController(PermissionService permissionService, QaRecordService qaRecordService,
                           FeedbackService feedbackService, SensitiveWordService sensitiveWordService,
                           OperationLogService operationLogService) {
        this.permissionService = permissionService;
        this.qaRecordService = qaRecordService;
        this.feedbackService = feedbackService;
        this.sensitiveWordService = sensitiveWordService;
        this.operationLogService = operationLogService;
    }

    /** 查看角色权限（仅超级管理员） */
    @GetMapping("/permissions")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<Map<String, List<String>>> permissions() {
        return Result.success(permissionService.all());
    }

    /** 修改角色权限（仅超级管理员） */
    @PutMapping("/permissions")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @OperationLog(module = "角色权限", action = "修改")
    public Result<String> updatePermissions(@RequestBody Map<String, List<String>> body) {
        if (body.containsKey("ADMIN")) {
            permissionService.update("ADMIN", body.get("ADMIN"));
        }
        if (body.containsKey("SUPER_ADMIN")) {
            permissionService.update("SUPER_ADMIN", body.get("SUPER_ADMIN"));
        }
        return Result.success("✅权限已保存");
    }

    /** 管理员检索问答记录（用户/关键词/反馈/时间范围，分页） */
    @GetMapping("/qa/records")
    public Result<Map<String, Object>> qaRecords(@RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) Long userId,
                                                 @RequestParam(required = false) String username,
                                                 @RequestParam(required = false) String feedback,
                                                 @RequestParam(required = false) String startTime,
                                                 @RequestParam(required = false) String endTime,
                                                 @RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "10") long size) {
        return Result.success(qaRecordService.adminRecords(
                userId, username, keyword, feedback, startTime, endTime, page, size));
    }

    /** 有问答记录的用户（后台筛选下拉） */
    @GetMapping("/qa/record-users")
    public Result<List<Map<String, Object>>> recordUsers() {
        return Result.success(qaRecordService.recordUsers());
    }

    /** 反馈列表（类型/状态/关键词/时间范围，分页） */
    @GetMapping("/feedback")
    public Result<Map<String, Object>> feedbackList(@RequestParam(required = false) String type,
                                                    @RequestParam(required = false) Integer handled,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) String startTime,
                                                    @RequestParam(required = false) String endTime,
                                                    @RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "10") long size) {
        return Result.success(feedbackService.list(type, handled, keyword, startTime, endTime, page, size));
    }

    /** 设置反馈处理状态：handled=true 已处理，false 取消处理（默认 true） */
    @PutMapping("/feedback/{id}/handle")
    @OperationLog(module = "反馈管理", action = "处理反馈")
    public Result<String> handleFeedback(@PathVariable Long id,
                                         @RequestBody(required = false) HandleFeedbackRequest request) {
        boolean handled = request == null || request.handled() == null || request.handled();
        boolean ok = feedbackService.handle(id, handled);
        return ok ? Result.success(handled ? "已处理" : "已取消处理") : Result.fail(4004, "反馈不存在");
    }

    /** 敏感词列表 */
    @GetMapping("/sensitive-words")
    public Result<List<Map<String, Object>>> sensitiveWords(@RequestParam(required = false) String keyword) {
        return Result.success(sensitiveWordService.list(keyword));
    }

    /** 新增敏感词 */
    @PostMapping("/sensitive-words")
    @OperationLog(module = "敏感词管理", action = "新增")
    public Result<Map<String, Object>> addSensitiveWord(@RequestBody Map<String, String> body) {
        return Result.success(sensitiveWordService.add(body.get("word")));
    }

    /** 删除敏感词 */
    @DeleteMapping("/sensitive-words/{id}")
    @OperationLog(module = "敏感词管理", action = "删除")
    public Result<String> deleteSensitiveWord(@PathVariable Long id) {
        return sensitiveWordService.delete(id) ? Result.success("已删除") : Result.fail(4004, "敏感词不存在");
    }

    /** 操作日志分页查询 */
    @GetMapping("/logs")
    public Result<Map<String, Object>> logs(@RequestParam(required = false) String module,
                                            @RequestParam(required = false) String username,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "10") long size) {
        return Result.success(operationLogService.list(module, username, keyword, page, size));
    }
}
