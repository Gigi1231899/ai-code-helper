-- =============================================================
-- AI 编程小助手 —— 数据库初始化脚本
-- 使用方式（本地开发，MySQL 8+）：
--   mysql -u root -p < src/main/resources/db/schema.sql
-- 说明：脚本可重复执行（CREATE DATABASE / TABLE IF NOT EXISTS）。
-- =============================================================

CREATE DATABASE IF NOT EXISTS ai_code_helper
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE ai_code_helper;

-- ----------------------------
-- 1. 用户表（用户名唯一）
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_user (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username    VARCHAR(50)  NOT NULL COMMENT '用户名（唯一）',
    password    VARCHAR(200) NOT NULL COMMENT '密码密文，格式：salt$hash（PBKDF2WithHmacSHA256）',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

-- ----------------------------
-- 2. 会话表（一个用户多个会话，会话ID 为 int，创建后模式不可改）
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_conversation (
    id          BIGINT      PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID（int，前端用它区分不同对话）',
    user_id     BIGINT      NOT NULL COMMENT '所属用户ID',
    title       VARCHAR(200) NOT NULL DEFAULT '新对话' COMMENT '会话标题',
    mode        VARCHAR(20) NOT NULL COMMENT '对话模式：rag=知识库 / mcp=联网搜索 / mix=混合（创建后不可修改）',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后活跃时间',
    KEY idx_user_update (user_id, update_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='会话表';

-- ----------------------------
-- 3. 消息表（给前端渲染历史对话用；大模型记忆另存 t_chat_memory）
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_message (
    id              BIGINT   PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    conversation_id BIGINT   NOT NULL COMMENT '所属会话ID',
    role            VARCHAR(20) NOT NULL COMMENT '角色：user=用户 / ai=AI',
    content         LONGTEXT NOT NULL COMMENT '消息内容',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    KEY idx_conversation (conversation_id, id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='对话消息表';

-- ----------------------------
-- 4. 私人知识库文件表（一个用户一个知识库，最多 50 个文件）
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_kb_file (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '文件ID',
    user_id     BIGINT       NOT NULL COMMENT '所属用户ID',
    file_name   VARCHAR(255) NOT NULL COMMENT '原始文件名（用于展示与向量元数据过滤）',
    store_name  VARCHAR(255) NOT NULL COMMENT '落盘文件名（UUID，避免重名覆盖）',
    file_size   BIGINT       NOT NULL COMMENT '文件大小（字节）',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    KEY idx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户知识库文件表';

-- ----------------------------
-- 5. 对话记忆表（LangChain4j ChatMemory 持久化，重启不丢上下文）
--    与 t_message 的区别：这里存的是「大模型视角」的完整消息（含工具调用消息），
--    t_message 只存给用户看的一问一答。
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_chat_memory (
    id          BIGINT    PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    memory_id   BIGINT    NOT NULL COMMENT '记忆ID，直接复用会话ID（保证多用户隔离）',
    content     LONGTEXT  COMMENT 'JSON 序列化的 ChatMessage 列表',
    update_time DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_memory_id (memory_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='对话记忆持久化表';
