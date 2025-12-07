package me.zed_0xff.zbetter_workshop_upload;

import zombie.core.znet.SteamWorkshopItem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WorkshopContentFilter {
    // Ignore file name to check
    private static final String IGNORE_FILE_NAME = ".workshopignore";
    
    // Map to track filtered folders by workshop item ID (string)
    private static final Map<String, FilteredFolderInfo> filteredFoldersByItemId = new HashMap<>();
    
    // Cache for ignore patterns per directory (maps directory path -> list of patterns)
    private static final Map<String, List<String>> ignoreFileCache = new HashMap<>();
    
    // Thread-local to track current SubmitWorkshopItem context
    private static final ThreadLocal<SteamWorkshopItem> submitContext = new ThreadLocal<>();
    
    // Files/patterns to exclude from upload (loaded from mod options)
    private static final Set<String> EXCLUDED_PATTERNS = new HashSet<>();
    private static boolean patternsLoaded = false;
    
    // Default patterns (used as fallback)
    private static final String[] DEFAULT_PATTERNS = {
        ".DS_Store",
        ".git*",
        ".gradle",
        ".idea",
        ".vscode",
        "*.log",
        "*.tmp",
        "*.swp",
        "Thumbs.db",
        "tmp",
    };
    
    /**
     * Loads exclusion patterns from mod options. Called from Lua.
     * 
     * @param patternsText Semicolon-separated patterns (e.g., ".git; *.tmp; .DS_Store")
     */
    public static void loadExcludedPatterns(String patternsText) {
        synchronized (EXCLUDED_PATTERNS) {
            EXCLUDED_PATTERNS.clear();
            
            if (patternsText != null && !patternsText.trim().isEmpty()) {
                String[] patterns = patternsText.split(";");
                for (String pattern : patterns) {
                    pattern = pattern.trim();
                    if (!pattern.isEmpty()) {
                        EXCLUDED_PATTERNS.add(pattern);
                    }
                }
            } else {
                // Use defaults if no configuration
                for (String pattern : DEFAULT_PATTERNS) {
                    EXCLUDED_PATTERNS.add(pattern);
                }
            }
            
            patternsLoaded = true;
        }
    }
    
    /**
     * Gets the current exclusion patterns.
     * 
     * @return Set of exclusion patterns
     */
    public static Set<String> getExcludedPatterns() {
        ensurePatternsLoaded();
        synchronized (EXCLUDED_PATTERNS) {
            return new HashSet<>(EXCLUDED_PATTERNS);
        }
    }
    
    /**
     * Gets the default exclusion patterns as a semicolon-separated string.
     * 
     * @return Semicolon-separated string of default patterns
     */
    public static String getDefaultPatternsString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < DEFAULT_PATTERNS.length; i++) {
            if (i > 0) {
                sb.append("; ");
            }
            sb.append(DEFAULT_PATTERNS[i]);
        }
        return sb.toString();
    }
    
    /**
     * Ensures patterns are loaded. If not loaded from mod options, uses defaults.
     */
    private static void ensurePatternsLoaded() {
        if (!patternsLoaded) {
            synchronized (EXCLUDED_PATTERNS) {
                if (!patternsLoaded) {
                    // Use defaults if not loaded from mod options
                    for (String pattern : DEFAULT_PATTERNS) {
                        EXCLUDED_PATTERNS.add(pattern);
                    }
                    patternsLoaded = true;
                }
            }
        }
    }
    
    private static class FilteredFolderInfo {
        String originalContentFolder;
        String filteredWorkshopFolder;
    }
    
    /**
     * Filters a list of file paths, removing entries that match exclusion patterns.
     * 
     * @param filePaths The list of file paths to filter (relative to basePath)
     * @param basePath The base content folder path (absolute) for resolving relative paths
     * @return A new ArrayList containing only the paths that don't match exclusion patterns
     */
    public static ArrayList<String> filterFilePaths(ArrayList<String> filePaths, String basePath) {
        ArrayList<String> filtered = new ArrayList<>();
        if (filePaths == null) {
            return filtered;
        }
        
        for (String filePath : filePaths) {
            if (shouldIncludePath(filePath, basePath)) {
                filtered.add(filePath);
            }
        }
        
        return filtered;
    }
    
    /**
     * Filters a list of file paths, removing entries that match exclusion patterns.
     * Legacy overload without basePath - uses relative path resolution (may not work correctly for ignore files).
     * 
     * @param filePaths The list of file paths to filter
     * @return A new ArrayList containing only the paths that don't match exclusion patterns
     */
    public static ArrayList<String> filterFilePaths(ArrayList<String> filePaths) {
        return filterFilePaths(filePaths, null);
    }
    
    /**
     * Checks if a file path should be included (doesn't match exclusion patterns).
     * 
     * @param filePath The file path to check (relative to basePath)
     * @param basePath The base content folder path (absolute) for resolving relative paths
     * @return true if the path should be included, false if it matches an exclusion pattern
     */
    public static boolean shouldIncludePath(String filePath, String basePath) {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }
        
        ensurePatternsLoaded();
        
        // Normalize path separators for matching
        String normalizedPath = filePath.replace("\\", "/");
        String fileName = new File(filePath).getName();
        
        // Check against global excluded patterns (from mod options)
        Set<String> patterns;
        synchronized (EXCLUDED_PATTERNS) {
            patterns = new HashSet<>(EXCLUDED_PATTERNS);
        }
        
        for (String pattern : patterns) {
            if (matchesPattern(normalizedPath, fileName, pattern)) {
                return false;
            }
        }
        
        // Check against local ignore files (recursive from file's directory up to root)
        // Resolve relative path to absolute path using basePath
        File absoluteFile;
        if (basePath != null && !basePath.isEmpty()) {
            absoluteFile = new File(basePath, filePath);
        } else {
            absoluteFile = new File(filePath);
        }
        
        File dir = absoluteFile.isDirectory() ? absoluteFile : absoluteFile.getParentFile();
        if (dir != null) {
            List<String> localPatterns = getIgnorePatternsForPath(dir);
            for (String pattern : localPatterns) {
                if (matchesPattern(normalizedPath, fileName, pattern)) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Gets all ignore patterns that apply to a given file/directory path.
     * Recursively checks parent directories for ignore files.
     * 
     * @param file The file or directory to check
     * @return List of patterns from ignore files (most specific first)
     */
    private static List<String> getIgnorePatternsForPath(File file) {
        List<String> allPatterns = new ArrayList<>();
        File currentDir = file.isDirectory() ? file : file.getParentFile();
        
        if (currentDir == null) {
            return allPatterns;
        }
        
        // Walk up the directory tree, collecting ignore patterns
        try {
            File root = currentDir.getCanonicalFile();
            File checkDir = root;
            
            while (checkDir != null && checkDir.exists()) {
                // Check cache first
                String dirPath = checkDir.getCanonicalPath();
                List<String> cachedPatterns;
                synchronized (ignoreFileCache) {
                    cachedPatterns = ignoreFileCache.get(dirPath);
                }
                
                if (cachedPatterns == null) {
                    // Read ignore files from this directory
                    cachedPatterns = readIgnoreFiles(checkDir);
                    synchronized (ignoreFileCache) {
                        ignoreFileCache.put(dirPath, cachedPatterns);
                    }
                }
                
                // Add patterns from this directory (most specific first)
                allPatterns.addAll(0, cachedPatterns);
                
                // Move to parent directory
                File parent = checkDir.getParentFile();
                if (parent == null || parent.equals(checkDir)) {
                    break;
                }
                checkDir = parent;
            }
        } catch (IOException e) {
            // If we can't get canonical path, just check the immediate directory
            List<String> patterns = readIgnoreFiles(currentDir);
            allPatterns.addAll(patterns);
        }
        
        return allPatterns;
    }
    
    /**
     * Reads ignore patterns from .workshopignore file in a directory.
     * 
     * @param directory The directory to check for ignore file
     * @return List of patterns found in ignore file
     */
    private static List<String> readIgnoreFiles(File directory) {
        List<String> patterns = new ArrayList<>();
        
        if (directory == null || !directory.isDirectory()) {
            return patterns;
        }
        
        File ignoreFile = new File(directory, IGNORE_FILE_NAME);
        if (ignoreFile.exists() && ignoreFile.isFile()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(ignoreFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    // Skip empty lines and comments
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    patterns.add(line);
                }
            } catch (IOException e) {
                System.err.println("[ZBetterWorkshopUpload] Failed to read ignore file: " + ignoreFile.getAbsolutePath());
                e.printStackTrace();
            }
        }
        
        return patterns;
    }
    
    /**
     * Clears the ignore file cache. Useful when ignore files might have changed.
     */
    public static void clearIgnoreFileCache() {
        synchronized (ignoreFileCache) {
            ignoreFileCache.clear();
        }
    }
    
    /**
     * Checks if a file path or filename matches a pattern with wildcards.
     * Supports "*" wildcards anywhere in the pattern.
     * 
     * @param filePath The full file path (normalized with "/")
     * @param fileName The filename only
     * @param pattern The pattern to match (may contain "*" wildcards)
     * @return true if the path matches the pattern
     */
    private static boolean matchesPattern(String filePath, String fileName, String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return false;
        }
        
        // Convert pattern with wildcards to regex
        String regexPattern = patternToRegex(pattern);
        
        try {
            // Check filename
            if (fileName.matches(regexPattern)) {
                return true;
            }
            
            // Check full path
            if (filePath.matches(regexPattern)) {
                return true;
            }
            
            // Check if pattern appears as a directory in the path
            // (for patterns without wildcards, check if it's a directory segment)
            if (!pattern.contains("*")) {
                // For non-wildcard patterns, check if it appears as a directory segment
                if (filePath.contains("/" + pattern + "/") || filePath.endsWith("/" + pattern) ||
                    filePath.startsWith(pattern + "/")) {
                    return true;
                }
            } else {
                // For wildcard patterns, check if any path segment matches
                String[] pathSegments = filePath.split("/");
                for (String segment : pathSegments) {
                    if (segment.matches(regexPattern)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // If regex is invalid, fall back to simple string matching
            if (filePath.contains(pattern) || fileName.contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Converts a pattern with "*" wildcards to a Java regex pattern.
     * "*" matches any sequence of characters (except path separators in directory context).
     * 
     * @param pattern The pattern with wildcards
     * @return A regex pattern string
     */
    private static String patternToRegex(String pattern) {
        // Escape special regex characters except *
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '*') {
                regex.append(".*");
            } else if (c == '.' || c == '+' || c == '?' || c == '^' || c == '$' || 
                       c == '[' || c == ']' || c == '(' || c == ')' || c == '{' || 
                       c == '}' || c == '|' || c == '\\') {
                regex.append("\\").append(c);
            } else {
                regex.append(c);
            }
        }
        return regex.toString();
    }
    
    /**
     * Enters SubmitWorkshopItem context and prepares filtered content.
     * 
     * @param steamWorkshopItem The workshop item being submitted
     */
    public static void enterSubmitContext(SteamWorkshopItem steamWorkshopItem) {
        try {
            String itemId = steamWorkshopItem.getID();
            if (itemId == null) {
                System.err.println("[ZBetterWorkshopUpload] Workshop item has no ID, cannot track for cleanup");
                return;
            }
            
            // Get original content folder (before filtering)
            String originalFolder = getOriginalContentFolder(steamWorkshopItem);
            System.out.println("[ZBetterWorkshopUpload] Original content folder: " + originalFolder);
            
            // Create filtered copy
            String filteredFolder = createFilteredCopy(originalFolder);
            if (filteredFolder == null) {
                System.out.println("[ZBetterWorkshopUpload] Failed to create filtered copy, using original");
                return;
            }
            
            // Store info for cleanup (keyed by item ID)
            FilteredFolderInfo info = new FilteredFolderInfo();
            info.filteredWorkshopFolder = filteredFolder;
            info.originalContentFolder = originalFolder;
            
            // Store for cleanup after upload completes
            synchronized (filteredFoldersByItemId) {
                filteredFoldersByItemId.put(itemId, info);
            }
            
            // Set thread-local context so getContentFolder() knows to return filtered path
            submitContext.set(steamWorkshopItem);
            
            System.out.println("[ZBetterWorkshopUpload] Using filtered folder: " + filteredFolder);
            System.out.println("[ZBetterWorkshopUpload] Will clean up filtered folder when upload completes for item ID: " + itemId);
        } catch (Exception e) {
            System.err.println("[ZBetterWorkshopUpload] Error in enterSubmitContext: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Exits SubmitWorkshopItem context.
     */
    public static void exitSubmitContext() {
        submitContext.remove();
    }
    
    /**
     * Gets the filtered content folder if we're in SubmitWorkshopItem context for this item.
     * 
     * @param steamWorkshopItem The workshop item
     * @return Filtered folder path, or null if not in context or no filtered folder exists
     */
    public static String getFilteredContentFolder(SteamWorkshopItem steamWorkshopItem) {
        SteamWorkshopItem contextItem = submitContext.get();
        if (contextItem != steamWorkshopItem) {
            // Not in SubmitWorkshopItem context for this item
            return null;
        }
        
        String itemId = steamWorkshopItem.getID();
        if (itemId == null) {
            return null;
        }
        
        FilteredFolderInfo info;
        synchronized (filteredFoldersByItemId) {
            info = filteredFoldersByItemId.get(itemId);
        }
        
        if (info != null && info.filteredWorkshopFolder != null) {
            return info.filteredWorkshopFolder;
        }
        
        return null;
    }
    
    /**
     * Gets the original content folder path without filtering.
     * 
     * @param steamWorkshopItem The workshop item
     * @return Original content folder path
     */
    private static String getOriginalContentFolder(SteamWorkshopItem steamWorkshopItem) {
        try {
            Field workshopFolderField = SteamWorkshopItem.class.getDeclaredField("workshopFolder");
            workshopFolderField.setAccessible(true);
            String workshopFolder = (String) workshopFolderField.get(steamWorkshopItem);
            return workshopFolder + File.separator + "Contents";
        } catch (Exception e) {
            // Fallback to calling getContentFolder (but this might return filtered if in context)
            return steamWorkshopItem.getContentFolder();
        }
    }
    
    /**
     * Cleans up all pending filtered folders. Called when upload completes.
     */
    public static void cleanupAllPendingFolders() {
        synchronized (filteredFoldersByItemId) {
            if (filteredFoldersByItemId.isEmpty()) {
                return;
            }
            
            // Clean up all pending filtered folders
            for (Map.Entry<String, FilteredFolderInfo> entry : filteredFoldersByItemId.entrySet()) {
                String itemId = entry.getKey();
                FilteredFolderInfo info = entry.getValue();
                
                try {
                    // Clean up filtered folder (delete the entire temp directory, not just Contents)
                    File filteredContentDir = new File(info.filteredWorkshopFolder);
                    File filteredWorkshopDir = filteredContentDir.getParentFile();
                    if (filteredWorkshopDir != null && filteredWorkshopDir.exists()) {
                        deleteDirectory(filteredWorkshopDir);
                        System.out.println("[ZBetterWorkshopUpload] Cleaned up filtered folder for item " + itemId + ": " + filteredWorkshopDir.getAbsolutePath());
                    }
                } catch (Exception e) {
                    System.err.println("[ZBetterWorkshopUpload] Failed to clean up filtered folder for item " + itemId + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            // Clear all entries
            filteredFoldersByItemId.clear();
            System.out.println("[ZBetterWorkshopUpload] Cleaned up all pending filtered folders");
        }
    }
    
    private static String createFilteredCopy(String sourceContentFolder) {
        try {
            File sourceDir = new File(sourceContentFolder);
            if (!sourceDir.exists() || !sourceDir.isDirectory()) {
                System.err.println("[ZBetterWorkshopUpload] Source folder does not exist: " + sourceContentFolder);
                return null;
            }
            
            // Create temp directory for filtered copy
            File tempDir = Files.createTempDirectory("zb_workshop_filtered_").toFile();
            File filteredContentDir = new File(tempDir, "Contents");
            filteredContentDir.mkdirs();
            
            System.out.println("[ZBetterWorkshopUpload] Creating filtered copy in: " + filteredContentDir.getAbsolutePath());
            
            // Copy files with filtering
            ensurePatternsLoaded();
            Set<String> patterns;
            synchronized (EXCLUDED_PATTERNS) {
                patterns = new HashSet<>(EXCLUDED_PATTERNS);
            }
            
            FileFilter filter = new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    // Use shouldIncludePath which checks both global patterns and local ignore files
                    // pathname is already absolute, so we need to get relative path for matching
                    String absolutePath = pathname.getAbsolutePath();
                    String relativePath = sourceDir.toPath().relativize(pathname.toPath()).toString().replace("\\", "/");
                    return shouldIncludePath(relativePath, sourceDir.getAbsolutePath());
                }
            };
            
            copyDirectoryFiltered(sourceDir, filteredContentDir, filter);
            
            System.out.println("[ZBetterWorkshopUpload] Filtered copy created successfully");
            return filteredContentDir.getAbsolutePath();
        } catch (Exception e) {
            System.err.println("[ZBetterWorkshopUpload] Failed to create filtered copy: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    private static void copyDirectoryFiltered(File sourceDir, File destDir, FileFilter filter) throws IOException {
        if (!sourceDir.isDirectory()) {
            return;
        }
        
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        
        File[] files = sourceDir.listFiles(filter);
        if (files != null) {
            for (File file : files) {
                File destFile = new File(destDir, file.getName());
                if (file.isDirectory()) {
                    copyDirectoryFiltered(file, destFile, filter);
                } else {
                    copyFile(file, destFile);
                }
            }
        }
    }
    
    private static void copyFile(File source, File dest) throws IOException {
        try (FileInputStream fis = new FileInputStream(source);
             FileOutputStream fos = new FileOutputStream(dest)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
        }
    }
    
    private static void deleteDirectory(File directory) {
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
}

