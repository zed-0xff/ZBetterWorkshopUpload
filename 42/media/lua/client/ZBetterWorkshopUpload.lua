require "OptionScreens/WorkshopSubmitScreen"
require "ZBetterWorkshopUploadOptions"

-- Colors for file list by extension (r, g, b, a in 0–1)
local COLOR_IMAGE   = { 0.35, 0.85, 0.4,  0.95 }
local COLOR_TEXT    = { 1.0,  1.0,  1.0,  0.95 }
local COLOR_LUA     = { 0.3,  0.85, 0.9,  0.95 }
local COLOR_JAVA    = { 0.4,  0.6,  1.0,  0.95 }
local COLOR_JAR     = { 1.0,  0.55, 0.25, 0.95 }
local COLOR_BINARY  = { 1.0,  0.35, 0.35, 0.95 }
local DEFAULT_COLOR = { 0.55, 0.55, 0.55, 0.95 }

local FILE_COLORS = {}
for _, ext in ipairs({ "png", "jpg", "gif" }) do FILE_COLORS[ext] = COLOR_IMAGE end
for _, ext in ipairs({ "txt", "md", "info" }) do FILE_COLORS[ext] = COLOR_TEXT end
for _, ext in ipairs({ "java", "gradle", "properties" }) do FILE_COLORS[ext] = COLOR_JAVA end
FILE_COLORS.lua = COLOR_LUA
FILE_COLORS.jar = COLOR_JAR
for _, ext in ipairs({ "exe", "dll", "dylib", "so", "bat", "cmd", "sh", "ps1" }) do FILE_COLORS[ext] = COLOR_BINARY end

local function colorForPath(path)
    if not path or path == "" then return DEFAULT_COLOR end
    local filename = path:match("([^/]+)$") or path
    if filename:lower() == "license" then return COLOR_TEXT end
    local ext = path:match("%.(%w+)$")
    if not ext then return DEFAULT_COLOR end
    ext = ext:lower()
    return FILE_COLORS[ext] or DEFAULT_COLOR
end

local function getVisiblePage(screen)
    for i = 1, 10 do
        local p = screen["page" .. i]
        if p and p:isVisible() then return p end
    end
    return nil
end

-- page1 - "Choose item directory"
-- page5 - "Prepare to publish item" with "Upload item to workshop now!" button

local orig_create = WorkshopSubmitScreen.create
function WorkshopSubmitScreen:create()
    orig_create(self)

    local spanX = 8
    local padY = 64

    local page1 = self.page1
    if page1 and page1.listbox then
        -- make listbox bigger
        page1.listbox:setHeight(self:getHeight() - page1.listbox:getY() - padY)
    end

    -- move Workshop folder path label to the top & right
    local text1 = getText("UI_WorkshopSubmit_ContentFolder")
    local text2 = Core.getMyDocumentFolder() .. getFileSeparator() .. "Workshop"
    for id, child in pairs(self.page1.children) do
        if child.Type == "ISLabel" then
            if child:getName() == text1 then
                page1.label1 = child
            elseif child:getName() == text2 then
                page1.label2 = child
            end
        end
    end

    if page1.label1 and page1.label2 then
        page1.label2:setX(page1.label1:getRight() + spanX)
        page1.label2:setY(page1.label1:getY())
        page1.label2:setColor(COLOR_LUA[1], COLOR_LUA[2], COLOR_LUA[3])
    end

    -- show mod icons in page1 listbox (override listbox.doDrawItem directly; page1.doDrawItem is copied at create)
    local iconW = 32
    local iconPad = 12
    local textX = 8 + iconW + iconPad  -- align all text (with or without icon)
    page1.listbox.doDrawItem = function(self, y, item, alt)
        local workshopItem = item and item.item
        if workshopItem and workshopItem.getPreviewImage then
            local imgPath = workshopItem:getPreviewImage()
            if imgPath and imgPath ~= "" then
                local tex = getTexture(imgPath)
                if tex then
                    local sz = math.min(self.itemheight - 4, iconW)
                    self:drawTextureScaled(tex, 8, y + (self.itemheight - sz) / 2, sz, sz, 1, 1, 1, 1)
                end
            end
        end
        self:drawRectBorder(0, y, self:getWidth(), self.itemheight - 1, 0.5, self.borderColor.r, self.borderColor.g, self.borderColor.b)
        if self.selected == item.index then
            self:drawRect(0, y, self:getWidth(), self.itemheight - 1, 0.3, 0.7, 0.35, 0.15)
        end
        local dy = (self.itemheight - getTextManager():getFontFromEnum(self.font):getLineHeight()) / 2
        self:drawText(item.text, textX, y + dy, 0.9, 0.9, 0.9, 0.9, self.font)
        return y + item.height
    end

    --- page5

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
            if fileList and fileList:size() > 0 then
                local paths = {}
                for i = 0, fileList:size() - 1 do
                    paths[#paths + 1] = fileList:get(i)
                end
                table.sort(paths)
                for _, path in ipairs(paths) do
                    page5.listbox:addItem(path)
                    local idx = page5.listbox:size()
                    local c = colorForPath(path)
                    page5.listbox:setItemTextColorRGBA(idx, c[1], c[2], c[3], c[4])
                    page5.listbox:setItemSelectedTextColorRGBA(idx, c[1], c[2], c[3], c[4])
                end
            end
        else
            page5.listbox:addItem("ZBetterWorkshopUpload.getWorkshopItemFilteredContents() is not available")
            page5.listbox:addItem("Please check the installation of the ZombieBuddy mod")
        end
    end
end

-- Esc: previous page, or close to main menu if on page9. Hook/unhook in setVisible so the listener
-- is only active while the Workshop Submit screen is visible.
local function onEscKey(key)
    if key ~= Keyboard.KEY_ESCAPE then return end
    local screen = WorkshopSubmitScreen.instance
    if not screen or not screen:isVisible() then return end
    local page = getVisiblePage(screen)
    if page and page.onButtonBack then
        page:onButtonBack()
    end
end

local orig_setVisible = WorkshopSubmitScreen.setVisible
function WorkshopSubmitScreen:setVisible(visible)
    orig_setVisible(self, visible)
    if visible then
        Events.OnKeyPressed.Add(onEscKey)
    else
        Events.OnKeyPressed.Remove(onEscKey)
    end
end
