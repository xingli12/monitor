package com.filecollection.strategy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CifsFileSystemTest {
    
    @Test
    void shouldMatchSmbPath() {
        // Given
        String uncPath = "\\\\server\\share\\data\\test.csv";
        String smbUri = "smb://server/share/data/test.csv";
        String localPath = "/local/path";
        
        // When & Then
        assertTrue(CifsFileSystem.isSmbPath(uncPath));
        assertTrue(CifsFileSystem.isSmbPath(smbUri));
        assertFalse(CifsFileSystem.isSmbPath(localPath));
    }
}
