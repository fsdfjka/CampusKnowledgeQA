package com.campusknowledgeqa.service;

import com.campusknowledgeqa.config.KnowledgeVectorStore;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 混合检索：向量检索（语义）+ 关键词检索（字面），用 RRF（Reciprocal Rank Fusion）融合排序。
 */
@Service
public class HybridRetriever {

    /** RRF 平滑常数，经典取值 60 */
    private static final int RRF_K = 60;

    private final KnowledgeVectorStore vectorStore;

    public HybridRetriever(KnowledgeVectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * @param query 用户问题
     * @param topK  最终返回条数
     */
    public List<Document> retrieve(String query, int topK) {
        if (query == null || query.isBlank() || topK <= 0) {
            return List.of();
        }
        int candidateK = Math.max(topK * 3, 10);

        // 1) 向量检索（不设阈值，取候选）
        List<Document> vectorHits = vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(candidateK).build());

        // 2) 关键词检索
        List<Document> keywordHits = keywordSearch(vectorStore.getAllDocuments(), query, candidateK);

        // 3) RRF 融合
        Map<String, Document> docMap = new LinkedHashMap<>();
        Map<String, Double> scores = new HashMap<>();
        addRanked(vectorHits, scores, docMap);
        addRanked(keywordHits, scores, docMap);

        return scores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topK)
                .map(e -> docMap.get(e.getKey()))
                .filter(Objects::nonNull)
                .toList();
    }

    private void addRanked(List<Document> docs, Map<String, Double> scores, Map<String, Document> docMap) {
        for (int i = 0; i < docs.size(); i++) {
            Document doc = docs.get(i);
            docMap.putIfAbsent(doc.getId(), doc);
            scores.merge(doc.getId(), 1.0 / (RRF_K + i + 1), Double::sum);
        }
    }

    private List<Document> keywordSearch(List<Document> docs, String query, int topK) {
        List<String> terms = tokenize(query);
        if (terms.isEmpty()) {
            return List.of();
        }
        return docs.stream()
                .map(doc -> Map.entry(doc, keywordScore(doc.getText(), terms)))
                .filter(e -> e.getValue() > 0)
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topK)
                .map(Map.Entry::getKey)
                .toList();
    }

    /** 关键词得分：词频 * 词长（长词权重更高） */
    private double keywordScore(String text, List<String> terms) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        String lower = text.toLowerCase();
        double score = 0;
        for (String term : terms) {
            int index = 0;
            int count = 0;
            while ((index = lower.indexOf(term, index)) >= 0) {
                count++;
                index += term.length();
            }
            if (count > 0) {
                score += (double) count * term.length();
            }
        }
        return score;
    }

    /** 简单分词：中文取 2-gram（单字兜底），英文/数字按非字母数字切分 */
    private List<String> tokenize(String query) {
        List<String> terms = new ArrayList<>();
        StringBuilder cjk = new StringBuilder();
        for (char c : query.toCharArray()) {
            if (isCjk(c)) {
                cjk.append(c);
            } else {
                flushCjk(cjk, terms);
            }
        }
        flushCjk(cjk, terms);

        for (String word : query.split("[^\\p{IsAlphabetic}\\p{IsDigit}]+")) {
            String term = word.toLowerCase().trim();
            if (term.length() >= 2) {
                terms.add(term);
            }
        }
        return terms;
    }

    private void flushCjk(StringBuilder cjk, List<String> terms) {
        String segment = cjk.toString();
        cjk.setLength(0);
        if (segment.isEmpty()) {
            return;
        }
        if (segment.length() == 1) {
            terms.add(segment);
            return;
        }
        for (int i = 0; i + 2 <= segment.length(); i++) {
            terms.add(segment.substring(i, i + 2));
        }
    }

    private boolean isCjk(char c) {
        return c >= 0x4E00 && c <= 0x9FFF;
    }
}
