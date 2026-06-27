package com.filecollection.strategy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FtpFileSystemTest {
    
    @Test
    void shouldCreateFtpFileSystem() {
        // Given & When
        FtpFileSystem fs = new FtpFileSystem("localhost", 21, "user", "pass", "/data");
        
        // Then
        assertNotNull(fs);
    }
    
    @Test
    void shouldThrowExceptionWhenWriteFile() {
        // Given
        FtpFileSystem fs = new FtpFileSystem("localhost", 21, "user", "pass", "/data");
        
        // When & Then
        assertThrows(UnsupportedOperationException.class, 
            () -> fs.writeFile("test.txt", null));
    }
}
