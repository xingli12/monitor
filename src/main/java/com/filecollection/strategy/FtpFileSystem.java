package com.filecollection.strategy;

import com.filecollection.util.GlobUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
public class FtpFileSystem implements FileSystemStrategy {
    
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    
    private FTPClient ftpClient;
    
    public FtpFileSystem(String host, int port, String username, String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }
    
    @Override
    public void connect() throws com.filecollection.exception.FileSystemException {
        try {
            ftpClient = new FTPClient();
            ftpClient.connect(host, port);
            
            boolean loginSuccess = ftpClient.login(username, password);
            if (!loginSuccess) {
                throw new com.filecollection.exception.FileSystemException(
                    "FTP login failed for user: " + username + " at " + host);
            }
            
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
            log.info("Connected to FTP server: {}:{}", host, port);
        } catch (IOException e) {
            throw new com.filecollection.exception.FileSystemException("Failed to connect to FTP server: " + host, e);
        }
    }
    
    @Override
    public java.util.List<String> listFiles(String path, String pattern) throws com.filecollection.exception.FileSystemException {
        try {
            FTPFile[] ftpFiles = ftpClient.listFiles(path);
            java.util.List<String> files = new java.util.ArrayList<>();
            
            if (ftpFiles != null) {
                for (FTPFile ftpFile : ftpFiles) {
                    if (ftpFile.isFile() && matchGlob(ftpFile.getName(), pattern)) {
                        files.add(ftpFile.getName());
                    }
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
            InputStream inputStream = ftpClient.retrieveFileStream(filePath);
            if (inputStream == null) {
                throw new com.filecollection.exception.FileSystemException(
                    "Failed to retrieve file: " + filePath + " - " + ftpClient.getReplyString());
            }
            return new FtpInputStream(inputStream, ftpClient);
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
            return (files != null && files.length > 0) ? files[0].getSize() : 0;
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
    
    private boolean matchGlob(String name, String pattern) {
        return GlobUtils.matchGlob(name, pattern);
    }
    
    /**
     * FTP InputStream wrapper that calls completePendingCommand on close.
     */
    private static class FtpInputStream extends java.io.FilterInputStream {
        private final FTPClient client;
        
        FtpInputStream(InputStream delegate, FTPClient client) {
            super(delegate);
            this.client = client;
        }
        
        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                client.completePendingCommand();
            }
        }
    }
}
