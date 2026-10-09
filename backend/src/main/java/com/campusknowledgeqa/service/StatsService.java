package com.campusknowledgeqa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campusknowledgeqa.entity.QaRecord;
import com.campusknowledgeqa.mapper.KbCategoryMapper;
import com.campusknowledgeqa.mapper.KbDocumentMapper;
import com.campusknowledgeqa.mapper.QaRecordMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 后台数据统计 */
@Service
public class StatsService {

    private static final DateTimeFormatter MD = DateTimeFormatter.ofPattern("MM-dd");

    private final KbDocumentMapper documentMapper;
    private final KbCategoryMapper categoryMapper;
    private final QaRecordMapper recordMapper;
    private final QaRecordService qaRecordService;

    public StatsService(KbDocumentMapper documentMapper, KbCategoryMapper categoryMapper,
                        QaRecordMapper recordMapper, QaRecordService qaRecordService) {
        this.documentMapper = documentMapper;
        this.categoryMapper = categoryMapper;
        this.recordMapper = recordMapper;
        this.qaRecordService = qaRecordService;
    }

    /** 总览：文档总数 / 分类数量 / 总提问量 / 好评率 */
    public Map<String, Object> overview() {
        long documents = documentMapper.selectCount(null);
        long categories = categoryMapper.selectCount(null);
        long questions = recordMapper.selectCount(null);
        long likes = countByFeedback("LIKE");
        long dislikes = countByFeedback("DISLIKE");
        long rated = likes + dislikes;
        int goodRate = rated == 0 ? 0 : (int) Math.round(likes * 100.0 / rated);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("documents", documents);
        data.put("categories", categories);
        data.put("questions", questions);
        data.put("goodRate", goodRate);
        return data;
    }

    /** 每日提问量（近 days 天，缺失日期补 0） */
    public Map<String, Object> daily(int days) {
        int n = Math.max(1, Math.min(days, 90));
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(n - 1L);

        List<Map<String, Object>> rows = recordMapper.selectMaps(new QueryWrapper<QaRecord>()
                .select("DATE(created_at) AS d", "COUNT(*) AS c")
                .ge("created_at", from.atStartOfDay())
                .groupBy("DATE(created_at)")
                .orderByAsc("d"));

        Map<String, Long> byDate = new HashMap<>();
        for (Map<String, Object> row : rows) {
            byDate.put(toDateString(row.get("d")), ((Number) row.get("c")).longValue());
        }

        List<String> dates = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            LocalDate date = from.plusDays(i);
            dates.add(MD.format(date));
            counts.add(byDate.getOrDefault(date.toString(), 0L));
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("dates", dates);
        data.put("counts", counts);
        return data;
    }

    /** 热门问题 TOP N（复用热度算法） */
    public List<Map<String, Object>> hot(int limit, int days) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> item : qaRecordService.hotQuestions(limit, days)) {
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("name", item.get("question"));
            view.put("value", item.get("count"));
            list.add(view);
        }
        return list;
    }

    /** 反馈分布：好评 / 差评 / 未评价 */
    public List<Map<String, Object>> feedback() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(feedbackItem("好评", countByFeedback("LIKE")));
        list.add(feedbackItem("差评", countByFeedback("DISLIKE")));
        list.add(feedbackItem("未评价", countByFeedback("NONE")));
        return list;
    }

    private Map<String, Object> feedbackItem(String name, long value) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("value", value);
        return item;
    }

    private long countByFeedback(String feedback) {
        Long count = recordMapper.selectCount(new LambdaQueryWrapper<QaRecord>()
                .eq(QaRecord::getFeedback, feedback));
        return count == null ? 0L : count;
    }

    private String toDateString(Object value) {
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate().toString();
        }
        if (value instanceof LocalDate date) {
            return date.toString();
        }
        return value == null ? "" : value.toString();
    }
}
