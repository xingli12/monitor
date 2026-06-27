package com.filecollection.strategy;

import com.filecollection.exception.FileSystemException;

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
