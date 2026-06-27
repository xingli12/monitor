package com.filecollection.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GlobUtilsTest {
    
    @Test
    void shouldMatchSimpleWildcard() {
        assertTrue(GlobUtils.matchGlob("test.csv", "*.csv"));
        assertTrue(GlobUtils.matchGlob("data.txt", "*.txt"));
        assertFalse(GlobUtils.matchGlob("test.csv", "*.txt"));
    }
    
    @Test
    void shouldMatchQuestionMark() {
        assertTrue(GlobUtils.matchGlob("a1b", "a?b"));
        assertTrue(GlobUtils.matchGlob("a2b", "a?b"));
        assertFalse(GlobUtils.matchGlob("ab", "a?b"));
        assertFalse(GlobUtils.matchGlob("a12b", "a?b"));
    }
    
    @Test
    void shouldMatchBracketExpression() {
        assertTrue(GlobUtils.matchGlob("a", "[abc]"));
        assertTrue(GlobUtils.matchGlob("b", "[abc]"));
        assertTrue(GlobUtils.matchGlob("c", "[abc]"));
        assertFalse(GlobUtils.matchGlob("d", "[abc]"));
    }
    
    @Test
    void shouldMatchBracketRange() {
        assertTrue(GlobUtils.matchGlob("5", "[0-9]"));
        assertTrue(GlobUtils.matchGlob("a", "[a-z]"));
        assertFalse(GlobUtils.matchGlob("A", "[a-z]"));
        assertFalse(GlobUtils.matchGlob("!", "[0-9]"));
    }
    
    @Test
    void shouldMatchNegatedBracket() {
        assertTrue(GlobUtils.matchGlob("d", "[^abc]"));
        assertFalse(GlobUtils.matchGlob("a", "[^abc]"));
    }
    
    @Test
    void shouldMatchPosixNegatedBracket() {
        assertTrue(GlobUtils.matchGlob("d", "[!abc]"));
        assertFalse(GlobUtils.matchGlob("a", "[!abc]"));
    }
    
    @Test
    void shouldMatchComplexPattern() {
        assertTrue(GlobUtils.matchGlob("report_2024.csv", "report_*.csv"));
        assertTrue(GlobUtils.matchGlob("data_v2.txt", "data_v?.txt"));
        assertFalse(GlobUtils.matchGlob("data_v22.txt", "data_v?.txt"));
    }
    
    @Test
    void shouldMatchExactFileName() {
        assertTrue(GlobUtils.matchGlob("test.csv", "test.csv"));
        assertFalse(GlobUtils.matchGlob("test.txt", "test.csv"));
    }
    
    @Test
    void shouldMatchAllFiles() {
        assertTrue(GlobUtils.matchGlob("anything", "*"));
        assertTrue(GlobUtils.matchGlob("test.csv", "*"));
    }
    
    @Test
    void shouldHandleEmptyPattern() {
        assertFalse(GlobUtils.matchGlob("test", ""));
    }
    
    @Test
    void shouldHandleSpecialCharactersInFileName() {
        assertTrue(GlobUtils.matchGlob("report(1).csv", "report*.csv"));
        assertTrue(GlobUtils.matchGlob("data+v2.csv", "data*.csv"));
        assertTrue(GlobUtils.matchGlob("file.name.txt", "file*.txt"));
    }
    
    @Test
    void shouldMatchWithMultipleWildcards() {
        assertTrue(GlobUtils.matchGlob("abc123def.csv", "*123*.csv"));
        assertTrue(GlobUtils.matchGlob("abc.csv", "a*c.csv"));
    }
    
    @TempDir
    Path tempDir;
    
    @Test
    void shouldFilterFilesWithGlob() throws Exception {
        // Given
        Files.createFile(tempDir.resolve("test.csv"));
        Files.createFile(tempDir.resolve("data.txt"));
        Files.createFile(tempDir.resolve("report.csv"));
        Files.createFile(tempDir.resolve("image.png"));
        
        // When
        List<String> files = Files.list(tempDir)
            .map(p -> p.getFileName().toString())
            .filter(name -> GlobUtils.matchGlob(name, "*.csv"))
            .toList();
        
        // Then
        assertEquals(2, files.size());
        assertTrue(files.contains("test.csv"));
        assertTrue(files.contains("report.csv"));
    }
}
