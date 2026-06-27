# 文件归集系统实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建一个 Spring Boot 应用，实现从 FTP/CIFS/本地目录限速拷贝文件到下游本地目录

**Architecture:** 策略模式统一三种文件系统操作，Guava RateLimiter 全局限速，MyBatis-Plus 持久化元数据和日志

**Tech Stack:** JDK 21, Spring Boot 3.x, Apache Commons VFS2, smbj, Apache Commons Net, Guava, MyBatis-Plus, Oracle, Redis, Nacos, Lombok, Logback

## Global Constraints

- JDK 21，Spring Boot 3.x，MyBatis-Plus 3.5.x
- 配置存储在 Nacos，支持 @RefreshScope 动态刷新
- 全局限速，所有上游共享带宽上限
- 文件冲突策略：覆盖写入
- Oracle 数据库，Redis 缓存配置

## 文件结构

```
com.filecollection
├── FileCollectionApplication.java
├── config/
│   ├── FileCollectionProperties.java
│   ├── NacosConfig.java
│   └── RestTemplateConfig.java
├── strategy/
│   ├── FileSystemStrategy.java
│   ├── FtpFileSystem.java
│   ├── CifsFileSystem.java
│   └── LocalFileSystem.java
├── core/
│   ├── FileCopier.java
│   ├── CollectionService.java
│   └── TaskManager.java
├── controller/
│   ├── CollectionController.java
│   └── dto/
│       ├── ExecuteRequest.java
│       └── TaskResponse.java
├── entity/
│   ├── FileMetadata.java
│   └── OperationLog.java
├── mapper/
│   ├── FileMetadataMapper.java
│   └── OperationLogMapper.java
└── exception/
    ├── FileSystemException.java
    └── CollectionException.java
```

---

### Task 1: 项目初始化

**Files:**
- Create: `pom.xml`
- Create: `src/main/resources/application.yml`
- Create: `src/main/resources/logback-spring.xml`

**Interfaces:**
- Produces: Spring Boot 项目骨架，可运行

