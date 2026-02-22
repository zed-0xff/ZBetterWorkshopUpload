package me.zed_0xff.zbetter_workshop_upload;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;

import zombie.core.znet.SteamWorkshopItem;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DescriptionIncludeProcessor.
 */
public class DescriptionIncludeProcessorTest {
    
    @TempDir
    Path tempDir;
    
    private TestSteamWorkshopItem workshopItem;
    private Path workshopFolder;
    
    @BeforeEach
    void setUp() throws Exception {
        workshopFolder = tempDir.resolve("workshop");
        Files.createDirectories(workshopFolder);
        
        workshopItem = new TestSteamWorkshopItem(workshopFolder.toString());
    }
    
    @AfterEach
    void tearDown() {
        workshopItem = null;
    }
    
    @Test
    void testNullDescription() {
        String result = DescriptionIncludeProcessor.expandIncludes(null, workshopItem);
        assertNull(result);
    }
    
    @Test
    void testEmptyDescription() {
        String result = DescriptionIncludeProcessor.expandIncludes("", workshopItem);
        assertEquals("", result);
    }
    
    @Test
    void testDescriptionWithoutIncludes() {
        String description = "This is a regular description\nWith multiple lines";
        String result = DescriptionIncludeProcessor.expandIncludes(description, workshopItem);
        assertEquals(description, result);
    }
    
    @Test
    void testSingleInclude() throws IOException {
        // Create test file
        Path testFile = workshopFolder.resolve("test.txt");
        Files.write(testFile, "File content".getBytes(StandardCharsets.UTF_8));
        
        String description = "@include(\"test.txt\")";
        String result = DescriptionIncludeProcessor.expandIncludes(description, workshopItem);
        
        assertEquals("File content", result);
    }
    
    @Test
    void testMultipleIncludes() throws IOException {
        // Create test files
        Path file1 = workshopFolder.resolve("file1.txt");
        Path file2 = workshopFolder.resolve("file2.txt");
        Files.write(file1, "Content 1".getBytes(StandardCharsets.UTF_8));
        Files.write(file2, "Content 2".getBytes(StandardCharsets.UTF_8));
        
        String description = "@include(\"file1.txt\")\n@include(\"file2.txt\")";
        String result = DescriptionIncludeProcessor.expandIncludes(description, workshopItem);
        
        assertEquals("Content 1\nContent 2", result);
    }
    
    @Test
    void testIncludeWithOtherText() throws IOException {
        // Create test file
        Path testFile = workshopFolder.resolve("test.txt");
        Files.write(testFile, "File content".getBytes(StandardCharsets.UTF_8));
        
        String description = "Before\n@include(\"test.txt\")\nAfter";
        String result = DescriptionIncludeProcessor.expandIncludes(description, workshopItem);
        
        assertEquals("Before\nFile content\nAfter", result);
    }
    
    @Test
    void testIncludeWithMultilineFile() throws IOException {
        // Create test file with multiple lines
        Path testFile = workshopFolder.resolve("test.txt");
        Files.write(testFile, "Line 1\nLine 2\nLine 3".getBytes(StandardCharsets.UTF_8));
        
        String description = "@include(\"test.txt\")";
        String result = DescriptionIncludeProcessor.expandIncludes(description, workshopItem);
        
        assertEquals("Line 1\nLine 2\nLine 3", result);
    }
    
    @Test
    void testIncludeWithWhitespace() throws IOException {
        // Create test file
        Path testFile = workshopFolder.resolve("test.txt");
        Files.write(testFile, "File content".getBytes(StandardCharsets.UTF_8));
        
        // Test with leading whitespace
        String description = "   @include(\"test.txt\")";
        String result = DescriptionIncludeProcessor.expandIncludes(description, workshopItem);
        
        assertEquals("File content", result);
    }
    
    @Test
    void testInvalidIncludeFormat() {
        String description = "@include \"test.txt\"";
        String result = DescriptionIncludeProcessor.expandIncludes(description, workshopItem);
        
        // Should remain unchanged
        assertEquals(description, result);
    }
    
    @Test
    void testInvalidIncludeFormatNoQuotes() {
        String description = "@include(test.txt)";
        String result = DescriptionIncludeProcessor.expandIncludes(description, workshopItem);
        
        // Should remain unchanged
        assertEquals(description, result);
    }
    
    @Test
    void testInvalidIncludeFormatNoParentheses() {
        String description = "@include \"test.txt\"";
        String result = DescriptionIncludeProcessor.expandIncludes(description, workshopItem);
        
        // Should remain unchanged
        assertEquals(description, result);
    }
    
    @Test
    void testIncludeNonExistentFile() {
        String description = "@include(\"nonexistent.txt\")";
        String result = DescriptionIncludeProcessor.expandIncludes(description, workshopItem);
        
        // Should remain unchanged (include failed)
        assertEquals(description, result);
    }
    
