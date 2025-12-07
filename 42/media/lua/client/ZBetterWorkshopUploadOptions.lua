local MOD_ID = "ZBetterWorkshopUpload"
local EXCLUDED_PATTERNS_ID = "excludedPatterns"

-- Get default patterns from Java (single source of truth)
-- Fallback to hardcoded value if Java isn't available yet
local function getDefaultPatterns()
    if ZBetterWorkshopUpload and ZBetterWorkshopUpload.getDefaultExcludedPatterns then
        return ZBetterWorkshopUpload.getDefaultExcludedPatterns()
    end
    -- Fallback (should match Java defaults, but Java is the source of truth)
    return ".DS_Store; .git*; .gradle; .idea; .vscode; *.log; *.tmp; *.swp; Thumbs.db; tmp"
end

-- ------------------------------------------------- --
-- Initialize Mod Options
-- ------------------------------------------------- --

local function InitZBetterWorkshopUploadModOptions()
    local options = PZAPI.ModOptions:create(MOD_ID, "ZBetterWorkshopUpload")

    -- Add title
    options:addTitle("Workshop Content Filter")

    -- Add description
    options:addDescription("Configure which files and patterns to exclude from workshop uploads. Separate patterns with semicolons.")

    -- Get default patterns from Java (single source of truth)
    local defaultPatterns = getDefaultPatterns()
    
    -- Add text entry for excluded patterns
    options:addTextEntry(EXCLUDED_PATTERNS_ID, "Excluded Patterns", defaultPatterns,
        "Separate patterns with semicolons. Examples: .git; *.tmp; .DS_Store")
end

Events.OnGameBoot.Add(InitZBetterWorkshopUploadModOptions)

-- ------------------------------------------------- --
-- Get Mod Options
-- ------------------------------------------------- --

function ZBetterWorkshopUpload_getExcludedPatterns()
    local options = PZAPI.ModOptions:getOptions(MOD_ID)
    if options then
        local patternOption = options:getOption(EXCLUDED_PATTERNS_ID)
        if patternOption then
            local patternsText = patternOption:getValue()
            if patternsText then
                return patternsText
            end
        end
    end
    -- Return defaults from Java (single source of truth)
    return getDefaultPatterns()
end

-- ------------------------------------------------- --
-- Update Java when options change
-- ------------------------------------------------- --

local function updateExcludedPatterns()
    if ZBetterWorkshopUpload then
        local patternsText = ZBetterWorkshopUpload_getExcludedPatterns()
        if patternsText then
            ZBetterWorkshopUpload.loadExcludedPatterns(patternsText)
        end
    end
end

-- Update patterns when mod options are loaded/changed
local function onModOptionsChanged()
    updateExcludedPatterns()
end

-- Hook into the options apply function
Events.OnGameBoot.Add(function()
    local options = PZAPI.ModOptions:getOptions(MOD_ID)
    if options then
        local origApply = options.apply
        options.apply = function(self)
            if origApply then origApply(self) end
            onModOptionsChanged()
        end
        -- Also update immediately after boot
        updateExcludedPatterns()
    end
end)