- [ ] **Step 1: 创建 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.0</version>
        <relativePath/>
    </parent>
    
    <groupId>com.filecollection</groupId>
    <artifactId>file-collection</artifactId>
    <version>1.0.0</version>
    <name>file-collection</name>
    <description>文件归集系统</description>
    
    <properties>
        <java.version>21</java.version>
        <mybatis-plus.version>3.5.6</mybatis-plus.version>
        <guava.version>33.2.0-jre</guava.version>
        <commons-net.version>3.10.0</commons-net.version>
        <smbj.version>0.13.0</smbj.version>
        <commons-vfs2.version>2.9.0</commons-vfs2.version>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
            <version>2023.0.1.0</version>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>
        <dependency>
            <groupId>com.oracle.database.jdbc</groupId>
            <artifactId>ojdbc8</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
            <version>${guava.version}</version>
        </dependency>
        <dependency>
            <groupId>commons-net</groupId>
            <artifactId>commons-net</artifactId>
            <version>${commons-net.version}</version>
        </dependency>
        <dependency>
            <groupId>com.hierynomus</groupId>
            <artifactId>smbj</artifactId>
            <version>${smbj.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-vfs2</artifactId>
            <version>${commons-vfs2.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建 application.yml**

```yaml
server:
  port: 8080

spring:
  application:
    name: file-collection
  cloud:
    nacos:
      config:
        server-addr: localhost:8848
        file-extension: yml
        group: DEFAULT_GROUP
  datasource:
    url: jdbc:oracle:thin:@localhost:1521:orcl
    username: system
    password: password
    driver-class-name: oracle.jdbc.OracleDriver
  data:
    redis:
      host: localhost
      port: 6379

mybatis-plus:
  mapper-locations: classpath:mapper/*.xml
  global-config:
    db-config:
      id-type: ASSIGN_ID
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

logging:
  level:
    com.filecollection: DEBUG
    org.apache.commons.net: INFO
    com.hierynomus.smbj: INFO
```

- [ ] **Step 3: 创建 logback-spring.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/file-collection.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/file-collection.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

- [ ] **Step 4: 创建启动类**

```java
package com.filecollection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FileCollectionApplication {
    public static void main(String[] args) {
        SpringApplication.run(FileCollectionApplication.class, args);
    }
}
```

- [ ] **Step 5: 验证项目可编译**

Run: `mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 6: 提交**

```bash
git add .
git commit -m "feat: init Spring Boot project with dependencies"
```

---

### Task 2: 配置属性映射

**Files:**
- Create: `src/main/java/com/filecollection/config/FileCollectionProperties.java`
- Create: `src/main/java/com/filecollection/config/NacosConfig.java`

**Interfaces:**
- Produces: `FileCollectionProperties` 类，提供 `getUpstreams()`, `getDownstreams()`, `getRateLimit()`

- [ ] **Step 1: 创建 FileCollectionProperties**

```java
package com.filecollection.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "file-collection")
public class FileCollectionProperties {
    
    private long rateLimit = 10 * 1024 * 1024; // 默认 10MB/s
    private List<UpstreamConfig> upstreams = new ArrayList<>();
    private List<DownstreamConfig> downstreams = new ArrayList<>();
    
    @Data
    public static class UpstreamConfig {
        private String name;
        private String type; // FTP, CIFS, LOCAL
        private String host;
        private int port;
        private String shareName;
        private String username;
        private String password;
        private String path;
        private String filePattern = "*.*";
    }
    
    @Data
    public static class DownstreamConfig {
        private String name;
        private String type; // LOCAL
        private String path;
        private String conflictStrategy = "OVERWRITE";
    }
}
```

- [ ] **Step 2: 创建 NacosConfig**

```java
package com.filecollection.config;

import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

@Configuration
@RefreshScope
public class NacosConfig {
    // Nacos 配置通过 @ConfigurationProperties 自动加载
    // @RefreshScope 支持配置热更新
}
```

- [ ] **Step 3: 验证配置加载**

Run: `mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/filecollection/config/
git commit -m "feat: add YAML config properties mapping"
```

---

### Task 3: FileSystemStrategy 接口

**Files:**
- Create: `src/main/java/com/filecollection/strategy/FileSystemStrategy.java`
- Create: `src/main/java/com/filecollection/exception/FileSystemException.java`

**Interfaces:**
- Produces: `FileSystemStrategy` 接口，定义 `connect()`, `listFiles()`, `copyFile()`, `disconnect()`

- [ ] **Step 1: 创建 FileSystemException**

```java
package com.filecollection.exception;

public class FileSystemException extends RuntimeException {
    
    public FileSystemException(String message) {
        super(message);
    }
    
    public FileSystemException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **Step 2: 创建 FileSystemStrategy 接口**

```java
package com.filecollection.strategy;

import java.io.InputStream;
import java.util.List;

public interface FileSystemStrategy {
    
    void connect() throws FileSystemException;
    
    List<String> listFiles(String path, String pattern) throws FileSystemException;
    
    InputStream readFile(String filePath) throws FileSystemException;
    
    void writeFile(String targetPath, InputStream content) throws FileSystemException;
    
    long getFileSize(String filePath) throws FileSystemException;
    
    void disconnect();
}
```

- [ ] **Step 3: 验证编译**

Run: `mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/filecollection/strategy/ src/main/java/com/filecollection/exception/
git commit -m "feat: add FileSystemStrategy interface and exception"
```

---

### Task 4: LocalFileSystem 实现

**Files:**
- Create: `src/main/java/com/filecollection/strategy/LocalFileSystem.java`
- Create: `src/test/java/com/filecollection/strategy/LocalFileSystemTest.java`

**Interfaces:**
- Consumes: `FileSystemStrategy` 接口
- Produces: `LocalFileSystem` 类

- [ ] **Step 1: 写失败测试**

```java
package com.filecollection.strategy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LocalFileSystemTest {
    
    @TempDir
    Path tempDir;
    
    @Test
    void shouldListFilesMatchingPattern() throws Exception {
        // Given
        Files.createFile(tempDir.resolve("test.csv"));
        Files.createFile(tempDir.resolve("data.txt"));
        Files.createFile(tempDir.resolve("report.csv"));
        
        LocalFileSystem fs = new LocalFileSystem(tempDir.toString(), "*.*");
        
        // When
        List<String> files = fs.listFiles(tempDir.toString(), "*.csv");
        
        // Then
        assertEquals(2, files.size());
        assertTrue(files.contains("test.csv"));
        assertTrue(files.contains("report.csv"));
    }
    
    @Test
    void shouldReadAndWriteFile() throws Exception {
        // Given
        LocalFileSystem fs = new LocalFileSystem(tempDir.toString(), "*.*");
        String content = "test content";
        InputStream inputStream = new java.io.ByteArrayInputStream(content.getBytes());
        
        // When
        String targetPath = tempDir.resolve("output.txt").toString();
        fs.writeFile(targetPath, inputStream);
        
        // Then
        assertTrue(Files.exists(Path.of(targetPath)));
        String result = Files.readString(Path.of(targetPath));
        assertEquals(content, result);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn test -Dtest=LocalFileSystemTest`
Expected: FAIL with "LocalFileSystem not found"

- [ ] **Step 3: 实现 LocalFileSystem**

```java
package com.filecollection.strategy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Stream;

public class LocalFileSystem implements FileSystemStrategy {
    
    private final String basePath;
    private final String filePattern;
    
    public LocalFileSystem(String basePath, String filePattern) {
        this.basePath = basePath;
        this.filePattern = filePattern;
    }
    
    @Override
    public void connect() {
        Path path = Path.of(basePath);
        if (!Files.exists(path)) {
            throw new FileSystemException("Base path does not exist: " + basePath);
        }
    }
    
    @Override
    public List<String> listFiles(String path, String pattern) {
        Path dir = Path.of(path);
        if (!Files.exists(dir)) {
            throw new FileSystemException("Path does not exist: " + path);
        }
        
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                .filter(Files::isRegularFile)
                .map(p -> p.getFileName().toString())
                .filter(name -> matchGlob(name, pattern))
                .toList();
        } catch (IOException e) {
            throw new FileSystemException("Failed to list files", e);
        }
    }
    
    @Override
    public InputStream readFile(String filePath) {
        try {
            return Files.newInputStream(Path.of(filePath));
        } catch (IOException e) {
            throw new FileSystemException("Failed to read file: " + filePath, e);
        }
    }
    
    @Override
    public void writeFile(String targetPath, InputStream content) {
        try {
            Path target = Path.of(targetPath);
            Files.createDirectories(target.getParent());
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new FileSystemException("Failed to write file: " + targetPath, e);
        }
    }
    
    @Override
    public long getFileSize(String filePath) {
        try {
            return Files.size(Path.of(filePath));
        } catch (IOException e) {
            throw new FileSystemException("Failed to get file size: " + filePath, e);
        }
    }
    
    @Override
    public void disconnect() {
        // 本地文件系统无需断开连接
    }
    
    private boolean matchGlob(String name, String pattern) {
        String regex = pattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".");
        return name.matches(regex);
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn test -Dtest=LocalFileSystemTest`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add src/main/java/com/filecollection/strategy/LocalFileSystem.java src/test/java/com/filecollection/strategy/LocalFileSystemTest.java
git commit -m "feat: implement LocalFileSystem with tests"
```

---

### Task 5: FtpFileSystem 实现

**Files:**
- Create: `src/main/java/com/filecollection/strategy/FtpFileSystem.java`
- Create: `src/test/java/com/filecollection/strategy/FtpFileSystemTest.java`

**Interfaces:**
- Consumes: `FileSystemStrategy` 接口
- Produces: `FtpFileSystem` 类

- [ ] **Step 1: 写失败测试**

```java
package com.filecollection.strategy;

import org.apache.commons.net.ftp.FTPClient;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FtpFileSystemTest {
    
    @Test
    void shouldConnectToFTPServer() {
        // Given
        FtpFileSystem fs = new FtpFileSystem("localhost", 21, "user", "pass", "/data");
        
        // When & Then (mock FTPClient)
        // 实际测试需要连接真实FTP服务器或使用Mock
    }
    
    @Test
    void shouldParseFtpFileList() throws Exception {
        // Given
        String listing = "-rw-r--r-- 1 user group 1024 Jun 27 10:00 test.csv\n"
                       + "-rw-r--r-- 1 user group 2048 Jun 27 11:00 data.txt";
        
        // When
        List<String> files = FtpFileSystem.parseFileList(listing);
        
        // Then
        assertEquals(2, files.size());
        assertTrue(files.contains("test.csv"));
        assertTrue(files.contains("data.txt"));
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn test -Dtest=FtpFileSystemTest`
Expected: FAIL

- [ ] **Step 3: 实现 FtpFileSystem**

```java
package com.filecollection.strategy;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class FtpFileSystem implements FileSystemStrategy {
    
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String basePath;
    
    private FTPClient ftpClient;
    
    public FtpFileSystem(String host, int port, String username, String password, String basePath) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.basePath = basePath;
    }
    
    @Override
    public void connect() throws FileSystemException {
        try {
            ftpClient = new FTPClient();
            ftpClient.connect(host, port);
            ftpClient.login(username, password);
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
            log.info("Connected to FTP server: {}", host);
        } catch (IOException e) {
            throw new FileSystemException("Failed to connect to FTP server: " + host, e);
        }
    }
    
    @Override
    public List<String> listFiles(String path, String pattern) throws FileSystemException {
        try {
            FTPFile[] ftpFiles = ftpClient.listFiles(path);
            List<String> files = new ArrayList<>();
            
            for (FTPFile ftpFile : ftpFiles) {
                if (ftpFile.isFile() && matchGlob(ftpFile.getName(), pattern)) {
                    files.add(ftpFile.getName());
                }
            }
            return files;
        } catch (IOException e) {
            throw new FileSystemException("Failed to list files", e);
        }
    }
    
    @Override
    public InputStream readFile(String filePath) throws FileSystemException {
        try {
            return ftpClient.retrieveFileStream(filePath);
        } catch (IOException e) {
            throw new FileSystemException("Failed to read file: " + filePath, e);
        }
    }
    
    @Override
    public void writeFile(String targetPath, InputStream content) throws FileSystemException {
        // FTP 作为上游，不需要实现写入
        throw new UnsupportedOperationException("FTP upstream does not support write");
    }
    
    @Override
    public long getFileSize(String filePath) throws FileSystemException {
        try {
            FTPFile[] files = ftpClient.listFiles(filePath);
            if (files.length > 0) {
                return files[0].getSize();
            }
            return 0;
        } catch (IOException e) {
            throw new FileSystemException("Failed to get file size: " + filePath, e);
        }
    }
    
    @Override
    public void disconnect() {
        try {
            if (ftpClient != null && ftpClient.isConnected()) {
                ftpClient.logout();
                ftpClient.disconnect();
                log.info("Disconnected from FTP server: {}", host);
            }
        } catch (IOException e) {
            log.error("Error disconnecting from FTP server", e);
        }
    }
    
    public static List<String> parseFileList(String listing) {
        List<String> files = new ArrayList<>();
        String[] lines = listing.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty() && !line.startsWith("total")) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 8) {
                    files.add(parts[parts.length - 1]);
                }
            }
        }
        return files;
    }
    
    private boolean matchGlob(String name, String pattern) {
        String regex = pattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".");
        return name.matches(regex);
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn test -Dtest=FtpFileSystemTest`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add src/main/java/com/filecollection/strategy/FtpFileSystem.java src/test/java/com/filecollection/strategy/FtpFileSystemTest.java
git commit -m "feat: implement FtpFileSystem with tests"
```