    @Test
    void testIncludeWithDoubleDot() throws IOException {
        // Create test file
        Path testFile = workshopFolder.resolve("test.txt");
        Files.write(testFile, "File content".getBytes(StandardCharsets.UTF_8));
        
        // Try to include with path traversal
        String description = "@include(\"../test.txt\")";
        String result = DescriptionIncludeProcessor.expandIncludes(description, workshopItem);
        
        // Should remain unchanged (security check failed)
        assertEquals(description, result);
    }
    
    @Test
    void testIncludeWithAbsolutePath() throws IOException {
        // Create test file
        Path testFile = workshopFolder.resolve("test.txt");
        Files.write(testFile, "File content".getBytes(StandardCharsets.UTF_8));
        
        // Try to include with absolute path
        String absolutePath = testFile.toAbsolutePath().toString();
        String description = "@include(\"" + absolutePath + "\")";
        String result = DescriptionIncludeProcessor.expandIncludes(description, workshopItem);
        
        // Should remain unchanged (security check failed)
        assertEquals(description, result);
    }
    
    @Test
    void testIncludeDirectoryInsteadOfFile() throws IOException {
        // Create a directory instead of a file
        Path testDir = workshopFolder.resolve("testdir");
        Files.createDirectories(testDir);
        
        String description = "@include(\"testdir\")";
        String result = DescriptionIncludeProcessor.expandIncludes(description, workshopItem);
        
        // Should remain unchanged (not a file)
        assertEquals(description, result);
    }
    
    @Test
    void testIncludeWithSubdirectory() throws IOException {
        // Create subdirectory and file
        Path subdir = workshopFolder.resolve("subdir");
        Files.createDirectories(subdir);
        Path testFile = subdir.resolve("test.txt");
        Files.write(testFile, "File content".getBytes(StandardCharsets.UTF_8));
        
        String description = "@include(\"subdir/test.txt\")";
        String result = DescriptionIncludeProcessor.expandIncludes(description, workshopItem);
        
        assertEquals("File content", result);
    }
    
    @Test
    void testIncludeWithEmptyFilename() {
        String description = "@include(\"\")";
        String result = DescriptionIncludeProcessor.expandIncludes(description, workshopItem);
        
        // Should remain unchanged
        assertEquals(description, result);
    }
    
    @Test
    void testIncludeWithSpecialCharactersInFilename() throws IOException {
        // Create test file with special characters in name
        Path testFile = workshopFolder.resolve("test-file_123.txt");
        Files.write(testFile, "File content".getBytes(StandardCharsets.UTF_8));
        
        String description = "@include(\"test-file_123.txt\")";
        String result = DescriptionIncludeProcessor.expandIncludes(description, workshopItem);
        
        assertEquals("File content", result);
    }
    
    @Test
    void testIncludeWithWindowsLineEndings() throws IOException {
        // Create test file
        Path testFile = workshopFolder.resolve("test.txt");
        Files.write(testFile, "File content".getBytes(StandardCharsets.UTF_8));
        
        String description = "@include(\"test.txt\")\r\nNext line";
        String result = DescriptionIncludeProcessor.expandIncludes(description, workshopItem);
        
        assertEquals("File content\nNext line", result);
    }
    
    @Test
    void testIncludeWithUnixLineEndings() throws IOException {
        // Create test file
        Path testFile = workshopFolder.resolve("test.txt");
        Files.write(testFile, "File content".getBytes(StandardCharsets.UTF_8));
        
        String description = "@include(\"test.txt\")\nNext line";
        String result = DescriptionIncludeProcessor.expandIncludes(description, workshopItem);
        
        assertEquals("File content\nNext line", result);
    }
    
    @Test
    void testMultipleIncludesWithMixedContent() throws IOException {
        // Create test files
        Path file1 = workshopFolder.resolve("file1.txt");
        Path file2 = workshopFolder.resolve("file2.txt");
        Files.write(file1, "Content 1".getBytes(StandardCharsets.UTF_8));
        Files.write(file2, "Content 2".getBytes(StandardCharsets.UTF_8));
        
        String description = "Start\n@include(\"file1.txt\")\nMiddle\n@include(\"file2.txt\")\nEnd";
        String result = DescriptionIncludeProcessor.expandIncludes(description, workshopItem);
        
        assertEquals("Start\nContent 1\nMiddle\nContent 2\nEnd", result);
    }
    
    /**
     * Test implementation of SteamWorkshopItem for unit testing.
     */
    private static class TestSteamWorkshopItem extends SteamWorkshopItem {
        private final String workshopFolder;
        
        public TestSteamWorkshopItem(String workshopFolder) {
            super(workshopFolder);
            this.workshopFolder = workshopFolder;
        }
        
        public String getContentFolder() {
            return workshopFolder + File.separator + "Contents";
        }
    }
}

