package com.campusknowledgeqa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusknowledgeqa.entity.QaFeedback;
import com.campusknowledgeqa.entity.QaRecord;
import com.campusknowledgeqa.entity.SysUser;
import com.campusknowledgeqa.mapper.QaFeedbackMapper;
import com.campusknowledgeqa.mapper.QaRecordMapper;
import com.campusknowledgeqa.mapper.SysUserMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 问答记录查询 */
@Service
public class QaRecordService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final QaRecordMapper recordMapper;
    private final SysUserMapper userMapper;
    private final QaFeedbackMapper feedbackMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public QaRecordService(QaRecordMapper recordMapper, SysUserMapper userMapper,
                           QaFeedbackMapper feedbackMapper) {
        this.recordMapper = recordMapper;
        this.userMapper = userMapper;
        this.feedbackMapper = feedbackMapper;
    }

    /**
     * 学生自己的问答历史（分页，支持关键词/仅收藏筛选）
     */
    public Map<String, Object> myRecords(Long userId, String keyword, Boolean favorite, long page, long size) {
        Page<QaRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<QaRecord> wrapper = new LambdaQueryWrapper<QaRecord>()
                .eq(QaRecord::getUserId, userId)
                .like(keyword != null && !keyword.isBlank(), QaRecord::getQuestion, keyword)
                .eq(Boolean.TRUE.equals(favorite), QaRecord::getIsFavorite, 1)
                .orderByDesc(QaRecord::getCreatedAt);

        Page<QaRecord> result = recordMapper.selectPage(pageParam, wrapper);
        List<Map<String, Object>> list = new ArrayList<>();
        for (QaRecord record : result.getRecords()) {
            list.add(toView(record));
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", list);
        data.put("total", result.getTotal());
        data.put("page", page);
        data.put("size", size);
        return data;
    }

    /**
     * 管理员检索问答记录：按用户（ID/用户名/昵称）、关键词、反馈、时间范围过滤，分页
     */
    public Map<String, Object> adminRecords(Long userId, String username, String keyword,
                                            String feedback, String startTime, String endTime,
                                            long page, long size) {
        List<Long> userIds = null;
        if (username != null && !username.isBlank()) {
            userIds = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                            .like(SysUser::getUsername, username.trim())
                            .or()
                            .like(SysUser::getNickname, username.trim()))
                    .stream().map(SysUser::getId).toList();
            if (userIds.isEmpty()) {
                return pageData(new ArrayList<>(), 0L, page, size);
            }
        }

        LocalDateTime start = parseTime(startTime, false);
        LocalDateTime end = parseTime(endTime, true);

        Page<QaRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<QaRecord> wrapper = new LambdaQueryWrapper<QaRecord>()
                .eq(userId != null, QaRecord::getUserId, userId)
                .in(userIds != null, QaRecord::getUserId, userIds)
                .like(keyword != null && !keyword.isBlank(), QaRecord::getQuestion, keyword)
                .eq(feedback != null && !feedback.isBlank(), QaRecord::getFeedback, feedback)
                .ge(start != null, QaRecord::getCreatedAt, start)
                .le(end != null, QaRecord::getCreatedAt, end)
                .orderByDesc(QaRecord::getCreatedAt);

        Page<QaRecord> result = recordMapper.selectPage(pageParam, wrapper);
        Map<Long, String> userNames = userNames();
        List<Map<String, Object>> list = new ArrayList<>();
        for (QaRecord record : result.getRecords()) {
            list.add(toAdminView(record, userNames));
        }
        return pageData(list, result.getTotal(), page, size);
    }

    public QaRecord getById(Long id) {
        return recordMapper.selectById(id);
    }

    /**
     * 热门问题：按时间窗口内提问频次 + 点赞/收藏加权，并做时间衰减，取前 limit 条。
     * 权重 = COUNT + 2×LIKE − 2×DISLIKE + 1×收藏；分数 = 权重 × exp(−0.05 × 距最近提问天数)。
     *
     * @param limit 返回条数（1~50）
     * @param days  统计窗口天数，<=0 表示不限时间
     */
    public List<Map<String, Object>> hotQuestions(int limit, int days) {
        int top = Math.max(1, Math.min(limit, 50));

        QueryWrapper<QaRecord> wrapper = new QueryWrapper<QaRecord>()
                .select("question",
                        "COUNT(*) AS ask_count",
                        "SUM(feedback = 'LIKE') AS likes",
                        "SUM(feedback = 'DISLIKE') AS dislikes",
                        "SUM(is_favorite) AS favorites",
                        "MAX(created_at) AS last_asked")
                .isNotNull("question")
                .groupBy("question")
                .orderByDesc("ask_count")
                .last("LIMIT 200");
        if (days > 0) {
            wrapper.ge("created_at", LocalDateTime.now().minusDays(days));
        }

        List<Map<String, Object>> rows = recordMapper.selectMaps(wrapper);
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> scored = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            long count = toLong(row.get("ask_count"));
            long likes = toLong(row.get("likes"));
            long dislikes = toLong(row.get("dislikes"));
            long favorites = toLong(row.get("favorites"));
            LocalDateTime last = toLocalDateTime(row.get("last_asked"));

            double base = count + 2.0 * likes - 2.0 * dislikes + favorites;
            double ageDays = last == null ? 0 : Math.max(0, ChronoUnit.MINUTES.between(last, now) / 1440.0);
            double score = base * Math.exp(-0.05 * ageDays);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("question", row.get("question"));
            item.put("count", count);
            item.put("likes", likes);
            item.put("dislikes", dislikes);
            item.put("favorites", favorites);
            item.put("hot", Math.round(score * 10000) / 10000.0);
            item.put("lastAskedAt", last == null ? null : FMT.format(last));
            scored.add(item);
        }

        scored.sort(Comparator
                .comparingDouble((Map<String, Object> m) -> ((Number) m.get("hot")).doubleValue()).reversed()
                .thenComparing(m -> (String) m.get("lastAskedAt"), Comparator.nullsLast(Comparator.reverseOrder())));

        return scored.size() > top ? new ArrayList<>(scored.subList(0, top)) : scored;
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        return value instanceof Number number ? number.longValue() : Long.parseLong(value.toString());
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime ldt) {
            return ldt;
        }
        if (value instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime();
        }
        if (value instanceof java.util.Date date) {
            return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        }
        return null;
    }

    /** 有问答记录的用户列表（供后台筛选下拉，按昵称排序） */
    public List<Map<String, Object>> recordUsers() {
        List<QaRecord> records = recordMapper.selectList(new LambdaQueryWrapper<QaRecord>()
                .select(QaRecord::getUserId)
                .isNotNull(QaRecord::getUserId)
                .groupBy(QaRecord::getUserId));
        List<Long> ids = records.stream().map(QaRecord::getUserId).distinct().toList();
        List<Map<String, Object>> result = new ArrayList<>();
        if (ids.isEmpty()) {
            return result;
        }
        List<SysUser> users = userMapper.selectBatchIds(ids);
        users.sort(Comparator.comparing(
                u -> u.getNickname() != null ? u.getNickname() : u.getUsername()));
        for (SysUser user : users) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", user.getId());
            item.put("name", user.getNickname() != null ? user.getNickname() : user.getUsername());
            item.put("username", user.getUsername());
            result.add(item);
        }
        return result;
    }

    /**
     * 设置收藏状态：favorite 为空时按当前状态取反（切换）。
     * 返回最新状态。
     */
    public Map<String, Object> setFavorite(QaRecord record, Boolean favorite) {
        boolean current = record.getIsFavorite() != null && record.getIsFavorite() == 1;
        boolean target = favorite != null ? favorite : !current;
        record.setIsFavorite(target ? 1 : 0);
        recordMapper.updateById(record);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", record.getId());
        data.put("favorite", target);
        return data;
    }

    /**
     * 提交问答反馈：一人一记录仅一条（改判则更新），并同步 qa_record 冗余字段。
     * 仅记录本人可反馈。
     */
    @Transactional
    public Map<String, Object> submitFeedback(Long userId, Long recordId, String type, String reason) {
        QaRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new IllegalArgumentException("问答记录不存在");
        }
        if (!Objects.equals(record.getUserId(), userId)) {
            throw new AccessDeniedException("无权反馈该记录");
        }

        String feedbackType = type == null ? "" : type.trim().toUpperCase();
        if (!"LIKE".equals(feedbackType) && !"DISLIKE".equals(feedbackType) && !"NONE".equals(feedbackType)) {
            throw new IllegalArgumentException("反馈类型仅支持 LIKE / DISLIKE / NONE");
        }
        String feedbackReason = reason == null ? null : reason.trim();
        if (feedbackReason != null && feedbackReason.isEmpty()) {
            feedbackReason = null;
        }

        // 取消反馈：删除明细并清空问答记录上的冗余字段
        if ("NONE".equals(feedbackType)) {
            feedbackMapper.delete(new LambdaQueryWrapper<QaFeedback>()
                    .eq(QaFeedback::getRecordId, recordId)
                    .eq(QaFeedback::getUserId, userId));
            record.setFeedback("NONE");
            record.setFeedbackReason(null);
            recordMapper.updateById(record);
            return feedbackResult(recordId, "NONE", null);
        }

        QaFeedback existing = feedbackMapper.selectOne(new LambdaQueryWrapper<QaFeedback>()
                .eq(QaFeedback::getRecordId, recordId)
                .eq(QaFeedback::getUserId, userId)
                .last("LIMIT 1"));
        if (existing == null) {
            QaFeedback feedback = new QaFeedback();
            feedback.setRecordId(recordId);
            feedback.setUserId(userId);
            feedback.setType(feedbackType);
            feedback.setReason(feedbackReason);
            feedback.setHandled(0);
            feedbackMapper.insert(feedback);
        } else {
            existing.setType(feedbackType);
            existing.setReason(feedbackReason);
            existing.setHandled(0);
            feedbackMapper.updateById(existing);
        }

        record.setFeedback(feedbackType);
        record.setFeedbackReason(feedbackReason);
        recordMapper.updateById(record);

        return feedbackResult(recordId, feedbackType, feedbackReason);
    }

    private Map<String, Object> feedbackResult(Long recordId, String feedback, String reason) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("recordId", recordId);
        data.put("feedback", feedback);
        data.put("feedbackReason", reason);
        return data;
    }

    /** 详情视图（含用户/会话/反馈原因等） */
    public Map<String, Object> toDetail(QaRecord record) {
        Map<String, Object> view = toView(record);
        view.put("userId", record.getUserId());
        view.put("conversationId", record.getConversationId());
        view.put("feedbackReason", record.getFeedbackReason());
        view.put("sensitiveHit", record.getSensitiveHit());
        return view;
    }

    private Map<String, Object> toView(QaRecord record) {        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", record.getId());
        view.put("question", record.getQuestion());
        view.put("answer", record.getAnswer());
        view.put("sources", parseSources(record.getSources()));
        view.put("feedback", record.getFeedback());
        view.put("favorite", record.getIsFavorite() != null && record.getIsFavorite() == 1);
        view.put("createdAt", record.getCreatedAt() == null ? null : FMT.format(record.getCreatedAt()));
        return view;
    }

    private Map<String, Object> toAdminView(QaRecord record, Map<Long, String> userNames) {
        Map<String, Object> view = toView(record);
        view.put("userId", record.getUserId());
        view.put("username", record.getUserId() == null ? null : userNames.get(record.getUserId()));
        view.put("conversationId", record.getConversationId());
        view.put("sensitiveHit", record.getSensitiveHit());
        return view;
    }

    private Map<String, Object> pageData(List<Map<String, Object>> list, long total, long page, long size) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", list);
        data.put("total", total);
        data.put("page", page);
        data.put("size", size);
        return data;
    }

    private Map<Long, String> userNames() {
        Map<Long, String> map = new HashMap<>();
        for (SysUser user : userMapper.selectList(null)) {
            map.put(user.getId(), user.getNickname() != null ? user.getNickname() : user.getUsername());
        }
        return map;
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

    private List<String> parseSources(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
