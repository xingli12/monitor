package com.filecollection.strategy;

import com.filecollection.exception.FileSystemException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
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
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        
        // When
        String targetPath = tempDir.resolve("output.txt").toString();
        fs.writeFile(targetPath, inputStream);
        
        // Then
        assertTrue(Files.exists(Path.of(targetPath)));
        String result = Files.readString(Path.of(targetPath));
        assertEquals(content, result);
    }
    
    @Test
    void shouldConnectSuccessfully() {
        // Given
        LocalFileSystem fs = new LocalFileSystem(tempDir.toString(), "*.*");
        
        // When & Then - 不抛异常即为成功
        assertDoesNotThrow(fs::connect);
    }
    
    @Test
    void shouldThrowExceptionWhenConnectToNonExistentPath() {
        // Given
        LocalFileSystem fs = new LocalFileSystem("/non/existent/path", "*.*");
        
        // When & Then
        FileSystemException exception = assertThrows(FileSystemException.class, fs::connect);
        assertTrue(exception.getMessage().contains("Base path does not exist"));
    }
    
    @Test
    void shouldReadFileSuccessfully() throws Exception {
        // Given
        Path file = tempDir.resolve("readme.txt");
        String content = "hello world";
        Files.writeString(file, content);
        
        LocalFileSystem fs = new LocalFileSystem(tempDir.toString(), "*.*");
        
        // When
        InputStream inputStream = fs.readFile(file.toString());
        String result = new String(inputStream.readAllBytes());
        
        // Then
        assertEquals(content, result);
    }
    
    @Test
    void shouldThrowExceptionWhenReadNonExistentFile() {
        // Given
        LocalFileSystem fs = new LocalFileSystem(tempDir.toString(), "*.*");
        
        // When & Then
        FileSystemException exception = assertThrows(FileSystemException.class, 
            () -> fs.readFile("/non/existent/file.txt"));
        assertTrue(exception.getMessage().contains("Failed to read file"));
    }
    
    @Test
    void shouldGetFileSizeSuccessfully() throws Exception {
        // Given
        Path file = tempDir.resolve("size_test.txt");
        String content = "12345";
        Files.writeString(file, content);
        
        LocalFileSystem fs = new LocalFileSystem(tempDir.toString(), "*.*");
        
        // When
        long size = fs.getFileSize(file.toString());
        
        // Then
        assertEquals(5, size);
    }
    
    @Test
    void shouldThrowExceptionWhenGetSizeOfNonExistentFile() {
        // Given
        LocalFileSystem fs = new LocalFileSystem(tempDir.toString(), "*.*");
        
        // When & Then
        FileSystemException exception = assertThrows(FileSystemException.class, 
            () -> fs.getFileSize("/non/existent/file.txt"));
        assertTrue(exception.getMessage().contains("Failed to get file size"));
    }
    
    @Test
    void shouldThrowExceptionWhenListFilesInNonExistentPath() {
        // Given
        LocalFileSystem fs = new LocalFileSystem(tempDir.toString(), "*.*");
        
        // When & Then
        FileSystemException exception = assertThrows(FileSystemException.class, 
            () -> fs.listFiles("/non/existent/dir", "*.*"));
        assertTrue(exception.getMessage().contains("Path does not exist"));
    }
    
    @Test
    void shouldDisconnectWithoutError() {
        // Given
        LocalFileSystem fs = new LocalFileSystem(tempDir.toString(), "*.*");
        
        // When & Then - disconnect 是空操作，不应抛异常
        assertDoesNotThrow(fs::disconnect);
    }
    
    @Test
    void shouldWriteFileToNewDirectory() throws Exception {
        // Given
        LocalFileSystem fs = new LocalFileSystem(tempDir.toString(), "*.*");
        String content = "new dir content";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        String targetPath = tempDir.resolve("subdir/nested/file.txt").toString();
        
        // When
        fs.writeFile(targetPath, inputStream);
        
        // Then
        assertTrue(Files.exists(Path.of(targetPath)));
        assertEquals(content, Files.readString(Path.of(targetPath)));
    }
    
    @Test
    void shouldOverwriteExistingFile() throws Exception {
        // Given
        LocalFileSystem fs = new LocalFileSystem(tempDir.toString(), "*.*");
        Path file = tempDir.resolve("overwrite.txt");
        Files.writeString(file, "old content");
        
        InputStream inputStream = new ByteArrayInputStream("new content".getBytes());
        
        // When
        fs.writeFile(file.toString(), inputStream);
        
        // Then
        assertEquals("new content", Files.readString(file));
    }
}
