-- ============================================
-- 文件归集系统 - Oracle 数据库表结构
-- ============================================

-- 1. 文件元数据表
CREATE TABLE file_metadata (
    id              NUMBER(20)      PRIMARY KEY,
    file_name       VARCHAR2(500)   NOT NULL,
    file_path       VARCHAR2(2000)  NOT NULL,
    file_size       NUMBER(20),
    md5_hash        VARCHAR2(32),
    upstream_name   VARCHAR2(200),
    copy_time       TIMESTAMP       DEFAULT SYSTIMESTAMP,
    status          VARCHAR2(20)    DEFAULT 'SUCCESS'
);

-- 添加注释
COMMENT ON TABLE  file_metadata IS '文件元数据表';
COMMENT ON COLUMN file_metadata.id IS '主键ID';
COMMENT ON COLUMN file_metadata.file_name IS '文件名';
COMMENT ON COLUMN file_metadata.file_path IS '文件路径';
COMMENT ON COLUMN file_metadata.file_size IS '文件大小(字节)';
COMMENT ON COLUMN file_metadata.md5_hash IS 'MD5哈希值';
COMMENT ON COLUMN file_metadata.upstream_name IS '上游源名称';
COMMENT ON COLUMN file_metadata.copy_time IS '拷贝时间';
COMMENT ON COLUMN file_metadata.status IS '状态: SUCCESS/FAILED';

-- 创建索引
CREATE INDEX idx_file_metadata_upstream ON file_metadata(upstream_name);
CREATE INDEX idx_file_metadata_copy_time ON file_metadata(copy_time);
CREATE INDEX idx_file_metadata_status ON file_metadata(status);
CREATE INDEX idx_file_metadata_md5 ON file_metadata(md5_hash);

-- ============================================

-- 2. 操作日志表
CREATE TABLE operation_log (
    id              NUMBER(20)      PRIMARY KEY,
    task_id         VARCHAR2(64)    NOT NULL,
    upstream_name   VARCHAR2(200),
    source_path     VARCHAR2(2000),
    target_path     VARCHAR2(2000),
    file_size       NUMBER(20),
    copy_duration   NUMBER(10),
    status          VARCHAR2(20),
    error_message   VARCHAR2(4000),
    create_time     TIMESTAMP       DEFAULT SYSTIMESTAMP
);

-- 添加注释
COMMENT ON TABLE  operation_log IS '操作日志表';
COMMENT ON COLUMN operation_log.id IS '主键ID';
COMMENT ON COLUMN operation_log.task_id IS '任务ID';
COMMENT ON COLUMN operation_log.upstream_name IS '上游源名称';
COMMENT ON COLUMN operation_log.source_path IS '源文件路径';
COMMENT ON COLUMN operation_log.target_path IS '目标文件路径';
COMMENT ON COLUMN operation_log.file_size IS '文件大小(字节)';
COMMENT ON COLUMN operation_log.copy_duration IS '拷贝耗时(毫秒)';
COMMENT ON COLUMN operation_log.status IS '状态: SUCCESS/FAILED';
COMMENT ON COLUMN operation_log.error_message IS '错误信息';
COMMENT ON COLUMN operation_log.create_time IS '创建时间';

-- 创建索引
CREATE INDEX idx_operation_log_task_id ON operation_log(task_id);
CREATE INDEX idx_operation_log_upstream ON operation_log(upstream_name);
CREATE INDEX idx_operation_log_status ON operation_log(status);
CREATE INDEX idx_operation_log_create_time ON operation_log(create_time);

-- ============================================

-- 3. Oracle 序列（MyBatis-Plus ASSIGN_ID 使用雪花算法，序列可选）
-- 如果使用数据库自增 ID，取消以下注释

/*
CREATE SEQUENCE seq_file_metadata
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

CREATE SEQUENCE seq_operation_log
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;
*/

-- ============================================
