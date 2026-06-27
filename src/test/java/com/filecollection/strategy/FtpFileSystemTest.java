package com.filecollection.strategy;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FtpFileSystemTest {
    
    @Test
    void shouldParseFtpFileList() throws Exception {
        // Given
        String listing = "-rw-r--r-- 1 user group 1024 Jun 27 10:00 test.csv\n"
                       + "-rw-r--r-- 1 user group 2048 Jun 27 11:00 data.txt";
        
        // When
        List<String> files = FtpFileSystem.parseFileList(listing);
        
        // Then
        assertEquals(2, files.size());
        assertTrue(files.contains("test.csv"));
        assertTrue(files.contains("data.txt"));
    }
}
