package com.campusknowledgeqa.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 基于 pgvector 的知识库向量存储封装：
 * - 读写/检索委托给 PgVectorStore
 * - clear() / getAllDocuments() / updateSource() 用 JdbcTemplate 操作向量表
 * - deleteBySource() 走 VectorStore 的 Filter 删除
 */
public class KnowledgeVectorStore implements VectorStore {

    private final VectorStore delegate;
    private final JdbcTemplate jdbcTemplate;
    private final String qualifiedTable;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KnowledgeVectorStore(VectorStore delegate, JdbcTemplate jdbcTemplate,
                                String schema, String table) {
        this.delegate = delegate;
        this.jdbcTemplate = jdbcTemplate;
        this.qualifiedTable = schema + "." + table;
    }

    @Override
    public void add(List<Document> documents) {
        delegate.add(documents);
    }

    @Override
    public void delete(List<String> ids) {
        delegate.delete(ids);
    }

    @Override
    public void delete(Filter.Expression filterExpression) {
        delegate.delete(filterExpression);
    }

    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        return delegate.similaritySearch(request);
    }

    /** 清空向量表 */
    public void clear() {
        jdbcTemplate.execute("TRUNCATE TABLE " + qualifiedTable);
        System.out.println("🧹 向量表已清空：" + qualifiedTable);
    }

    /** 按来源删除向量：删除某文档时同步清理其切片 */
    public void deleteBySource(String source) {
        Filter.Expression expression = new Filter.Expression(
                Filter.ExpressionType.EQ,
                new Filter.Key("source"),
                new Filter.Value(source));
        delegate.delete(expression);
    }

    /** 文档改名时同步更新 metadata.source（metadata 列为 json，需转 jsonb 再转回） */
    public void updateSource(String oldSource, String newSource) {
        jdbcTemplate.update(
                "UPDATE " + qualifiedTable
                        + " SET metadata = jsonb_set(metadata::jsonb, '{source}', to_jsonb(?::text))::json"
                        + " WHERE metadata->>'source' = ?",
                newSource, oldSource);
    }

    /** 读取向量表中的全部文档 */
    public List<Document> getAllDocuments() {
        return jdbcTemplate.query(
                "SELECT id, content, metadata FROM " + qualifiedTable,
                (rs, rowNum) -> new Document(
                        rs.getString("id"),
                        rs.getString("content"),
                        parseMetadata(rs.getString("metadata"))));
    }

    private Map<String, Object> parseMetadata(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }
}
