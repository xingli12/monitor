package com.filecollection.strategy;

import com.filecollection.exception.FileSystemException;
import com.filecollection.util.GlobUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Stream;

public class LocalFileSystem implements FileSystemStrategy {
    
    private final String basePath;
    
    public LocalFileSystem(String basePath) {
        this.basePath = basePath;
    }
    
    @Override
    public void connect() {
        validatePath(basePath, "Base path does not exist");
    }
    
    @Override
    public List<String> listFiles(String path, String pattern) {
        validatePath(path, "Path does not exist");
        
        try (Stream<Path> stream = Files.list(Path.of(path))) {
            return stream
                .filter(Files::isRegularFile)
                .map(p -> p.getFileName().toString())
                .filter(name -> matchGlob(name, pattern))
                .toList();
        } catch (IOException e) {
            throw new FileSystemException("Failed to list files at: " + path, e);
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
    
    private void validatePath(String path, String message) {
        if (!Files.exists(Path.of(path))) {
            throw new FileSystemException(message + ": " + path);
        }
    }
    
    private boolean matchGlob(String name, String pattern) {
        return GlobUtils.matchGlob(name, pattern);
    }
}
