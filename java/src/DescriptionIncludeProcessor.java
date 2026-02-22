package me.zed_0xff.zbetter_workshop_upload;

import zombie.core.znet.SteamWorkshopItem;
import zombie.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class DescriptionIncludeProcessor {
    /**
     * Expands @include directives in a description string.
     * Processes each line separately, replacing lines that start with @include("filename") 
     * with the contents of the referenced file.
     * 
     * @param description The description string to process
     * @param workshopItem The workshop item to get the folder path from
     * @return The description with all @include directives expanded
     */
    public static String expandIncludes(String description, SteamWorkshopItem workshopItem) {
        if (description == null || description.isEmpty()) {
            return description;
        }
        
        String workshopFolderPath = WorkshopContentFilter.getWorkshopFolderPath(workshopItem);
        
        // Split description into lines and process each line
        String[] lines = description.split("\r?\n", -1);
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String processedLine = line;
            
            // Check if line starts with @include(
            if (line.trim().startsWith("@include(")) {
                String filename = extractFilenameFromInclude(line);
                
                if (filename != null && !filename.isEmpty()) {
                    // Security checks: both should be false
                    if (!StringUtils.containsDoubleDot(filename) && !new File(filename).isAbsolute()) {
                        String fileContents = readIncludeFile(workshopFolderPath, filename);
                        if (fileContents != null) {
                            processedLine = fileContents;
                        }
                    }
                }
            }
            
            result.append(processedLine);
            
            // Add newline except for last line
            if (i < lines.length - 1) {
                result.append("\n");
            }
        }
        
        return result.toString();
    }
    
    /**
     * Extracts the filename from an @include directive.
     * Expected format: @include("filename")
     * 
     * @param line The line containing the @include directive
     * @return The filename, or null if the format is invalid
     */
    private static String extractFilenameFromInclude(String line) {
        String trimmed = line.trim();
        if (!trimmed.startsWith("@include(")) {
            return null;
        }
        
        // Find opening parenthesis
        int parenStart = "@include".length();
        if (trimmed.charAt(parenStart) != '(') {
            return null;
        }
        
        // Find opening quote after parenthesis
        int quoteStart = trimmed.indexOf('"', parenStart + 1);
        if (quoteStart == -1) {
            return null;
        }
        
        // Find closing quote
        int quoteEnd = trimmed.indexOf('"', quoteStart + 1);
        if (quoteEnd == -1) {
            return null;
        }
        
        // Find closing parenthesis (should be right after closing quote)
        int parenEnd = quoteEnd + 1;
        if (parenEnd >= trimmed.length() || trimmed.charAt(parenEnd) != ')') {
            return null;
        }
        
        return trimmed.substring(quoteStart + 1, quoteEnd);
    }
    
    /**
     * Reads the contents of an include file.
     * 
     * @param workshopFolderPath The base workshop folder path
     * @param filename The filename to read (relative to workshop folder)
     * @return The file contents, or null if the file cannot be read
     */
    private static String readIncludeFile(String workshopFolderPath, String filename) {
        try {
            File file = new File(workshopFolderPath, filename);
            
            if (file.exists() && file.isFile()) {
                return Files.readString(file.toPath(), StandardCharsets.UTF_8);
            } else {
                System.err.println("[ZBetterWorkshopUpload] Include file not found: " + filename);
            }
        } catch (IOException e) {
            System.err.println("[ZBetterWorkshopUpload] Failed to read include file: " + filename);
            e.printStackTrace();
        }
        
        return null;
    }
}

