package com.filecollection.strategy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CifsFileSystemTest {
    
    @Test
    void shouldMatchSmbPath() {
        // Given
        String path = "/incoming/data/test.csv";
        
        // When & Then
        assertTrue(CifsFileSystem.isSmbPath(path));
        assertFalse(CifsFileSystem.isSmbPath("/local/path"));
    }
}
