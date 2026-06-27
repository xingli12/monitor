package com.filecollection.core;

import com.filecollection.strategy.FileSystemStrategy;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Slf4j
public class FileCopier {
    
    private static final int BUFFER_SIZE = 8192;
    
    private final RateLimiter rateLimiter;
    
    public FileCopier(long bytesPerSecond) {
        this.rateLimiter = RateLimiter.create(bytesPerSecond);
    }
    
    public void copyFile(FileSystemStrategy sourceFs, String sourcePath, 
                         FileSystemStrategy targetFs, String targetPath) {
        log.debug("Copying file: {} -> {}", sourcePath, targetPath);
        
        try (InputStream in = sourceFs.readFile(sourcePath)) {
            Path target = Path.of(targetPath);
            Files.createDirectories(target.getParent());
            
            try (OutputStream out = Files.newOutputStream(target, 
                    StandardOpenOption.CREATE, 
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                
                while ((bytesRead = in.read(buffer)) != -1) {
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
