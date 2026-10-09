-- =============================================================
-- 校园知识库问答系统 · MySQL 业务库建表脚本
-- 数据库：MySQL 8.x   库名：campus_kb
-- 说明：仅 DDL 脚本，请自行执行（本文件不会被自动执行）
-- 向量表在 PostgreSQL（见 pg_schema.sql）
-- =============================================================

-- -------------------------------------------------------------
-- 1. 用户表（学生 / 管理员，RBAC 基础）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(64)  NOT NULL COMMENT '登录名 / 学号',
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 哈希，禁止明文',
    nickname    VARCHAR(64)  DEFAULT NULL COMMENT '姓名',
    role        VARCHAR(20)  NOT NULL DEFAULT 'STUDENT' COMMENT 'STUDENT / ADMIN',
    student_no  VARCHAR(32)  DEFAULT NULL COMMENT '学号',
    college     VARCHAR(128) DEFAULT NULL COMMENT '学院',
    major       VARCHAR(128) DEFAULT NULL COMMENT '专业',
    email       VARCHAR(128) DEFAULT NULL,
    phone       VARCHAR(32)  DEFAULT NULL,
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1=正常, 0=禁用',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username),
    KEY idx_sys_user_role (role),
    KEY idx_sys_user_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统用户（学生/管理员）';

