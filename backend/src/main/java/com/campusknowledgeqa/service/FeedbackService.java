package com.campusknowledgeqa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusknowledgeqa.entity.QaFeedback;
import com.campusknowledgeqa.entity.QaRecord;
import com.campusknowledgeqa.entity.SysUser;
import com.campusknowledgeqa.mapper.QaFeedbackMapper;
import com.campusknowledgeqa.mapper.QaRecordMapper;
import com.campusknowledgeqa.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** 后台反馈管理 */
@Service
public class FeedbackService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final QaFeedbackMapper feedbackMapper;
    private final QaRecordMapper recordMapper;
    private final SysUserMapper userMapper;

    public FeedbackService(QaFeedbackMapper feedbackMapper, QaRecordMapper recordMapper,
                           SysUserMapper userMapper) {
        this.feedbackMapper = feedbackMapper;
        this.recordMapper = recordMapper;
        this.userMapper = userMapper;
    }

    /**
     * 反馈列表：按类型/处理状态/关键词/时间范围筛选，分页（未处理优先）。
     */
    public Map<String, Object> list(String type, Integer handled, String keyword,
                                    String startTime, String endTime, long page, long size) {
        LocalDateTime start = parseTime(startTime, false);
        LocalDateTime end = parseTime(endTime, true);

        LambdaQueryWrapper<QaFeedback> wrapper = new LambdaQueryWrapper<QaFeedback>()
                .eq(type != null && !type.isBlank(), QaFeedback::getType, type)
                .eq(handled != null, QaFeedback::getHandled, handled)
                .ge(start != null, QaFeedback::getCreatedAt, start)
                .le(end != null, QaFeedback::getCreatedAt, end);

        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            List<Long> recordIds = recordMapper.selectList(new LambdaQueryWrapper<QaRecord>()
                            .select(QaRecord::getId)
                            .like(QaRecord::getQuestion, kw))
                    .stream().map(QaRecord::getId).toList();
            List<Long> userIds = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                            .like(SysUser::getUsername, kw)
                            .or()
                            .like(SysUser::getNickname, kw))
                    .stream().map(SysUser::getId).toList();
            wrapper.and(w -> {
                w.like(QaFeedback::getReason, kw);
                if (!recordIds.isEmpty()) {
                    w.or().in(QaFeedback::getRecordId, recordIds);
                }
                if (!userIds.isEmpty()) {
                    w.or().in(QaFeedback::getUserId, userIds);
                }
            });
        }

        wrapper.orderByAsc(QaFeedback::getHandled).orderByDesc(QaFeedback::getCreatedAt);
        Page<QaFeedback> result = feedbackMapper.selectPage(new Page<>(page, size), wrapper);

        List<QaFeedback> rows = result.getRecords();
        Set<Long> recordIds = rows.stream().map(QaFeedback::getRecordId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> userIds = rows.stream().map(QaFeedback::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, String> questions = new HashMap<>();
        if (!recordIds.isEmpty()) {
            for (QaRecord record : recordMapper.selectBatchIds(recordIds)) {
                questions.put(record.getId(), record.getQuestion());
            }
        }
        Map<Long, String> usernames = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (SysUser user : userMapper.selectBatchIds(userIds)) {
                usernames.put(user.getId(), user.getNickname() != null ? user.getNickname() : user.getUsername());
            }
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (QaFeedback fb : rows) {
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("id", fb.getId());
            view.put("recordId", fb.getRecordId());
            view.put("userId", fb.getUserId());
            view.put("username", fb.getUserId() == null ? null : usernames.get(fb.getUserId()));
            view.put("type", fb.getType());
            view.put("reason", fb.getReason());
            view.put("handled", fb.getHandled() != null && fb.getHandled() == 1);
            view.put("question", fb.getRecordId() == null ? null : questions.get(fb.getRecordId()));
            view.put("createdAt", fb.getCreatedAt() == null ? null : FMT.format(fb.getCreatedAt()));
            list.add(view);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", list);
        data.put("total", result.getTotal());
        data.put("page", page);
        data.put("size", size);
        return data;
    }

    /** 标记反馈为已处理 */
    @Transactional
    public boolean handle(Long id) {
        QaFeedback feedback = feedbackMapper.selectById(id);
        if (feedback == null) {
            return false;
        }
        feedback.setHandled(1);
        feedbackMapper.updateById(feedback);
        return true;
    }

    private LocalDateTime parseTime(String value, boolean endOfDay) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim();
        try {
            if (v.length() <= 10) {
                LocalDate date = LocalDate.parse(v);
                return endOfDay ? date.atTime(23, 59, 59) : date.atStartOfDay();
            }
            return LocalDateTime.parse(v.replace('T', ' '), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            return null;
        }
    }
}
