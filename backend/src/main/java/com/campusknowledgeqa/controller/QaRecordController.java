package com.campusknowledgeqa.controller;

import com.campusknowledgeqa.common.Result;
import com.campusknowledgeqa.dto.FavoriteRequest;
import com.campusknowledgeqa.dto.FeedbackRequest;
import com.campusknowledgeqa.entity.QaRecord;
import com.campusknowledgeqa.service.QaRecordService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
public class QaRecordController {

    private final QaRecordService qaRecordService;

    public QaRecordController(QaRecordService qaRecordService) {
        this.qaRecordService = qaRecordService;
    }

    /** 我的问答历史（学生） */
    @GetMapping("/qa/records/my")
    public Result<Map<String, Object>> myRecords(@RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) Boolean favorite,
                                                 @RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "10") long size,
                                                 Authentication authentication) {
        Long userId = currentUserId(authentication);
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        return Result.success(qaRecordService.myRecords(userId, keyword, favorite, page, size));
    }

    /** 单条问答详情：本人或管理员可看 */
    @GetMapping("/qa/records/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id, Authentication authentication) {
        QaRecord record = qaRecordService.getById(id);
        if (record == null) {
            return Result.fail(4004, "记录不存在");
        }
        boolean admin = isAdmin(authentication);
        if (!admin && !Objects.equals(currentUserId(authentication), record.getUserId())) {
            return Result.fail(403, "无权查看该记录");
        }
        return Result.success(qaRecordService.toDetail(record));
    }

    /** 收藏 / 取消收藏：本人或管理员可操作；favorite 为空时按当前状态取反 */
    @PutMapping("/qa/records/{id}/favorite")
    public Result<Map<String, Object>> favorite(@PathVariable Long id,
                                                @RequestBody(required = false) FavoriteRequest request,
                                                Authentication authentication) {
        Long userId = currentUserId(authentication);
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        QaRecord record = qaRecordService.getById(id);
        if (record == null) {
            return Result.fail(4004, "记录不存在");
        }
        if (!isAdmin(authentication) && !Objects.equals(userId, record.getUserId())) {
            return Result.fail(403, "无权操作该记录");
        }
        Boolean favorite = request == null ? null : request.favorite();
        return Result.success(qaRecordService.setFavorite(record, favorite));
    }

    /** 热门问题：近 days 天按热度排序，取前 limit 条 */
    @GetMapping("/qa/hot")
    public Result<List<Map<String, Object>>> hot(@RequestParam(defaultValue = "10") int limit,
                                                 @RequestParam(defaultValue = "30") int days) {
        return Result.success(qaRecordService.hotQuestions(limit, days));
    }

    /** 提交问答反馈：点赞/点踩 + 可选备注 */
    @PostMapping("/qa/feedback")
    public Result<Map<String, Object>> feedback(@Valid @RequestBody FeedbackRequest request,
                                                Authentication authentication) {
        Long userId = currentUserId(authentication);
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        return Result.success(qaRecordService.submitFeedback(
                userId, request.recordId(), request.type(), request.reason()));
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())
                        || "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object details = authentication.getDetails();
        return details instanceof Number number ? number.longValue() : null;
    }
}
