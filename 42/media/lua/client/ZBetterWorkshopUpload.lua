require "OptionScreens/WorkshopSubmitScreen"
require "ZBetterWorkshopUploadOptions"

-- page1 - "Choose item directory"
-- page5 - "Prepare to publish item" with "Upload item to workshop now!" button

local orig_create = WorkshopSubmitScreen.create
function WorkshopSubmitScreen:create()
    orig_create(self)

    local padY = 64

    local page1 = self.page1
    if page1 and page1.listbox then
        -- make listbox bigger
        page1.listbox:setHeight(self:getHeight() - page1.listbox:getY() - padY)
    end

    local page5 = self.page5
    local text1 = getText("UI_WorkshopSubmit_ItemTitle")
    local text4 = getText("UI_WorkshopSubmit_ItemID")
    local text5 = getText("UI_WorkshopSubmit_Legal1")
    local text6 = getText("UI_WorkshopSubmit_Legal2")

    for id, child in pairs(self.page5.children) do
        -- print(id, child, child.Type)
        if child.Type == "ISLabel" then
            if child:getName() == text1 then
                page5.label1 = child
            elseif child:getName() == text4 then
                page5.label4 = child
            elseif child:getName() == text5 then
                page5.label5 = child
            elseif child:getName() == text6 then
                page5.label6 = child
            end
        end
        -- for k, v in pairs(child) do
        --     print("    ", k, v)
        -- end
    end

    if not page5.label1 then
        print "[ZBetterWorkshopUpload] Could not find label1"
        return
    end

    if not page5.label4 then
        print "[ZBetterWorkshopUpload] Could not find label4"
        return
    end

    if not page5.label5 then
        print "[ZBetterWorkshopUpload] Could not find label5"
        return
    end

    if not page5.label6 then
        print "[ZBetterWorkshopUpload] Could not find label6"
        return
    end

    if not page5.titleEntry then
        print "[ZBetterWorkshopUpload] Could not find titleEntry"
        return
    end

    if not page5.IDEntry then
        print "[ZBetterWorkshopUpload] Could not find IDEntry"
        return
    end

    if not page5.button1 then
        print "[ZBetterWorkshopUpload] Could not find button1"
        return
    end

    if not page5.button2 then
        print "[ZBetterWorkshopUpload] Could not find button2"
        return
    end

    local padX = 96
    local spanX = 8

    page5.label1:setX(padX)
    page5.label1:setY(64)

    page5.titleEntry:setX(page5.label1:getRight() + spanX)
    page5.titleEntry:setY(64)

    page5.label4:setX(page5.titleEntry:getRight() + spanX*2)
    page5.label4:setY(64)

    page5.IDEntry:setX(page5.label4:getRight() + spanX)
    page5.IDEntry:setY(64)

    page5.button1:setX(padX)
    page5.button1:setY(self:getHeight() - padY - page5.button1:getHeight())

    page5.button2:setX(self:getWidth() - padX - page5.button2:getWidth())
    page5.button2:setY(self:getHeight() - padY - page5.button2:getHeight())
    page5.button2:setAnchorLeft(false)
    page5.button2:setAnchorRight(true)
    page5.button2:setAnchorTop(false)
    page5.button2:setAnchorBottom(true)
    page5.button2:setBackgroundRGBA(0.7, 0.35, 0.15, 0.3)

    page5.label5:setY(page5.button2:getBottom() + 12)
    page5.label6:setY(page5.button2:getBottom() + 12)

    local listboxY = 128
    local listboxHeight = page5:getHeight() - listboxY - (page5:getHeight() - page5.button1:getY()) - padY/2

    page5.listbox = ISScrollingListBox:new(padX, listboxY, page5.width - padX * 2, listboxHeight)
    page5.listbox:initialise()
    page5.listbox:setAnchorLeft(true)
    page5.listbox:setAnchorRight(true)
    page5.listbox:setAnchorTop(true)
    page5.listbox:setAnchorBottom(true)
    page5.listbox:setFont("Medium", 4)
    page5.listbox.drawBorder = true
    page5.listbox.selectionColor = nil  -- Disable red selection highlight
    page5:addChild(page5.listbox)

    local orig_setFields = page5.setFields
    page5.setFields = function(...)
        orig_setFields(...)

        page5.listbox:clear()

        local workshopItem = page5.parent.item
        if ZBetterWorkshopUpload and ZBetterWorkshopUpload.getWorkshopItemFilteredContents then
            print("[ZBetterWorkshopUpload] Getting workshop item filtered contents for item "..tostring(workshopItem))
            local fileList = ZBetterWorkshopUpload.getWorkshopItemFilteredContents(workshopItem)
            if fileList and fileList:size()>0 then
                for i=0,fileList:size()-1 do
                    page5.listbox:addItem(fileList:get(i))
                end
            end
        else
            page5.listbox:addItem("ZBetterWorkshopUpload.getWorkshopItemFilteredContents() is not available")
            page5.listbox:addItem("Please check the installation of the ZombieBuddy mod")
        end
    end
end

