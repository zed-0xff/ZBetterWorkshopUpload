package me.zed_0xff.zbetter_workshop_upload;

import zombie.core.znet.SteamWorkshopItem;
import zombie.ZomboidFileSystem;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import me.zed_0xff.zombie_buddy.Exposer;

@Exposer.LuaClass
public class ZBetterWorkshopUpload {
    /**
     * Gets filtered contents of a workshop item.
     * 
     * @param workshopItem The workshop item to get contents for
     * @return ArrayList of filtered file paths (relative to content folder)
     */
    public static ArrayList<String> getWorkshopItemFilteredContents(SteamWorkshopItem workshopItem) {
        File contentFolder = new File(workshopItem.getContentFolder());
        if (!contentFolder.exists() || !contentFolder.isDirectory()) {
            return new ArrayList<>();
        }

        String basePathStr = contentFolder.getAbsolutePath();
        Path basePath = Paths.get(basePathStr);
        
        // Get all files recursively
        File[] filteredFiles = ZomboidFileSystem.listAllFiles(contentFolder, new FileFilter() {
            @Override
            public boolean accept(File file) {
                Path filePath = Paths.get(file.getAbsolutePath());
                String relativePath = basePath.relativize(filePath).toString().replace("\\", "/");
                return WorkshopContentFilter.shouldIncludePath(relativePath, basePathStr);
            }
        }, true);
        
        // Convert to relative paths
        ArrayList<String> filenames = new ArrayList<>();
        for (File file : filteredFiles) {
            Path filePath = Paths.get(file.getAbsolutePath());
            String relativePath = basePath.relativize(filePath).toString().replace("\\", "/");
            filenames.add(relativePath);
        }
        
        return filenames;
    }
    
    /**
     * Loads excluded patterns from configuration.
     * Called from Lua mod options.
     * 
     * @param patternsText Semicolon-separated patterns (e.g., ".git; *.tmp; .DS_Store")
     */
    public static void loadExcludedPatterns(String patternsText) {
        WorkshopContentFilter.loadExcludedPatterns(patternsText);
    }
    
    /**
     * Gets the default exclusion patterns as a semicolon-separated string.
     * Used by Lua mod options to initialize the default value.
     * 
     * @return Semicolon-separated string of default patterns
     */
    public static String getDefaultExcludedPatterns() {
        return WorkshopContentFilter.getDefaultPatternsString();
    }
}
