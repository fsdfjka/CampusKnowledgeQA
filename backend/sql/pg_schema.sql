-- =============================================================
-- 校园知识库问答系统 · PostgreSQL 向量库建表脚本
-- 数据库：PostgreSQL 17 + pgvector   库名：campus_kb
-- 说明：PostgreSQL 只存文本向量；业务数据在 MySQL（见 mysql_schema.sql）
--       本文件仅 DDL，请自行执行
-- =============================================================

-- 启用 pgvector 扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 向量表（Spring AI PgVectorStore 也会自动创建，此处为等价 DDL）
CREATE TABLE IF NOT EXISTS vector_store (
    id        UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    content   TEXT,
    metadata  JSONB,
    embedding VECTOR(1024)                                  -- 维度需与 Embedding 模型一致
);
COMMENT ON TABLE vector_store IS '向量存储（pgvector）';

-- HNSW 索引（余弦距离）
CREATE INDEX IF NOT EXISTS vector_store_embedding_idx
    ON vector_store USING hnsw (embedding vector_cosine_ops);
