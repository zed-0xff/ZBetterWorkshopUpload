package me.zed_0xff.zbetter_workshop_upload;

import me.zed_0xff.zombie_buddy.Exposer;
import me.zed_0xff.zombie_buddy.Patch;

import zombie.core.znet.SteamWorkshopItem;

public class Main {
    public static void main(String[] args) {
        Exposer.exposeClassToLua(ZBetterWorkshopUpload.class);
    }

    @Patch(className = "zombie.core.znet.SteamWorkshop", methodName = "SubmitWorkshopItem")
    public class Patch_SubmitWorkshopItem {
        @Patch.OnEnter
        public static void beforeSubmitWorkshopItem(SteamWorkshopItem steamWorkshopItem) {
            WorkshopContentFilter.enterSubmitContext(steamWorkshopItem);
        }
        
        @Patch.OnExit
        public static void afterSubmitWorkshopItem(SteamWorkshopItem steamWorkshopItem, @Patch.Return(readOnly = true) boolean result) {
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

    // Patch getContentFolder to return filtered folder when in SubmitWorkshopItem context
    @Patch(className = "zombie.core.znet.SteamWorkshopItem", methodName = "getContentFolder")
    public static class Patch_getContentFolder {
        @Patch.OnExit
        public static void afterGetContentFolder(@Patch.Return(readOnly = false) String result, SteamWorkshopItem self) {
            String filteredFolder = WorkshopContentFilter.getFilteredContentFolder(self);
            if (filteredFolder != null) {
                result = filteredFolder;
            }
        }
    }
}
