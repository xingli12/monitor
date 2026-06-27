package com.filecollection.strategy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
        InputStream inputStream = new java.io.ByteArrayInputStream(content.getBytes());
        
        // When
        String targetPath = tempDir.resolve("output.txt").toString();
        fs.writeFile(targetPath, inputStream);
        
        // Then
        assertTrue(Files.exists(Path.of(targetPath)));
        String result = Files.readString(Path.of(targetPath));
        assertEquals(content, result);
    }
}
