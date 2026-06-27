package com.filecollection.strategy;

import com.filecollection.exception.FileSystemException;

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
