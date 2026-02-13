package me.zed_0xff.zbetter_workshop_upload;

import me.zed_0xff.zombie_buddy.Patch;

import zombie.core.znet.SteamWorkshopItem;

public class Patch_SteamWorkshop {

    @Patch(className = "zombie.core.znet.SteamWorkshop", methodName = "SubmitWorkshopItem")
    public class Patch_SubmitWorkshopItem {
        @Patch.OnEnter
        public static void beforeSubmitWorkshopItem(SteamWorkshopItem steamWorkshopItem) {
            WorkshopContentFilter.enterSubmitContext(steamWorkshopItem);
        }

        @Patch.OnExit
        public static void afterSubmitWorkshopItem() {
            WorkshopContentFilter.exitSubmitContext();
            WorkshopContentFilter.cleanupAllPendingFolders();
        }
    }    
    
    // Patch the callback methods to trigger cleanup
    @Patch(className = "zombie.core.znet.SteamWorkshop", methodName = "onItemUpdated")
    public static class Patch_onItemUpdated {
        @Patch.OnEnter
        public static void beforeOnItemUpdated() {
            WorkshopContentFilter.cleanupAllPendingFolders();
        }
    }
    
    @Patch(className = "zombie.core.znet.SteamWorkshop", methodName = "onItemNotUpdated")
    public static class Patch_onItemNotUpdated {
        @Patch.OnEnter
        public static void beforeOnItemNotUpdated() {
            WorkshopContentFilter.cleanupAllPendingFolders();
        }
    }
}
