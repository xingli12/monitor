# 文件归集系统设计文档

## 概述

构建一个独立的 Spring Boot 应用，实现从多种上游文件源（FTP、CIFS/SMB、本地目录）限速拷贝文件到下游本地目录的功能。

## 技术栈

- JDK 21
- Spring Boot 3.x
- Apache Commons VFS2（文件系统统一抽象）
- smbj（CIFS/SMB 客户端）
- Apache Commons Net（FTP 客户端）
- Guava RateLimiter（全局限速）
- MyBatis-Plus + Oracle（数据持久化）
- Redis（配置缓存）
- Nacos（配置中心）
- Lombok
- Logback

## 配置结构

```yaml
file-collection:
  rate-limit: 10485760  # 10MB/s 全局限速
  
  upstreams:
    - name: ftp-server-01
      type: FTP
      host: 192.168.1.100
      port: 21
      username: user
      password: pass
      path: /data/incoming
      file-pattern: "*.*"
      
    - name: cifs-share-01
      type: CIFS
      host: 192.168.1.200
      share-name: shared
      username: domain\user
      password: pass
      path: /incoming
      file-pattern: "*.csv"
      
    - name: local-source-01
      type: LOCAL
      path: /data/local/incoming
      file-pattern: "*.*"
  
  downstreams:
    - name: local-target-01
      type: LOCAL
      path: /data/collected
      conflict-strategy: OVERWRITE
```

## 核心架构

```
┌─────────────────────────────────────────────────┐
│                  REST API Layer                  │
│  POST /api/collection/execute  (触发归集任务)     │
│  GET  /api/collection/tasks/{id} (查询任务状态)   │
│  GET  /api/collection/config (查看配置)           │
└─────────────┬───────────────────────────────────┘
              │
┌─────────────▼───────────────────────────────────┐
│            CollectionService (核心编排)           │
│  1. 读取 YAML 配置，加载上游/下游信息            │
│  2. 遍历每个上游，获取文件列表                    │
│  3. 逐文件通过限速器拷贝到所有下游                │
│  4. 写入操作日志 + 文件元数据                     │
└─────┬───────────────────┬───────────────────────┘
      │                   │
┌─────▼─────┐     ┌───────▼───────────────────┐
│ RateLimiter│     │  FileSystemStrategy       │
│ (Guava)   │     │  ┌─────────────────────┐  │
│ 全局限速   │     │  │ FtpFileSystem       │  │
└───────────┘     │  │ (Apache Commons Net)│  │
                  │  ├─────────────────────┤  │
                  │  │ CifsFileSystem      │  │
                  │  │ (smbj)              │  │
                  │  ├─────────────────────┤  │
                  │  │ LocalFileSystem     │  │
                  │  │ (Java NIO)          │  │
                  │  └─────────────────────┘  │
                  └───────────────────────────┘
```

### 核心组件

1. **FileSystemStrategy** — 策略接口，定义 `connect()`、`listFiles()`、`copyFile()`、`disconnect()`
2. **FtpFileSystem** — 基于 Apache Commons Net 实现 FTP 操作
3. **CifsFileSystem** — 基于 smbj 实现 CIFS/SMB 操作
4. **LocalFileSystem** — 基于 Java NIO 实现本地文件操作
5. **FileCopier** — 核心拷贝器，集成 Guava RateLimiter 控制流量
6. **CollectionService** — 编排层，协调配置读取、文件扫描、拷贝执行、日志记录

## 数据流

```
1. POST /api/collection/execute
   │
2. CollectionService.loadConfig()
   │  从 YAML 加载 upstreams + downstreams 配置
   │
3. 遍历每个 upstream:
   │  ├─ 创建 FileSystemStrategy (FTP/CIFS/LOCAL)
   │  ├─ connect() 建立连接
   │  ├─ listFiles(pattern) 获取文件列表
   │
4. 对每个文件:
   │  ├─ 通过 RateLimiter.acquire(size) 等待配额
   │  ├─ 从 upstream 读取文件流
   │  ├─ 写入 downstream 目录 (覆盖写)
   │  ├─ 记录文件元数据到 Oracle (MD5 + 大小 + 时间)
   │  └─ 记录操作日志到 Oracle
   │
5. disconnect() 关闭所有连接
   │
6. 返回执行结果 (成功数/失败数/跳过数)
```

### 关键细节

- **限速实现**：Guava RateLimiter 以 bytes/sec 为单位，每次 `acquire(size)` 等待发送 size 字节的配额
- **流式拷贝**：不读完整文件到内存，使用 8KB buffer 流式传输
- **错误隔离**：单个文件拷贝失败不影响其他文件，失败记录到日志继续执行
- **连接复用**：同一 upstream 的多个文件共享一个连接，执行完后断开

