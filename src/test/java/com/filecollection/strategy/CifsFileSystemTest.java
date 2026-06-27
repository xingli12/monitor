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
    
    @Test
    void shouldIdentifyFilesAndDirectories() {
        CifsFileSystem fs = new CifsFileSystem("host", "share", "user", "pass");
        
        @SuppressWarnings("unused")
        class FileEntry {
            public Long getFileAttributes() {
                return 0x00000080L; // FILE_ATTRIBUTE_NORMAL
            }
        }
        @SuppressWarnings("unused")
        class DirectoryEntry {
            public Long getFileAttributes() {
                return 0x00000010L; // FILE_ATTRIBUTE_DIRECTORY
            }
        }
        class BadEntry {
            // does not have getFileAttributes()
        }
        
        assertTrue(fs.isFile(new FileEntry()));
        assertFalse(fs.isFile(new DirectoryEntry()));
        assertTrue(fs.isFile(new BadEntry())); // fallback
    }
}
