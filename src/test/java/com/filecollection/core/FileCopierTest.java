package com.filecollection.core;

import com.filecollection.strategy.LocalFileSystem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileCopierTest {
    
    @TempDir
    Path tempDir;
    
    @Test
    void shouldCopyFileWithRateLimit() throws Exception {
        // Given
        Path source = tempDir.resolve("source.txt");
        Files.writeString(source, "test content");
        
        Path target = tempDir.resolve("target.txt");
        
        LocalFileSystem sourceFs = new LocalFileSystem(tempDir.toString(), "*.*");
        LocalFileSystem targetFs = new LocalFileSystem(tempDir.toString(), "*.*");
        
        FileCopier copier = new FileCopier(1024 * 1024); // 1MB/s
        
        // When
        copier.copyFile(sourceFs, source.toString(), targetFs, target.toString());
        
        // Then
        assertTrue(Files.exists(target));
        assertEquals("test content", Files.readString(target));
    }
    
    @Test
    void shouldCopyEmptyFile() throws Exception {
        // Given
        Path source = tempDir.resolve("empty.txt");
        Files.writeString(source, "");
        
        Path target = tempDir.resolve("empty_target.txt");
        
        LocalFileSystem sourceFs = new LocalFileSystem(tempDir.toString(), "*.*");
        LocalFileSystem targetFs = new LocalFileSystem(tempDir.toString(), "*.*");
        
        FileCopier copier = new FileCopier(1024 * 1024);
        
        // When
        copier.copyFile(sourceFs, source.toString(), targetFs, target.toString());
        
        // Then
        assertTrue(Files.exists(target));
        assertEquals("", Files.readString(target));
    }
    
    @Test
    void shouldCopyLargeFile() throws Exception {
        // Given
        Path source = tempDir.resolve("large.txt");
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeContent.append("Line ").append(i).append(": This is a test line for large file copy.\n");
        }
        Files.writeString(source, largeContent.toString());
        
        Path target = tempDir.resolve("large_target.txt");
        
        LocalFileSystem sourceFs = new LocalFileSystem(tempDir.toString(), "*.*");
        LocalFileSystem targetFs = new LocalFileSystem(tempDir.toString(), "*.*");
        
        FileCopier copier = new FileCopier(1024 * 1024);
        
        // When
        copier.copyFile(sourceFs, source.toString(), targetFs, target.toString());
        
        // Then
        assertTrue(Files.exists(target));
        assertEquals(Files.size(source), Files.size(target));
    }
    
    @Test
    void shouldThrowExceptionWhenSourceFileNotFound() {
        // Given
        LocalFileSystem sourceFs = new LocalFileSystem(tempDir.toString(), "*.*");
        LocalFileSystem targetFs = new LocalFileSystem(tempDir.toString(), "*.*");
        
        FileCopier copier = new FileCopier(1024 * 1024);
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> copier.copyFile(sourceFs, "/non/existent/file.txt", targetFs, tempDir.resolve("target.txt").toString()));
        assertTrue(exception.getMessage().contains("Failed to copy file"));
    }
    
    @Test
    void shouldCreateTargetDirectoryIfNotExists() throws Exception {
        // Given
        Path source = tempDir.resolve("source.txt");
        Files.writeString(source, "content");
        
        Path target = tempDir.resolve("newdir/subdir/target.txt");
        
        LocalFileSystem sourceFs = new LocalFileSystem(tempDir.toString(), "*.*");
        LocalFileSystem targetFs = new LocalFileSystem(tempDir.toString(), "*.*");
        
        FileCopier copier = new FileCopier(1024 * 1024);
        
        // When
        copier.copyFile(sourceFs, source.toString(), targetFs, target.toString());
        
        // Then
        assertTrue(Files.exists(target));
        assertEquals("content", Files.readString(target));
    }
    
    @Test
    void shouldOverwriteExistingTargetFile() throws Exception {
        // Given
        Path source = tempDir.resolve("source.txt");
        Files.writeString(source, "new content");
        
        Path target = tempDir.resolve("target.txt");
        Files.writeString(target, "old content");
        
        LocalFileSystem sourceFs = new LocalFileSystem(tempDir.toString(), "*.*");
        LocalFileSystem targetFs = new LocalFileSystem(tempDir.toString(), "*.*");
        
        FileCopier copier = new FileCopier(1024 * 1024);
        
        // When
        copier.copyFile(sourceFs, source.toString(), targetFs, target.toString());
        
        // Then
        assertEquals("new content", Files.readString(target));
    }
    
    @Test
    void shouldCopyBinaryFile() throws Exception {
        // Given
        Path source = tempDir.resolve("binary.dat");
        byte[] binaryData = new byte[256];
        for (int i = 0; i < 256; i++) {
            binaryData[i] = (byte) i;
        }
        Files.write(source, binaryData);
        
        Path target = tempDir.resolve("binary_target.dat");
        
        LocalFileSystem sourceFs = new LocalFileSystem(tempDir.toString(), "*.*");
        LocalFileSystem targetFs = new LocalFileSystem(tempDir.toString(), "*.*");
        
        FileCopier copier = new FileCopier(1024 * 1024);
        
        // When
        copier.copyFile(sourceFs, source.toString(), targetFs, target.toString());
        
        // Then
        assertTrue(Files.exists(target));
        byte[] result = Files.readAllBytes(target);
        assertArrayEquals(binaryData, result);
    }
    
    @Test
    void shouldUpdateRateLimitDynamically() {
        // Given
        FileCopier copier = new FileCopier(1024 * 1024);
        
        // When & Then
        assertDoesNotThrow(() -> copier.setRateLimit(2048 * 1024));
    }
}
