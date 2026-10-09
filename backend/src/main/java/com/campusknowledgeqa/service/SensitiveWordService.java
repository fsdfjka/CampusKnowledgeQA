package com.campusknowledgeqa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusknowledgeqa.entity.SysSensitiveWord;
import com.campusknowledgeqa.mapper.SysSensitiveWordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 敏感词库 */
@Service
public class SensitiveWordService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    /** 敏感词缓存有效期（毫秒） */
    private static final long CACHE_TTL_MS = 60_000L;

    private final SysSensitiveWordMapper wordMapper;

    private volatile List<String> cache = List.of();
    private volatile long cacheLoadedAt = 0L;

    public SensitiveWordService(SysSensitiveWordMapper wordMapper) {
        this.wordMapper = wordMapper;
    }

    /** 敏感词列表（支持关键词模糊，按添加时间倒序） */
    public List<Map<String, Object>> list(String keyword) {
        List<SysSensitiveWord> words = wordMapper.selectList(new LambdaQueryWrapper<SysSensitiveWord>()
                .like(keyword != null && !keyword.isBlank(), SysSensitiveWord::getWord, keyword)
                .orderByDesc(SysSensitiveWord::getId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysSensitiveWord word : words) {
            result.add(toView(word));
        }
        return result;
    }

    @Transactional
    public Map<String, Object> add(String word) {
        String value = word == null ? "" : word.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("敏感词不能为空");
        }
        if (value.length() > 64) {
            throw new IllegalArgumentException("敏感词长度不能超过 64");
        }
        Long exists = wordMapper.selectCount(new LambdaQueryWrapper<SysSensitiveWord>()
                .eq(SysSensitiveWord::getWord, value));
        if (exists != null && exists > 0) {
            throw new IllegalArgumentException("该敏感词已存在");
        }
        SysSensitiveWord entity = new SysSensitiveWord();
        entity.setWord(value);
        wordMapper.insert(entity);
        invalidateCache();
        return toView(wordMapper.selectById(entity.getId()));
    }

    @Transactional
    public boolean delete(Long id) {
        boolean deleted = wordMapper.deleteById(id) > 0;
        if (deleted) {
            invalidateCache();
        }
        return deleted;
    }

    /** 返回文本命中的敏感词，未命中返回 null */
    public String findHit(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        for (String word : cachedWords()) {
            if (!word.isBlank() && text.contains(word)) {
                return word;
            }
        }
        return null;
    }

    /** 清空缓存（新增/删除后调用），下次访问自动重载 */
    public void invalidateCache() {
        cacheLoadedAt = 0L;
    }

    private List<String> cachedWords() {
        long now = System.currentTimeMillis();
        if (now - cacheLoadedAt > CACHE_TTL_MS) {
            synchronized (this) {
                if (now - cacheLoadedAt > CACHE_TTL_MS) {
                    cache = wordMapper.selectList(null).stream()
                            .map(SysSensitiveWord::getWord)
                            .filter(Objects::nonNull)
                            .toList();
                    cacheLoadedAt = now;
                }
            }
        }
        return cache;
    }

    private Map<String, Object> toView(SysSensitiveWord word) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", word.getId());
        view.put("word", word.getWord());
        view.put("createdAt", word.getCreatedAt() == null ? null : FMT.format(word.getCreatedAt()));
        return view;
    }
}
