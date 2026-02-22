package me.zed_0xff.zbetter_workshop_upload;

import me.zed_0xff.zombie_buddy.Patch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import zombie.core.znet.SteamWorkshopItem;

// sort workshop items by folder name
@Patch(className = "zombie.Lua.LuaManager$GlobalObject", methodName = "getSteamWorkshopStagedItems")
public class Patch_getSteamWorkshopStagedItems {
    public static final Comparator<SteamWorkshopItem> comparator = new Comparator<SteamWorkshopItem>() {
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
            Collections.sort(result, comparator);
        }
    }
}
