package com.filecollection.core;

import com.filecollection.strategy.FileSystemStrategy;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.io.OutputStream;

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
        
        try (InputStream in = sourceFs.readFile(sourcePath)) {
            // 创建目标文件的输出流
            java.nio.file.Path target = java.nio.file.Path.of(targetPath);
            java.nio.file.Files.createDirectories(target.getParent());
            
            try (OutputStream out = java.nio.file.Files.newOutputStream(target, 
                    java.nio.file.StandardOpenOption.CREATE, 
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)) {
                
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                
                while ((bytesRead = in.read(buffer)) != -1) {
                    // 限速：等待发送这些字节的配额
                    rateLimiter.acquire(bytesRead);
                    out.write(buffer, 0, bytesRead);
                }
                
                out.flush();
                log.debug("File copied successfully: {}", targetPath);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to copy file: " + sourcePath, e);
        }
    }
}