---

### Task 6: CifsFileSystem 实现

**Files:**
- Create: `src/main/java/com/filecollection/strategy/CifsFileSystem.java`
- Create: `src/test/java/com/filecollection/strategy/CifsFileSystemTest.java`

**Interfaces:**
- Consumes: `FileSystemStrategy` 接口，smbj 库
- Produces: `CifsFileSystem` 类

- [ ] **Step 1: 写失败测试**

```java
package com.filecollection.strategy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CifsFileSystemTest {
    
    @Test
    void shouldMatchSmbPath() {
        // Given
        String path = "/incoming/data/test.csv";
        
        // When & Then
        assertTrue(CifsFileSystem.isSmbPath(path));
        assertFalse(CifsFileSystem.isSmbPath("/local/path"));
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn test -Dtest=CifsFileSystemTest`
Expected: FAIL

- [ ] **Step 3: 实现 CifsFileSystem**

```java
package com.filecollection.strategy;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Slf4j
public class CifsFileSystem implements FileSystemStrategy {
    
    private final String host;
    private final String shareName;
    private final String username;
    private final String password;
    private final String basePath;
    
    private SMBClient client;
    private DiskShare share;
    private com.hierynomus.smbj.session.Session session;
    
    public CifsFileSystem(String host, String shareName, String username, String password, String basePath) {
        this.host = host;
        this.shareName = shareName;
        this.username = username;
        this.password = password;
        this.basePath = basePath;
    }
    
    @Override
    public void connect() throws FileSystemException {
        try {
            client = new SMBClient();
            session = client.connect(host).authenticate(new AuthenticationContext(username, password.toCharArray(), null));
            share = (DiskShare) session.connectShare(shareName);
            log.info("Connected to CIFS share: {} at {}", shareName, host);
        } catch (IOException e) {
            throw new FileSystemException("Failed to connect to CIFS server: " + host, e);
        }
    }
    
    @Override
    public List<String> listFiles(String path, String pattern) throws FileSystemException {
        List<String> files = new ArrayList<>();
        try (var dir = share.openFolder(path, EnumSet.of(AccessMask.GENERIC_READ), null, EnumSet.of(SMB2ShareAccess.FILE_SHARE_READ), null, null)) {
            for (var entry : dir.list()) {
                if (!entry.isDirectory() && matchGlob(entry.getName(), pattern)) {
                    files.add(entry.getName());
                }
            }
        } catch (IOException e) {
            throw new FileSystemException("Failed to list files", e);
        }
        return files;
    }
    
    @Override
    public InputStream readFile(String filePath) throws FileSystemException {
        try {
            File smbFile = share.openFile(
                filePath,
                EnumSet.of(AccessMask.GENERIC_READ),
                null,
                EnumSet.of(SMB2ShareAccess.FILE_SHARE_READ),
                SMB2CreateDisposition.FILE_OPEN,
                null
            );
            return smbFile.getInputStream();
        } catch (IOException e) {
            throw new FileSystemException("Failed to read file: " + filePath, e);
        }
    }
    
    @Override
    public void writeFile(String targetPath, InputStream content) throws FileSystemException {
        try (File smbFile = share.openFile(
                targetPath,
                EnumSet.of(AccessMask.GENERIC_WRITE),
                null,
                EnumSet.of(SMB2ShareAccess.FILE_SHARE_WRITE),
                SMB2CreateDisposition.FILE_OVERWRITE_IF,
                null
            );
            OutputStream out = smbFile.getOutputStream()) {
            content.transferTo(out);
        } catch (IOException e) {
            throw new FileSystemException("Failed to write file: " + targetPath, e);
        }
    }
    
    @Override
    public long getFileSize(String filePath) throws FileSystemException {
        try (File smbFile = share.openFile(
                filePath,
                EnumSet.of(AccessMask.GENERIC_READ),
                null,
                EnumSet.of(SMB2ShareAccess.FILE_SHARE_READ),
                SMB2CreateDisposition.FILE_OPEN,
                null
            )) {
            return smbFile.getFileInformation().getStandardInformation().getEndOfFile();
        } catch (IOException e) {
            throw new FileSystemException("Failed to get file size: " + filePath, e);
        }
    }
    
    @Override
    public void disconnect() {
        try {
            if (share != null) share.close();
            if (session != null) session.close();
            if (client != null) client.close();
            log.info("Disconnected from CIFS share: {}", shareName);
        } catch (IOException e) {
            log.error("Error disconnecting from CIFS server", e);
        }
    }
    
    public static boolean isSmbPath(String path) {
        return path.startsWith("\\\\") || path.startsWith("smb://");
    }
    
    private boolean matchGlob(String name, String pattern) {
        String regex = pattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".");
        return name.matches(regex);
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn test -Dtest=CifsFileSystemTest`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add src/main/java/com/filecollection/strategy/CifsFileSystem.java src/test/java/com/filecollection/strategy/CifsFileSystemTest.java
git commit -m "feat: implement CifsFileSystem with smbj"
```

---

### Task 7: FileCopier 限速拷贝器

**Files:**
- Create: `src/main/java/com/filecollection/core/FileCopier.java`
- Create: `src/test/java/com/filecollection/core/FileCopierTest.java`

**Interfaces:**
- Consumes: `FileSystemStrategy` 接口，Guava `RateLimiter`
- Produces: `FileCopier` 类，方法 `copyFile(strategy, sourcePath, targetPath, fileSize)`

- [ ] **Step 1: 写失败测试**

```java
package com.filecollection.core;

