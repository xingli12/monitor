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
    
    private static final int MIN_LIST_COLUMNS = 8;
    
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
    public void connect() throws com.filecollection.exception.FileSystemException {
        try {
            ftpClient = new FTPClient();
            ftpClient.connect(host, port);
            ftpClient.login(username, password);
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
            log.info("Connected to FTP server: {}:{}", host, port);
        } catch (IOException e) {
            throw new com.filecollection.exception.FileSystemException("Failed to connect to FTP server: " + host, e);
        }
    }
    
    @Override
    public List<String> listFiles(String path, String pattern) throws com.filecollection.exception.FileSystemException {
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
            throw new com.filecollection.exception.FileSystemException("Failed to list files at: " + path, e);
        }
    }
    
    @Override
    public InputStream readFile(String filePath) throws com.filecollection.exception.FileSystemException {
        try {
            return ftpClient.retrieveFileStream(filePath);
        } catch (IOException e) {
            throw new com.filecollection.exception.FileSystemException("Failed to read file: " + filePath, e);
        }
    }
    
    @Override
    public void writeFile(String targetPath, InputStream content) throws com.filecollection.exception.FileSystemException {
        throw new UnsupportedOperationException("FTP upstream does not support write");
    }
    
    @Override
    public long getFileSize(String filePath) throws com.filecollection.exception.FileSystemException {
        try {
            FTPFile[] files = ftpClient.listFiles(filePath);
            return files.length > 0 ? files[0].getSize() : 0;
        } catch (IOException e) {
            throw new com.filecollection.exception.FileSystemException("Failed to get file size: " + filePath, e);
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
            log.error("Error disconnecting from FTP server: {}", host, e);
        }
    }
    
    public static List<String> parseFileList(String listing) {
        List<String> files = new ArrayList<>();
        String[] lines = listing.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty() && !line.startsWith("total")) {
                String[] parts = line.split("\\s+");
                if (parts.length >= MIN_LIST_COLUMNS) {
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
