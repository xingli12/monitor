package com.filecollection.core;

import com.filecollection.exception.FileSystemException;
import com.filecollection.strategy.FileSystemStrategy;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

@Slf4j
public class FileCopier {
    
    private static final int BUFFER_SIZE = 8192;
    private static final String TEMP_SUFFIX = ".tmp";
    private static final String STRATEGY_OVERWRITE = "OVERWRITE";
    
    private final RateLimiter rateLimiter;
    
    public FileCopier(long bytesPerSecond) {
        this.rateLimiter = RateLimiter.create(bytesPerSecond);
    }
    
    public void copyFile(FileSystemStrategy sourceFs, String sourcePath, 
                         FileSystemStrategy targetFs, String targetPath) {
        copyFile(sourceFs, sourcePath, targetFs, targetPath, STRATEGY_OVERWRITE);
    }
    
    public void copyFile(FileSystemStrategy sourceFs, String sourcePath, 
                         FileSystemStrategy targetFs, String targetPath, String conflictStrategy) {
        log.debug("Copying file: {} -> {}", sourcePath, targetPath);
        
        Path target = Path.of(targetPath);
        Path tempTarget = Path.of(targetPath + TEMP_SUFFIX);
        
        try {
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            
            try (InputStream in = sourceFs.readFile(sourcePath);
                 OutputStream out = Files.newOutputStream(tempTarget, 
                         StandardOpenOption.CREATE, 
                         StandardOpenOption.TRUNCATE_EXISTING)) {
                
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                
                while ((bytesRead = in.read(buffer)) != -1) {
                    rateLimiter.acquire(bytesRead);
                    out.write(buffer, 0, bytesRead);
                }
                
                out.flush();
            }
            
            // 根据冲突策略决定是否覆盖
            if (STRATEGY_OVERWRITE.equalsIgnoreCase(conflictStrategy)) {
                Files.move(tempTarget, target, StandardCopyOption.REPLACE_EXISTING);
            } else {
                // SKIP 策略：如果目标文件已存在则删除临时文件
                if (Files.exists(target)) {
                    cleanupTempFile(tempTarget);
                    log.debug("File skipped (already exists): {}", targetPath);
                    return;
                }
                Files.move(tempTarget, target);
            }
            log.debug("File copied successfully: {}", targetPath);
            
        } catch (Exception e) {
            // 清理临时文件
            cleanupTempFile(tempTarget);
            throw new FileSystemException("Failed to copy file: " + sourcePath, e);
        }
    }
    
    private void cleanupTempFile(Path tempFile) {
        try {
            if (Files.exists(tempFile)) {
                Files.delete(tempFile);
                log.debug("Cleaned up temp file: {}", tempFile);
            }
        } catch (IOException ex) {
            log.warn("Failed to cleanup temp file: {}", tempFile, ex);
        }
    }
}
