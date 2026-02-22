package me.zed_0xff.zbetter_workshop_upload;

import me.zed_0xff.zombie_buddy.Patch;

import zombie.core.znet.SteamWorkshopItem;

import java.io.File;

public class Patch_SteamWorkshopItem {

    @Patch(className = "zombie.core.znet.SteamWorkshopItem", methodName = "getSubmitDescription")
    public static class Patch_getSubmitDescription {
        @Patch.OnExit
        public static void exit(
            @Patch.This Object self,
            @Patch.Return(readOnly = false) String result
        ) {
            String expandedDescription = DescriptionIncludeProcessor.expandIncludes(result, (SteamWorkshopItem) self);
            if (expandedDescription != null && !expandedDescription.equals(result)) {
                System.out.println("[ZBetterWorkshopUpload] Expanded description from: " + result.length() + " to " + expandedDescription.length() + " chars");
                result = expandedDescription;
            }
        }
    }

    // @Patch(className = "zombie.core.znet.SteamWorkshopItem", methodName = "getPreviewImage")
    // public static class Patch_getPreviewImage {
    //     @Patch.OnExit
    //     public static void exit(
    //         @Patch.This Object self,
    //         @Patch.Return(readOnly = false) String result
    //     ) {
    //         String filteredFolder = WorkshopContentFilter.getFilteredWorkshopFolder((SteamWorkshopItem) self);
    //         if (filteredFolder != null) {
    //             result = new File(filteredFolder, "preview.png").getAbsolutePath();
    //         }
    //         System.out.println("[ZBetterWorkshopUpload] getPreviewImage: " + result);
    //     }
    // }

    // Patch getContentFolder to return filtered folder when in SubmitWorkshopItem context
    @Patch(className = "zombie.core.znet.SteamWorkshopItem", methodName = "getContentFolder")
    public static class Patch_getContentFolder {
        @Patch.OnExit
        public static void afterGetContentFolder(
            @Patch.This Object self,
            @Patch.Return(readOnly = false) String result
        ) {
            String originalValue = result;

            // Get the filtered folder path (this is the "Contents" directory path)
            String filteredFolder = WorkshopContentFilter.getFilteredWorkshopFolder((SteamWorkshopItem) self);
            if (filteredFolder != null) {
                String filteredWorkshopFolder = new File(filteredFolder).getParent();
                File contentsDir = new File(filteredWorkshopFolder, "Contents");
                if (!contentsDir.exists() || !contentsDir.isDirectory()) {
                    System.err.println("[ZBetterWorkshopUpload] ERROR: Contents folder does not exist at: " + contentsDir.getAbsolutePath());
                    System.err.println("[ZBetterWorkshopUpload] filteredFolder: " + filteredFolder);
                    System.err.println("[ZBetterWorkshopUpload] filteredWorkshopFolder: " + filteredWorkshopFolder);
                    throw new RuntimeException("[ZBetterWorkshopUpload] ERROR: Contents folder does not exist at: " + contentsDir.getAbsolutePath());
                } else {
                    result = contentsDir.getAbsolutePath();
                    System.out.println("[ZBetterWorkshopUpload] Modified contents folder from " + originalValue + " to " + result);
                    System.out.println("[ZBetterWorkshopUpload] Verified Contents folder exists at " + contentsDir.getAbsolutePath());
                }
            } else {
                System.out.println("[ZBetterWorkshopUpload] returning original workshopFolder: " + result);
            }
        }
    }
}
