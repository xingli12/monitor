package com.filecollection.core;

import com.filecollection.strategy.LocalFileSystem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
}
