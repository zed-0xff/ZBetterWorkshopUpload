require "OptionScreens/WorkshopSubmitScreen"
require "ZBetterWorkshopUploadOptions"

-- Colors for file list by extension (r, g, b, a in 0–1)
local COLOR_IMAGE   = { 0.35, 0.85, 0.4,  0.95 }
local COLOR_TEXT    = { 1.0,  1.0,  1.0,  0.95 }
local COLOR_LUA     = { 0.3,  0.85, 0.9,  0.95 }
local COLOR_JAVA    = { 0.4,  0.6,  1.0,  0.95 }
local COLOR_JAR     = { 1.0,  0.55, 0.25, 0.95 }
local DEFAULT_COLOR = { 0.55, 0.55, 0.55, 0.95 }

local FILE_COLORS = {}
for _, ext in ipairs({ "png", "jpg", "gif" }) do FILE_COLORS[ext] = COLOR_IMAGE end
for _, ext in ipairs({ "txt", "md", "info" }) do FILE_COLORS[ext] = COLOR_TEXT end
FILE_COLORS.lua = COLOR_LUA
FILE_COLORS.java = COLOR_JAVA
FILE_COLORS.jar = COLOR_JAR

local function colorForPath(path)
    if not path or path == "" then return DEFAULT_COLOR end
    local ext = path:match("%.(%w+)$")
    if not ext then return DEFAULT_COLOR end
    ext = ext:lower()
    return FILE_COLORS[ext] or DEFAULT_COLOR
end

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

    local required = { "label1", "label4", "label5", "label6", "titleEntry", "IDEntry", "button1", "button2" }
    for _, key in ipairs(required) do
        if not page5[key] then
            print("[ZBetterWorkshopUpload] Could not find " .. key)
            return
        end
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
                    local path = fileList:get(i)
                    page5.listbox:addItem(path)
                    local c = colorForPath(path)
                    page5.listbox:setItemTextColorRGBA(page5.listbox:size(), c[1], c[2], c[3], c[4])
                end
            end
        else
            page5.listbox:addItem("ZBetterWorkshopUpload.getWorkshopItemFilteredContents() is not available")
            page5.listbox:addItem("Please check the installation of the ZombieBuddy mod")
        end
    end
end