import com.filecollection.strategy.LocalFileSystem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileCopierTest {
    
    @TempDir
    Path tempDir;
    
    @Test
    void shouldCopyFileWithRateLimit() throws Exception {
        // Given
        Path source = tempDir.resolve("source.txt");
        Files.writeString(source, "test content");
        
        Path target = tempDir.resolve("target.txt");
        
        LocalFileSystem sourceFs = new LocalFileSystem(tempDir.toString(), "*.*");
        LocalFileSystem targetFs = new LocalFileSystem(tempDir.toString(), "*.*");
        
        FileCopier copier = new FileCopier(1024 * 1024); // 1MB/s
        
        // When
        copier.copyFile(sourceFs, source.toString(), targetFs, target.toString());
        
        // Then
        assertTrue(Files.exists(target));
        assertEquals("test content", Files.readString(target));
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn test -Dtest=FileCopierTest`
Expected: FAIL

- [ ] **Step 3: 实现 FileCopier**

```java
package com.filecollection.core;

import com.filecollection.strategy.FileSystemStrategy;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class FileCopier {
    
    private final RateLimiter rateLimiter;
    private static final int BUFFER_SIZE = 8192; // 8KB buffer
    
    public FileCopier(long bytesPerSecond) {
        this.rateLimiter = RateLimiter.create(bytesPerSecond);
    }
    
    public void copyFile(FileSystemStrategy sourceFs, String sourcePath, 
                         FileSystemStrategy targetFs, String targetPath) {
        log.debug("Copying file: {} -> {}", sourcePath, targetPath);
        
        try (InputStream in = sourceFs.readFile(sourcePath);
             OutputStream out = targetFs.writeFile(targetPath, in)) {
            
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            
            while ((bytesRead = in.read(buffer)) != -1) {
                // 限速：等待发送这些字节的配额
                rateLimiter.acquire(bytesRead);
                out.write(buffer, 0, bytesRead);
            }
            
            out.flush();
            log.debug("File copied successfully: {}", targetPath);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to copy file: " + sourcePath, e);
        }
    }
}
```

- [ ] **Step 4: 修正 LocalFileSystem.writeFile 返回 OutputStream**

```java
// 修改 LocalFileSystem.java
@Override
public OutputStream writeFile(String targetPath) throws FileSystemException {
    try {
        Path target = Path.of(targetPath);
        Files.createDirectories(target.getParent());
        return Files.newOutputStream(target, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
        throw new FileSystemException("Failed to open output stream: " + targetPath, e);
    }
}
```

- [ ] **Step 5: 运行测试确认通过**

Run: `mvn test -Dtest=FileCopierTest`
Expected: PASS

- [ ] **Step 6: 提交**

```bash
git add src/main/java/com/filecollection/core/FileCopier.java src/main/java/com/filecollection/strategy/LocalFileSystem.java src/test/java/com/filecollection/core/FileCopierTest.java
git commit -m "feat: implement FileCopier with rate limiting"
```

---

### Task 8: 数据库实体和 Mapper

**Files:**
- Create: `src/main/java/com/filecollection/entity/FileMetadata.java`
- Create: `src/main/java/com/filecollection/entity/OperationLog.java`
- Create: `src/main/java/com/filecollection/mapper/FileMetadataMapper.java`
- Create: `src/main/java/com/filecollection/mapper/OperationLogMapper.java`

**Interfaces:**
- Produces: MyBatis-Plus 实体和 Mapper

- [ ] **Step 1: 创建 FileMetadata 实体**

```java
package com.filecollection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_metadata")
public class FileMetadata {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String md5Hash;
    private String upstreamName;
    private LocalDateTime copyTime;
    private String status;
}
```

- [ ] **Step 2: 创建 OperationLog 实体**

```java
package com.filecollection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("operation_log")
public class OperationLog {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private String taskId;
    private String upstreamName;
    private String sourcePath;
    private String targetPath;
    private Long fileSize;
    private Long copyDuration;
    private String status;
    private String errorMessage;
    private LocalDateTime createTime;
}
```

- [ ] **Step 3: 创建 FileMetadataMapper**

```java
package com.filecollection.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.filecollection.entity.FileMetadata;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileMetadataMapper extends BaseMapper<FileMetadata> {
}
```

- [ ] **Step 4: 创建 OperationLogMapper**

```java
package com.filecollection.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.filecollection.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
```

- [ ] **Step 5: 验证编译**

Run: `mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 6: 提交**

```bash
git add src/main/java/com/filecollection/entity/ src/main/java/com/filecollection/mapper/
git commit -m "feat: add database entities and MyBatis-Plus mappers"
```

---

### Task 9: TaskManager 任务管理

**Files:**
- Create: `src/main/java/com/filecollection/core/TaskManager.java`
- Create: `src/test/java/com/filecollection/core/TaskManagerTest.java`

**Interfaces:**
- Produces: `TaskManager` 类，方法 `createTask()`, `getTask()`, `updateTaskStatus()`

- [ ] **Step 1: 写失败测试**

```java
package com.filecollection.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TaskManagerTest {
    
    @Test
    void shouldCreateAndGetTask() {
        // Given
        TaskManager manager = new TaskManager();
        
        // When
        String taskId = manager.createTask();
        TaskManager.TaskStatus task = manager.getTask(taskId);
        
        // Then
        assertNotNull(taskId);
        assertNotNull(task);
        assertEquals("RUNNING", task.getStatus());
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn test -Dtest=TaskManagerTest`
Expected: FAIL

- [ ] **Step 3: 实现 TaskManager**

```java
package com.filecollection.core;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TaskManager {
    
    private final Map<String, TaskStatus> tasks = new ConcurrentHashMap<>();
    
    public String createTask() {
        String taskId = UUID.randomUUID().toString();
        TaskStatus status = new TaskStatus();
        status.setTaskId(taskId);
        status.setStatus("RUNNING");
        status.setStartTime(LocalDateTime.now());
        tasks.put(taskId, status);
        return taskId;
    }
    
    public TaskStatus getTask(String taskId) {
        return tasks.get(taskId);
    }
    
    public void completeTask(String taskId, int totalFiles, int successCount, int failCount, long totalBytes, long durationMs) {
        TaskStatus task = tasks.get(taskId);
        if (task != null) {
            task.setStatus("SUCCESS");
            task.setEndTime(LocalDateTime.now());
            task.setTotalFiles(totalFiles);
            task.setSuccessCount(successCount);
            task.setFailCount(failCount);
            task.setTotalBytes(totalBytes);
            task.setDurationMs(durationMs);
        }
    }
    
    public void failTask(String taskId, String error) {
        TaskStatus task = tasks.get(taskId);
        if (task != null) {
            task.setStatus("FAILED");
            task.setEndTime(LocalDateTime.now());
            task.setError(error);
        }
    }
    
    @Data
    public static class TaskStatus {
        private String taskId;
        private String status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private int totalFiles;
        private int successCount;
        private int failCount;
        private long totalBytes;
        private long durationMs;
        private String error;
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn test -Dtest=TaskManagerTest`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add src/main/java/com/filecollection/core/TaskManager.java src/test/java/com/filecollection/core/TaskManagerTest.java
git commit -m "feat: implement TaskManager for task status tracking"
```

---

### Task 10: CollectionService 核心编排

**Files:**
- Create: `src/main/java/com/filecollection/core/CollectionService.java`

**Interfaces:**
- Consumes: `FileCollectionProperties`, `FileCopier`, `TaskManager`, `OperationLogMapper`, `FileMetadataMapper`
- Produces: `CollectionService` 类，方法 `executeCollection(List<String> upstreamNames)`

- [ ] **Step 1: 实现 CollectionService**

```java
package com.filecollection.core;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.filecollection.config.FileCollectionProperties;
import com.filecollection.entity.FileMetadata;
import com.filecollection.entity.OperationLog;
import com.filecollection.mapper.FileMetadataMapper;
import com.filecollection.mapper.OperationLogMapper;
import com.filecollection.strategy.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectionService {
    
    private final FileCollectionProperties properties;
    private final FileCopier fileCopier;
    private final TaskManager taskManager;
    private final FileMetadataMapper fileMetadataMapper;
    private final OperationLogMapper operationLogMapper;
    
    public TaskManager.TaskStatus executeCollection(List<String> upstreamNames) {
        String taskId = taskManager.createTask();
        long startTime = System.currentTimeMillis();
        
        int totalFiles = 0;
        int successCount = 0;
        int failCount = 0;
        long totalBytes = 0;
        List<String> errors = new ArrayList<>();
        
        try {
            // 获取要执行的上游列表
            List<FileCollectionProperties.UpstreamConfig> upstreams = properties.getUpstreams();
            if (upstreamNames != null && !upstreamNames.isEmpty()) {
                upstreams = upstreams.stream()
                    .filter(u -> upstreamNames.contains(u.getName()))
                    .toList();
            }
            
            // 遍历每个上游
            for (FileCollectionProperties.UpstreamConfig upstream : upstreams) {
                FileSystemStrategy sourceFs = createFileSystem(upstream);
                
                try {
                    sourceFs.connect();
                    log.info("Connected to upstream: {}", upstream.getName());
                    
                    List<String> files = sourceFs.listFiles(upstream.getPath(), upstream.getFilePattern());
                    log.info("Found {} files in upstream: {}", files.size(), upstream.getName());
                    
                    // 遍历每个文件，拷贝到所有下游
                    for (String fileName : files) {
                        String sourcePath = upstream.getPath() + "/" + fileName;
                        long fileSize = sourceFs.getFileSize(sourcePath);
                        
                        for (FileCollectionProperties.DownstreamConfig downstream : properties.getDownstreams()) {
                            String targetPath = downstream.getPath() + "/" + fileName;
                            
                            try {
                                long copyStart = System.currentTimeMillis();
                                fileCopier.copyFile(sourceFs, sourcePath, new LocalFileSystem(downstream.getPath(), "*.*"), targetPath);
                                long copyDuration = System.currentTimeMillis() - copyStart;
                                
                                // 记录文件元数据
                                FileMetadata metadata = new FileMetadata();
                                metadata.setFileName(fileName);
                                metadata.setFilePath(targetPath);
                                metadata.setFileSize(fileSize);
                                metadata.setUpstreamName(upstream.getName());
                                metadata.setCopyTime(LocalDateTime.now());
                                metadata.setStatus("SUCCESS");
                                fileMetadataMapper.insert(metadata);
                                
                                // 记录操作日志
                                OperationLog opLog = new OperationLog();
                                opLog.setTaskId(taskId);
                                opLog.setUpstreamName(upstream.getName());
                                opLog.setSourcePath(sourcePath);
                                opLog.setTargetPath(targetPath);
                                opLog.setFileSize(fileSize);
                                opLog.setCopyDuration(copyDuration);
                                opLog.setStatus("SUCCESS");
                                opLog.setCreateTime(LocalDateTime.now());
                                operationLogMapper.insert(opLog);
                                
                                successCount++;
                                totalBytes += fileSize;
                                log.debug("File copied: {} -> {} ({}ms)", sourcePath, targetPath, copyDuration);
                                
                            } catch (Exception e) {
                                failCount++;
                                errors.add(fileName + ": " + e.getMessage());
                                log.error("Failed to copy file: {}", sourcePath, e);
                                
                                // 记录失败日志
                                OperationLog opLog = new OperationLog();
                                opLog.setTaskId(taskId);
                                opLog.setUpstreamName(upstream.getName());
                                opLog.setSourcePath(sourcePath);
                                opLog.setTargetPath(targetPath);
                                opLog.setFileSize(fileSize);
                                opLog.setStatus("FAILED");
                                opLog.setErrorMessage(e.getMessage());
                                opLog.setCreateTime(LocalDateTime.now());
                                operationLogMapper.insert(opLog);
                            }
                        }
                        totalFiles++;
                    }
                    
                } catch (Exception e) {
                    log.error("Failed to process upstream: {}", upstream.getName(), e);
                    errors.add(upstream.getName() + ": " + e.getMessage());
                } finally {
                    sourceFs.disconnect();
                }
            }
            
            long durationMs = System.currentTimeMillis() - startTime;
            taskManager.completeTask(taskId, totalFiles, successCount, failCount, totalBytes, durationMs);
            
            log.info("Collection completed: taskId={}, total={}, success={}, fail={}, duration={}ms",
                    taskId, totalFiles, successCount, failCount, durationMs);
            
        } catch (Exception e) {
            taskManager.failTask(taskId, e.getMessage());
            log.error("Collection failed", e);
        }
        
        return taskManager.getTask(taskId);
    }
    
    private FileSystemStrategy createFileSystem(FileCollectionProperties.UpstreamConfig config) {
        return switch (config.getType().toUpperCase()) {
            case "FTP" -> new FtpFileSystem(config.getHost(), config.getPort(), 
                    config.getUsername(), config.getPassword(), config.getPath());
            case "CIFS" -> new CifsFileSystem(config.getHost(), config.getShareName(),
                    config.getUsername(), config.getPassword(), config.getPath());
            case "LOCAL" -> new LocalFileSystem(config.getPath(), config.getFilePattern());
            default -> throw new IllegalArgumentException("Unknown file system type: " + config.getType());
        };
    }
}
```

- [ ] **Step 2: 验证编译**

Run: `mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/filecollection/core/CollectionService.java
git commit -m "feat: implement CollectionService core orchestration"
```

---

### Task 11: REST API Controller

**Files:**
- Create: `src/main/java/com/filecollection/controller/dto/ExecuteRequest.java`
- Create: `src/main/java/com/filecollection/controller/dto/TaskResponse.java`
- Create: `src/main/java/com/filecollection/controller/CollectionController.java`

**Interfaces:**
- Consumes: `CollectionService`, `TaskManager`, `FileCollectionProperties`
- Produces: REST API endpoints

- [ ] **Step 1: 创建 ExecuteRequest DTO**

```java
package com.filecollection.controller.dto;

import lombok.Data;

import java.util.List;

@Data
public class ExecuteRequest {
    private List<String> upstreamNames;
}
```

- [ ] **Step 2: 创建 TaskResponse DTO**

```java
package com.filecollection.controller.dto;

import lombok.Data;

import java.util.List;

@Data
public class TaskResponse<T> {
    private int code;
    private String message;
    private T data;
    
    public static <T> TaskResponse<T> success(T data) {
        TaskResponse<T> response = new TaskResponse<>();
        response.setCode(200);
        response.setMessage("success");
        response.setData(data);
        return response;
    }
    
    public static <T> TaskResponse<T> error(String message) {
        TaskResponse<T> response = new TaskResponse<>();
        response.setCode(500);
        response.setMessage(message);
        return response;
    }
}
```

- [ ] **Step 3: 创建 CollectionController**

```java
package com.filecollection.controller;

import com.filecollection.config.FileCollectionProperties;
import com.filecollection.controller.dto.ExecuteRequest;
import com.filecollection.controller.dto.TaskResponse;
import com.filecollection.core.CollectionService;
import com.filecollection.core.TaskManager;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/collection")
@RequiredArgsConstructor
public class CollectionController {
    
    private final CollectionService collectionService;
    private final TaskManager taskManager;
    private final FileCollectionProperties properties;
    
    @PostMapping("/execute")
    public TaskResponse<Map<String, Object>> execute(@RequestBody(required = false) ExecuteRequest request) {
        try {
            var taskStatus = collectionService.executeCollection(
                request != null ? request.getUpstreamNames() : null
            );
            
            Map<String, Object> data = new HashMap<>();
            data.put("taskId", taskStatus.getTaskId());
            data.put("status", taskStatus.getStatus());
            
            Map<String, Object> summary = new HashMap<>();
            summary.put("totalFiles", taskStatus.getTotalFiles());
            summary.put("successCount", taskStatus.getSuccessCount());
            summary.put("failCount", taskStatus.getFailCount());
            summary.put("totalBytes", taskStatus.getTotalBytes());
            summary.put("durationMs", taskStatus.getDurationMs());
            data.put("summary", summary);
            
            return TaskResponse.success(data);
        } catch (Exception e) {
            return TaskResponse.error(e.getMessage());
        }
    }
    
    @GetMapping("/tasks/{taskId}")
    public TaskResponse<TaskManager.TaskStatus> getTask(@PathVariable String taskId) {
        TaskManager.TaskStatus task = taskManager.getTask(taskId);
        if (task == null) {
            return TaskResponse.error("Task not found: " + taskId);
        }
        return TaskResponse.success(task);
    }
    
    @GetMapping("/config")
    public TaskResponse<Map<String, Object>> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("rateLimit", properties.getRateLimit());
        config.put("upstreamCount", properties.getUpstreams().size());
        config.put("downstreamCount", properties.getDownstreams().size());
        return TaskResponse.success(config);
    }
}
```

- [ ] **Step 4: 验证编译**

Run: `mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add src/main/java/com/filecollection/controller/
git commit -m "feat: implement REST API controller"
```

---

### Task 12: 集成测试

**Files:**
- Create: `src/test/java/com/filecollection/CollectionServiceIntegrationTest.java`

**Interfaces:**
- Consumes: 所有核心组件
- Produces: 集成测试验证完整流程

- [ ] **Step 1: 写集成测试**

```java
package com.filecollection;

import com.filecollection.core.CollectionService;
import com.filecollection.core.TaskManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CollectionServiceIntegrationTest {
    
    @Autowired
    private CollectionService collectionService;
    
    @Test
    void shouldExecuteCollectionFromLocalSource() {
        // Given & When
        TaskManager.TaskStatus result = collectionService.executeCollection(null);
        
        // Then
        assertNotNull(result);
        assertNotNull(result.getTaskId());
        // 实际测试需要配置真实的文件系统路径
    }
}
```

- [ ] **Step 2: 运行测试**

Run: `mvn test -Dtest=CollectionServiceIntegrationTest`
Expected: PASS (需要配置真实环境)

- [ ] **Step 3: 提交**

```bash
git add src/test/java/com/filecollection/CollectionServiceIntegrationTest.java
git commit -m "test: add integration test for collection service"
```

---

## 执行选项

**计划完成并保存到 `docs/superpowers/plans/2026-06-27-file-collection-implementation.md`。**

两种执行方式：

**1. Subagent-Driven（推荐）** - 每个任务分派独立子代理执行，任务间有审查点，快速迭代

**2. Inline Execution** - 在当前会话中使用 executing-plans 批量执行，带检查点

选择哪种方式？
