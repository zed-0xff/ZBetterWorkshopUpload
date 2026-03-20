local version = 3.0

if type(zbHook_VERSION) == "number" and zbHook_VERSION >= version then return end

print("zbHook v" .. tostring(version) .. " init")
zbHook_VERSION = version

local registry = {}

local function named_function(objName, methodName, orig, wrapper)
    if not loadstring or not setfenv then
        -- fallback to unnamed functions if loadstring or setfenv is not available
        return function(...) return wrapper(orig, ...) end
    end

    local curHook = registry[orig]
    if curHook and curHook.orig then
        print("[zbHook] updating existing " .. tostring(methodName) .. " hook: " .. tostring(curHook.filename) .. ":" .. tostring(curHook.line))
        registry[orig] = nil
        orig = curHook.orig
    end

    local str = "local function " .. methodName .. "(...) return wrapper(orig, ...) end; return " .. methodName
    local cinfo = nil
    if ZombieBuddy and ZombieBuddy.getClosureInfo then
        cinfo = ZombieBuddy.getClosureInfo(wrapper)
        if cinfo and cinfo.line and cinfo.line > 1 then
            -- fix linenumber so tostring(wrapper) shows original wrapper's source file and line number instead of zbHook's
            str = string.rep("\n", cinfo.line - 1) .. str
        end
    end
    local chunk = loadstring(str, "zbHook")
    setfenv(chunk, { wrapper = wrapper, orig = orig })
    local newFun = chunk()

    if cinfo then
        cinfo.orig = orig
        registry[newFun] = cinfo
    end

    return newFun
end

local function isCallable(obj)
    if type(obj) == "function" then return true end
    local mt = getmetatable(obj)
    return type(mt) == "table" and type(mt.__call) == "function"
end

local function hookTable(tbl, hooks, objName, objDisplayName)
    local nHooked = 0
    for methodName, wrapper in pairs(hooks) do
        local orig = tbl[methodName]
        if orig ~= wrapper then -- skip duplicate hooks
            if isCallable(orig) then
                if isCallable(wrapper) then
                    tbl[methodName] = named_function( objName, methodName, orig, wrapper )
                    nHooked = nHooked + 1
                else
                    print("[!] zbHook: " .. objDisplayName .. "." .. tostring(methodName) .. " wrapper is not callable, type=" .. type(wrapper))
                end
            else
                print("[?] zbHook: " .. objDisplayName .. "." .. tostring(methodName) .. " is not callable, type=" .. type(orig))
            end
        end
    end
    return nHooked
end

local function hookMetaTable(obj, hooks, objName, objDisplayName)
    local mt = getmetatable(obj)
    if not mt then return end

    local index = mt.__index
    if not index then return end

    return hookTable(index, hooks, objName, objDisplayName)
end

local function hookObj(objName, hooks)
    local obj = nil
    if type(objName) == "string" then
        obj = _G[objName]
    else
        obj = objName
        objName = nil
    end

    local objDisplayName = objName or tostring(obj)

    if type(hooks) ~= "table" then
        print("[!] zbHook: expected hooks for " .. objDisplayName .. " to be a table, got " .. type(hooks))
        return 0
    end

    if type(obj) == "table"    then return hookTable(obj, hooks, objName, objDisplayName) end
    if type(obj) == "userdata" then return hookMetaTable(obj, hooks, objName, objDisplayName) end

    print("[?] zbHook: expected " .. objDisplayName .. " to be a table or userdata, got " .. type(obj))
    return 0
end

--- Hooks methods on an object by wrapping them with custom functions.
-- @param tbl table of the form { obj1 = { methodName1 = wrapper1, methodName2 = wrapper2, ... }, obj2 = { ... }, ... }
function zbHook(tbl)
    if type(tbl) ~= "table" then
        print("[!] zbHook: expected a table, got " .. type(tbl))
        return
    end

    local nHooked = 0
    for objName, methods in pairs(tbl) do
        nHooked = nHooked + hookObj(objName, methods)
    end
    return nHooked
end