-- -------------------------------------------------------------
-- 2. 知识库分类
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS kb_category (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(64)  NOT NULL COMMENT '分类名：学籍、奖助、毕业...',
    description VARCHAR(255) DEFAULT NULL COMMENT '描述',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_kb_category_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '知识库分类';

-- -------------------------------------------------------------
-- 3. 知识库文档
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS kb_document (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(255) NOT NULL COMMENT '文档名',
    category_id BIGINT       DEFAULT NULL COMMENT '分类ID',
    file_type   VARCHAR(16)  DEFAULT NULL COMMENT 'PDF / DOCX / DOC / TXT',
    file_size   BIGINT       DEFAULT NULL COMMENT '字节',
    file_path   VARCHAR(500) DEFAULT NULL COMMENT '存储路径',
    chunk_count INT          NOT NULL DEFAULT 0 COMMENT '切片数量',
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PARSING/DONE/FAILED',
    uploader_id BIGINT       DEFAULT NULL COMMENT '上传人ID',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_kb_document_category (category_id),
    KEY idx_kb_document_status (status),
    CONSTRAINT fk_kb_document_category FOREIGN KEY (category_id) REFERENCES kb_category (id) ON DELETE SET NULL,
    CONSTRAINT fk_kb_document_uploader FOREIGN KEY (uploader_id) REFERENCES sys_user (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '知识库文档';

-- -------------------------------------------------------------
-- 4. 问答记录（每一轮问答）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS qa_record (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id         BIGINT       DEFAULT NULL COMMENT '提问用户ID',
    conversation_id BIGINT       DEFAULT NULL COMMENT '所属会话ID',
    question        TEXT         NOT NULL COMMENT '问题',
    answer          TEXT         COMMENT '回答',
    sources         JSON         DEFAULT NULL COMMENT '引用来源: ["a.pdf","b.txt"]',
    feedback        VARCHAR(20)  NOT NULL DEFAULT 'NONE' COMMENT 'NONE/LIKE/DISLIKE',
    feedback_reason VARCHAR(500) DEFAULT NULL COMMENT '点踩原因',
    is_favorite     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否收藏 0/1',
    sensitive_hit   TINYINT      NOT NULL DEFAULT 0 COMMENT '是否命中敏感词 0/1',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_qa_record_user (user_id),
    KEY idx_qa_record_conversation (conversation_id),
    KEY idx_qa_record_created (created_at),
    KEY idx_qa_record_feedback (feedback)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '问答记录';

-- -------------------------------------------------------------
-- 5. 问答反馈（管理员查看，用于知识库优化）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS qa_feedback (
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    record_id  BIGINT       DEFAULT NULL COMMENT '关联问答记录',
    user_id    BIGINT       DEFAULT NULL COMMENT '反馈用户',
    type       VARCHAR(20)  NOT NULL DEFAULT 'DISLIKE' COMMENT 'LIKE / DISLIKE',
    reason     VARCHAR(500) DEFAULT NULL COMMENT '反馈说明',
    handled    TINYINT      NOT NULL DEFAULT 0 COMMENT '0未处理 1已处理',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_feedback_record_user (record_id, user_id),
    KEY idx_qa_feedback_record (record_id),
    KEY idx_qa_feedback_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '问答反馈记录';
-- 老库升级：ALTER TABLE qa_feedback ADD UNIQUE KEY uk_feedback_record_user (record_id, user_id);

-- -------------------------------------------------------------
-- 6. 敏感词库
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_sensitive_word (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    word       VARCHAR(64) NOT NULL COMMENT '敏感词',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sensitive_word (word)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '敏感词库';

-- -------------------------------------------------------------
-- 7. 操作日志
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_operation_log (
    id         BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id    BIGINT        DEFAULT NULL COMMENT '操作人ID',
    username   VARCHAR(64)   DEFAULT NULL COMMENT '操作人',
    module     VARCHAR(64)   DEFAULT NULL COMMENT '模块',
    action     VARCHAR(64)   DEFAULT NULL COMMENT '动作',
    detail     VARCHAR(1000) DEFAULT NULL COMMENT '详情',
    ip         VARCHAR(64)   DEFAULT NULL COMMENT 'IP',
    status     TINYINT       NOT NULL DEFAULT 1 COMMENT '0失败 1成功',
    error_msg  VARCHAR(500)  DEFAULT NULL COMMENT '失败原因',
    created_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_op_log_user (user_id),
    KEY idx_op_log_created (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统操作日志';
-- 老库升级：ALTER TABLE sys_operation_log
--   ADD COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT '0失败 1成功',
--   ADD COLUMN error_msg VARCHAR(500) DEFAULT NULL COMMENT '失败原因';

-- -------------------------------------------------------------
-- 8. 系统配置
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_config (
    id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    config_key   VARCHAR(100)  NOT NULL COMMENT '配置键',
    config_value VARCHAR(1000) DEFAULT NULL COMMENT '配置值',
    description  VARCHAR(255)  DEFAULT NULL COMMENT '描述',
    updated_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_config_key (config_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统参数配置';

-- -------------------------------------------------------------
-- 9. 会话表（多轮对话上下文）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS qa_conversation (
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id    BIGINT       DEFAULT NULL COMMENT '用户ID',
    title      VARCHAR(128) DEFAULT NULL COMMENT '会话标题（一般取首问）',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_qa_conversation_user (user_id),
    KEY idx_qa_conversation_updated (updated_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '问答会话（多轮上下文）';

-- -------------------------------------------------------------
-- 10. 会话消息表（上下文对话记录：user / assistant 逐条）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS qa_message (
    id              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    conversation_id BIGINT      NOT NULL COMMENT '会话ID',
    role            VARCHAR(16) NOT NULL COMMENT 'user / assistant / system',
    content         TEXT        COMMENT '消息内容',
    sources         JSON        DEFAULT NULL COMMENT '该条回答引用的知识库来源',
    tokens          INT         DEFAULT NULL COMMENT '可选：token 数',
    created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_qa_message_conversation (conversation_id),
    KEY idx_qa_message_created (created_at),
    CONSTRAINT fk_qa_message_conversation FOREIGN KEY (conversation_id) REFERENCES qa_conversation (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '会话消息（上下文对话记录）';

-- -------------------------------------------------------------
-- 11. 角色权限（RBAC：角色 -> 可访问模块）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    role       VARCHAR(20) NOT NULL COMMENT '角色：ADMIN / SUPER_ADMIN',
    module     VARCHAR(64) NOT NULL COMMENT '模块 key：dashboard/documents/qa/feedback/stats/sensitive/users/roles/logs/settings',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_module (role, module)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色权限';

-- =============================================================
-- 初始化数据（可选，按需执行）
-- =============================================================

-- 默认管理员：admin / admin123（密码为 BCrypt 哈希，请上线前修改）
-- INSERT INTO sys_user (username, password, nickname, role)
-- VALUES ('admin', '$2a$10$replace_with_real_bcrypt_hash', '系统管理员', 'ADMIN');

-- 默认分类
-- INSERT INTO kb_category (name, description, sort) VALUES
--   ('入学指南', '新生报到、注册、缴费等流程', 1),
--   ('教务教学', '选课、考试、成绩、学籍管理', 2),
--   ('奖助学金', '奖学金、助学金、贷款政策', 3),
--   ('校园生活', '宿舍、食堂、社团、校园卡', 4),
--   ('就业创业', '实习、招聘、就业指导', 5);

-- 默认敏感词
-- INSERT INTO sys_sensitive_word (word) VALUES ('违禁词'), ('暴恐'), ('代考'), ('黄牛');

-- 默认系统配置
-- INSERT INTO sys_config (config_key, config_value, description) VALUES
--   ('llm.api-url', 'https://dashscope.aliyuncs.com', '大模型接口地址'),
--   ('llm.api-key', '', '大模型 API Key'),
--   ('llm.model', 'qwen-turbo', '对话模型'),
--   ('llm.temperature', '0.7', '温度'),
--   ('llm.max-tokens', '2048', '最大 token'),
--   ('rag.top-k', '3', '向量检索返回片段数');