## 数据库设计

### 文件元数据表 `file_metadata`

```sql
CREATE TABLE file_metadata (
    id              NUMBER PRIMARY KEY,
    file_name       VARCHAR2(500) NOT NULL,
    file_path       VARCHAR2(2000) NOT NULL,
    file_size       NUMBER(20),
    md5_hash        VARCHAR2(32),
    upstream_name   VARCHAR2(200),
    copy_time       TIMESTAMP DEFAULT SYSTIMESTAMP,
    status          VARCHAR2(20) DEFAULT 'SUCCESS'
);
```

### 操作日志表 `operation_log`

```sql
CREATE TABLE operation_log (
    id              NUMBER PRIMARY KEY,
    task_id         VARCHAR2(64) NOT NULL,
    upstream_name   VARCHAR2(200),
    source_path     VARCHAR2(2000),
    target_path     VARCHAR2(2000),
    file_size       NUMBER(20),
    copy_duration   NUMBER(10),
    status          VARCHAR2(20),
    error_message   VARCHAR2(4000),
    create_time     TIMESTAMP DEFAULT SYSTIMESTAMP
);
```

### 说明

- `file_metadata`：记录每个已拷贝文件的指纹，支持后续去重/审计
- `operation_log`：记录每次拷贝操作的详细信息，含耗时和错误信息
- 使用 MyBatis-Plus 自动建表 + CRUD
- Redis 用于缓存 YAML 配置，避免每次执行都读文件

## API 设计

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/collection/execute` | 手动触发归集任务 |
| GET | `/api/collection/config` | 查看当前配置（脱敏） |
| GET | `/api/collection/tasks/{taskId}` | 查询任务执行状态 |

### 触发归集接口

```json
// POST /api/collection/execute
// Request Body (可选，指定上游)
{
  "upstreamNames": ["ftp-server-01"]  // 空则执行所有上游
}

// Response
{
  "code": 200,
  "data": {
    "taskId": "uuid-xxxx-xxxx",
    "status": "SUCCESS",
    "summary": {
      "totalFiles": 128,
      "successCount": 125,
      "failCount": 3,
      "skippedCount": 0,
      "totalBytes": 1073741824,
      "durationMs": 85230
    }
  }
}
```

### 查询任务状态

```json
// GET /api/collection/tasks/{taskId}
// Response
{
  "code": 200,
  "data": {
    "taskId": "uuid-xxxx-xxxx",
    "status": "SUCCESS",
    "startTime": "2026-06-27T10:00:00",
    "endTime": "2026-06-27T10:01:25",
    "summary": { ... },
    "errors": [
      { "fileName": "bad_file.csv", "error": "Connection timeout" }
    ]
  }
}
```

## 错误处理

| 错误类型 | 处理方式 |
|----------|----------|
| 上游连接失败 | 跳过该上游，记录错误，继续执行其他上游 |
| 单文件拷贝失败 | 记录错误，跳过该文件，继续拷贝其他文件 |
| 下游写入失败 | 记录错误，跳过该文件 |
| 限速器异常 | 降级为不限速，记录警告 |

## Nacos 集成

- YAML 配置文件存储在 Nacos 配置中心，支持动态刷新
- 通过 `@RefreshScope` 实现配置热更新，无需重启应用

## 日志规范（Logback）

- 任务启动/完成：INFO 级别
- 文件拷贝成功：DEBUG 级别
- 连接超时/文件拷贝失败：ERROR 级别
- 限速等待：TRACE 级别

## 包结构

```
com.filecollection
├── config/
│   ├── FileCollectionProperties.java    # YAML 配置映射
│   └── NacosConfig.java                 # Nacos 配置类
├── strategy/
│   ├── FileSystemStrategy.java          # 策略接口
│   ├── FtpFileSystem.java              # FTP 实现
│   ├── CifsFileSystem.java             # CIFS 实现 (smbj)
│   └── LocalFileSystem.java            # 本地文件实现
├── core/
│   ├── CollectionService.java           # 核心编排服务
│   ├── FileCopier.java                 # 限速拷贝器
│   └── TaskManager.java                # 任务状态管理
├── controller/
│   └── CollectionController.java        # REST API
├── entity/
│   ├── FileMetadata.java               # 文件元数据实体
│   └── OperationLog.java               # 操作日志实体
├── mapper/
│   ├── FileMetadataMapper.java          # MyBatis-Plus Mapper
│   └── OperationLogMapper.java          # MyBatis-Plus Mapper
└── dto/
    ├── ExecuteRequest.java             # 执行请求 DTO
    └── TaskResponse.java               # 任务响应 DTO
```
