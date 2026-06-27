# AGENTS.md

## 项目概述

文件归集系统 - 从上游文件源（FTP/CIFS/本地目录）限速拷贝文件到下游本地目录的 Spring Boot 应用。

## 技术栈

- JDK 21, Spring Boot 3.3.0
- MyBatis-Plus 3.5.6 + Oracle
- Nacos（配置中心）、Redis
- smbj（CIFS/SMB）、Apache Commons Net（FTP）
- Guava RateLimiter（限速）

## 常用命令

```bash
# 编译
mvn compile

# 运行全部测试
mvn test

# 运行单个测试类
mvn test -Dtest=GlobUtilsTest

# 打包
mvn package -DskipTests
```

## 架构结构

```
src/main/java/com/filecollection/
├── config/           # Spring 配置、Nacos、属性映射
├── controller/       # REST API（POST /execute、GET /tasks/{id}、GET /config）
├── core/             # CollectionService、FileCopier、TaskManager
├── entity/           # MyBatis-Plus 实体（FileMetadata、OperationLog）
├── exception/        # FileSystemException
├── mapper/           # MyBatis-Plus Mapper
├── strategy/         # FileSystemStrategy + FTP/CIFS/LOCAL 实现
└── util/             # GlobUtils（glob 转正则）
```

## 关键设计模式

- **策略模式**：`FileSystemStrategy` 接口，FTP/CIFS/LOCAL 三种实现
- **原子写入**：FileCopier 先写临时文件，成功后 `Files.move()` 替换
- **并发控制**：CollectionService 使用 `AtomicBoolean` 防止并行任务
- **任务清理**：`@Scheduled` 每 5 分钟清理过期任务（60 分钟 TTL）
- **Glob 匹配**：`GlobUtils` 带 Pattern 缓存，支持 `*`、`?`、`[abc]`、`[a-z]`

## 配置文件

- 主配置：`src/main/resources/application.yml`
- Nacos 配置示例：`src/main/resources/sql/nacos-config-example.yml`
- Oracle 建表脚本：`src/main/resources/sql/init.sql`
- 配置前缀：`file-collection.*`

## 测试说明

- 43 个单元测试覆盖核心逻辑
- 测试文件位于 `src/test/java/`
- 无集成测试（需要 Oracle/Nacos/Redis 环境）

## 注意事项

- FTP `retrieveFileStream` 必须调用 `completePendingCommand()` - 已通过 `FtpInputStream` 包装类处理
- 路径分隔符统一使用 `/`，兼容跨协议
- `conflictStrategy` 配置支持 OVERWRITE（默认）和 SKIP
- `FileCopier` 已配置为 Spring Bean（`FileCopierConfig`）
