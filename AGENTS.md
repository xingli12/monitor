# AGENTS.md

## Project Overview

文件归集系统 - Spring Boot application that copies files from upstream sources (FTP/CIFS/LOCAL) to downstream local directories with rate limiting.

## Tech Stack

- JDK 21, Spring Boot 3.3.0
- MyBatis-Plus 3.5.6 + Oracle
- Nacos (config center), Redis
- smbj (CIFS/SMB), Apache Commons Net (FTP)
- Guava RateLimiter (rate limiting)

## Commands

```bash
# Compile
mvn compile

# Run all tests
mvn test

# Run single test class
mvn test -Dtest=GlobUtilsTest

# Package
mvn package -DskipTests
```

## Architecture

```
src/main/java/com/filecollection/
├── config/           # Spring config, Nacos, properties
├── controller/       # REST API (POST /execute, GET /tasks/{id}, GET /config)
├── core/             # CollectionService, FileCopier, TaskManager
├── entity/           # MyBatis-Plus entities (FileMetadata, OperationLog)
├── exception/        # FileSystemException
├── mapper/           # MyBatis-Plus mappers
├── strategy/         # FileSystemStrategy + FTP/CIFS/LOCAL implementations
└── util/             # GlobUtils (glob-to-regex)
```

## Key Patterns

- **Strategy pattern**: `FileSystemStrategy` interface with FTP/CIFS/LOCAL implementations
- **Atomic writes**: FileCopier writes to temp file, then `Files.move()` on success
- **Concurrent guard**: `AtomicBoolean` in CollectionService prevents parallel tasks
- **Task cleanup**: `@Scheduled` removes expired tasks every 5 minutes (60min TTL)
- **Glob matching**: `GlobUtils` with Pattern cache - supports `*`, `?`, `[abc]`, `[a-z]`

## Configuration

- Main config: `src/main/resources/application.yml`
- Nacos config example: `src/main/resources/sql/nacos-config-example.yml`
- Oracle schema: `src/main/resources/sql/init.sql`
- Config prefix: `file-collection.*`

## Testing

- 43 unit tests covering core logic
- Test files mirror source structure under `src/test/java/`
- No integration tests (requires Oracle/Nacos/Redis)

## Gotchas

- FTP `retrieveFileStream` requires `completePendingCommand()` - handled by `FtpInputStream` wrapper
- Path separators normalized to `/` for cross-protocol compatibility
- `conflictStrategy` in config supports OVERWRITE (default) and SKIP
