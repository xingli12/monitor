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
    
    @Test
    void shouldHandleNullFileListGracefully() throws Exception {
        // Given
        FtpFileSystem fs = new FtpFileSystem("localhost", 21, "user", "pass", "/data");
        org.apache.commons.net.ftp.FTPClient mockClient = org.mockito.Mockito.mock(org.apache.commons.net.ftp.FTPClient.class);
        org.mockito.Mockito.when(mockClient.listFiles(org.mockito.Mockito.anyString())).thenReturn(null);
        
        // Inject mockClient into fs
        java.lang.reflect.Field field = FtpFileSystem.class.getDeclaredField("ftpClient");
        field.setAccessible(true);
        field.set(fs, mockClient);
        
        // When & Then
        java.util.List<String> files = fs.listFiles("/data", "*.*");
        assertNotNull(files);
        assertTrue(files.isEmpty());
        
        long size = fs.getFileSize("/data/file.txt");
        assertEquals(0, size);
    }
}
