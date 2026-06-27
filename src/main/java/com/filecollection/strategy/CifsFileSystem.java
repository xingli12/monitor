package com.filecollection.strategy;

import com.filecollection.util.GlobUtils;
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
    
    private static final String UNC_PREFIX = "\\\\";
    private static final String SMB_PREFIX = "smb://";
    private static final String CURRENT_DIR = ".";
    private static final String PARENT_DIR = "..";
    
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
    public void connect() throws com.filecollection.exception.FileSystemException {
        try {
            client = new SMBClient();
            session = client.connect(host).authenticate(new AuthenticationContext(username, password.toCharArray(), null));
            share = (DiskShare) session.connectShare(shareName);
            log.info("Connected to CIFS share: {} at {}", shareName, host);
        } catch (IOException e) {
            throw new com.filecollection.exception.FileSystemException("Failed to connect to CIFS server: " + host, e);
        }
    }
    
    @Override
    public List<String> listFiles(String path, String pattern) throws com.filecollection.exception.FileSystemException {
        List<String> files = new ArrayList<>();
        try (com.hierynomus.smbj.share.Directory dir = share.openDirectory(path, EnumSet.of(AccessMask.GENERIC_READ), null, EnumSet.of(SMB2ShareAccess.FILE_SHARE_READ), null, null)) {
            List<?> entries = dir.list();
            for (Object entry : entries) {
                String fileName = extractFileName(entry);
                if (fileName == null || isSpecialDirectory(fileName)) {
                    continue;
                }
                if (isFile(entry) && matchGlob(fileName, pattern)) {
                    files.add(fileName);
                }
            }
        } catch (Exception e) {
            throw new com.filecollection.exception.FileSystemException("Failed to list files at: " + path, e);
        }
        return files;
    }
    
    @Override
    public InputStream readFile(String filePath) throws com.filecollection.exception.FileSystemException {
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
        } catch (Exception e) {
            throw new com.filecollection.exception.FileSystemException("Failed to read file: " + filePath, e);
        }
    }
    
    @Override
    public void writeFile(String targetPath, InputStream content) throws com.filecollection.exception.FileSystemException {
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
        } catch (Exception e) {
            throw new com.filecollection.exception.FileSystemException("Failed to write file: " + targetPath, e);
        }
    }
    
    @Override
    public long getFileSize(String filePath) throws com.filecollection.exception.FileSystemException {
        try (File smbFile = share.openFile(
                filePath,
                EnumSet.of(AccessMask.GENERIC_READ),
                null,
                EnumSet.of(SMB2ShareAccess.FILE_SHARE_READ),
                SMB2CreateDisposition.FILE_OPEN,
                null
            )) {
            return smbFile.getFileInformation().getStandardInformation().getEndOfFile();
        } catch (Exception e) {
            throw new com.filecollection.exception.FileSystemException("Failed to get file size: " + filePath, e);
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
            log.error("Error disconnecting from CIFS server: {}", shareName, e);
        }
    }
    
    public static boolean isSmbPath(String path) {
        return path.startsWith(UNC_PREFIX) || path.startsWith(SMB_PREFIX);
    }
    
    private String extractFileName(Object entry) {
        try {
            var method = entry.getClass().getMethod("getFileName");
            return (String) method.invoke(entry);
        } catch (Exception e) {
            return null;
        }
    }
    
    private boolean isSpecialDirectory(String fileName) {
        return CURRENT_DIR.equals(fileName) || PARENT_DIR.equals(fileName);
    }
    
    private boolean isFile(Object entry) {
        try {
            var attrsMethod = entry.getClass().getMethod("getFileAttributes");
            var attrs = attrsMethod.invoke(entry);
            if (attrs != null) {
                var isDirMethod = attrs.getClass().getMethod("isDirectory");
                return !(Boolean) isDirMethod.invoke(attrs);
            }
        } catch (Exception e) {
            // 如果无法判断，假设是文件
        }
        return true;
    }
    
    private boolean matchGlob(String name, String pattern) {
        return GlobUtils.matchGlob(name, pattern);
    }
}
