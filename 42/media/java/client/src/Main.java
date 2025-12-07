package me.zed_0xff.zbetter_workshop_upload;

import me.zed_0xff.zombie_buddy.Exposer;
import me.zed_0xff.zombie_buddy.Patch;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;

import zombie.core.znet.SteamWorkshopItem;

public class Main {
    public static void main(String[] args) {
        Exposer.exposeClassToLua(ZBetterWorkshopUpload.class);
    }

    @Patch(className = "zombie.core.znet.SteamWorkshop", methodName = "SubmitWorkshopItem")
    public class Patch_SubmitWorkshopItem {
        // Thread-local to store original workshopFolder value for restoration
        // Must be public for ByteBuddy Advice to access it
        public static final ThreadLocal<String> originalWorkshopFolder = new ThreadLocal<>();
        
        // Thread-local to store original description value for restoration
        // Must be public for ByteBuddy Advice to access it
        public static final ThreadLocal<String> originalDescription = new ThreadLocal<>();
        
        @Patch.OnEnter
        public static void beforeSubmitWorkshopItem(SteamWorkshopItem steamWorkshopItem) {
            // Enter context first to create filtered folder
            WorkshopContentFilter.enterSubmitContext(steamWorkshopItem);
            
            // Store and process description
            try {
                String originalDesc = steamWorkshopItem.getDescription();
                originalDescription.set(originalDesc);
                
                // Expand @include directives in description
                String expandedDescription = DescriptionIncludeProcessor.expandIncludes(originalDesc, steamWorkshopItem);
                if (expandedDescription != null && !expandedDescription.equals(originalDesc)) {
                    steamWorkshopItem.setDescription(expandedDescription);
                    System.out.println("[ZBetterWorkshopUpload] Expanded description with includes: " + originalDesc.length() + " -> " + expandedDescription.length());
                }
            } catch (Exception e) {
                System.err.println("[ZBetterWorkshopUpload] Failed to process description: " + e.getMessage());
                e.printStackTrace();
            }
            
            // Get the filtered folder path (this is the "Contents" directory path)
            String filteredFolder = WorkshopContentFilter.getFilteredContentFolder(steamWorkshopItem);
            if (filteredFolder != null) {
                // Modify the workshopFolder field to point to the filtered folder
                // The filtered folder is the "Contents" directory, so we need to go up one level
                try {
                    Field workshopFolderField = SteamWorkshopItem.class.getDeclaredField("workshopFolder");
                    workshopFolderField.setAccessible(true);
                    
                    // Store original value for restoration
                    String originalValue = (String) workshopFolderField.get(steamWorkshopItem);
                    originalWorkshopFolder.set(originalValue);
                    
                    // filteredFolder is the Contents path, so get parent for workshopFolder
                    String filteredWorkshopFolder = new File(filteredFolder).getParent();
                    
                    // Verify that the Contents folder exists at the expected location
                    File contentsDir = new File(filteredWorkshopFolder, "Contents");
                    if (!contentsDir.exists() || !contentsDir.isDirectory()) {
                        System.err.println("[ZBetterWorkshopUpload] ERROR: Contents folder does not exist at: " + contentsDir.getAbsolutePath());
                        System.err.println("[ZBetterWorkshopUpload] filteredFolder: " + filteredFolder);
                        System.err.println("[ZBetterWorkshopUpload] filteredWorkshopFolder: " + filteredWorkshopFolder);
                    } else {
                        workshopFolderField.set(steamWorkshopItem, filteredWorkshopFolder);
                        System.out.println("[ZBetterWorkshopUpload] Modified workshopFolder from " + originalValue + " to " + filteredWorkshopFolder);
                        System.out.println("[ZBetterWorkshopUpload] Verified Contents folder exists at: " + contentsDir.getAbsolutePath());
                    }
                } catch (Exception e) {
                    System.err.println("[ZBetterWorkshopUpload] Failed to modify workshopFolder: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        
        @Patch.OnExit
        public static void afterSubmitWorkshopItem(SteamWorkshopItem steamWorkshopItem, @Patch.Return(readOnly = true) boolean result) {
            // Restore original description
            String originalDesc = originalDescription.get();
            if (originalDesc != null) {
                try {
                    steamWorkshopItem.setDescription(originalDesc);
                    System.out.println("[ZBetterWorkshopUpload] Restored original description");
                } catch (Exception e) {
                    System.err.println("[ZBetterWorkshopUpload] Failed to restore description: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    originalDescription.remove();
                }
            }
            
            // Restore original workshopFolder value
            String originalValue = originalWorkshopFolder.get();
            if (originalValue != null) {
                try {
                    Field workshopFolderField = SteamWorkshopItem.class.getDeclaredField("workshopFolder");
                    workshopFolderField.setAccessible(true);
                    workshopFolderField.set(steamWorkshopItem, originalValue);
                    System.out.println("[ZBetterWorkshopUpload] Restored original workshopFolder: " + originalValue);
                } catch (Exception e) {
                    System.err.println("[ZBetterWorkshopUpload] Failed to restore workshopFolder: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    originalWorkshopFolder.remove();
                }
            }
            
            WorkshopContentFilter.exitSubmitContext();
        }
    }    
    
    // Patch the callback methods to trigger cleanup
    @Patch(className = "zombie.core.znet.SteamWorkshop", methodName = "onItemUpdated")
    public static class Patch_onItemUpdated {
        @Patch.OnEnter
        public static void beforeOnItemUpdated(boolean z) {
            WorkshopContentFilter.cleanupAllPendingFolders();
        }
    }
    
    @Patch(className = "zombie.core.znet.SteamWorkshop", methodName = "onItemNotUpdated")
    public static class Patch_onItemNotUpdated {
        @Patch.OnEnter
        public static void beforeOnItemNotUpdated(int i) {
            WorkshopContentFilter.cleanupAllPendingFolders();
        }
    }

    @Patch(className = "zombie.core.znet.SteamWorkshopItem", methodName = "getSubmitDescription")
    public static class Patch_getSubmitDescription {
        @Patch.OnExit
        public static void exit() {
            System.out.println("[ZBetterWorkshopUpload] afterGetSubmitDescription");
        }
    }

    @Patch(className = "zombie.Lua.LuaManager$GlobalObject", methodName = "getSteamWorkshopStagedItems")
    public static class Patch_getSteamWorkshopStagedItems {
        public static final java.util.Comparator<SteamWorkshopItem> comparator = new java.util.Comparator<SteamWorkshopItem>() {
            @Override
            public int compare(SteamWorkshopItem o1, SteamWorkshopItem o2) {
                String folder1 = o1.getFolderName();
                String folder2 = o2.getFolderName();
                if (folder1 == null && folder2 == null) return 0;
                if (folder1 == null) return -1;
                if (folder2 == null) return 1;
                return folder1.compareToIgnoreCase(folder2);
            }
        };

        @Patch.OnExit
        public static void exit(@Patch.Return(readOnly = false) ArrayList<SteamWorkshopItem> result) {
            if (result != null) {
                // sort results by getFolderName()
                Collections.sort(result, comparator);
            }
        }
    }

    // XXX these patches don't get triggered for some reason, have to use another way
    //
    // // Patch getContentFolder to return filtered folder when in SubmitWorkshopItem context
    // @Patch(className = "zombie.core.znet.SteamWorkshopItem", methodName = "getContentFolder")
    // public static class Patch_getContentFolder {
    //     @Patch.OnExit
    //     public static void afterGetContentFolder(@Patch.Return(readOnly = false) String result) {
    //         System.out.println("[ZBetterWorkshopUpload] afterGetContentFolder");
    //     }
    // }
    //
    // @Patch(className = "zombie.core.znet.SteamWorkshopItem", methodName = "getDescription")
    //public static class Patch_getDescription {
    //    @Patch.OnExit
    //    public static void exit() {
    //        System.out.println("[ZBetterWorkshopUpload] afterGetDescription");
    //    }
    //}
}
