# ZBetterWorkshopUpload

A Project Zomboid mod that filters unwanted files from Steam Workshop uploads and provides a preview of what will be uploaded.

## ☕ Support the Developer

If you find this mod useful, consider supporting the developer with a coffee!

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/zed_0xff)

## What It Does

ZBetterWorkshopUpload automatically filters out unwanted files (like `.git`, `.DS_Store`, build artifacts, etc.) when uploading mods to the Steam Workshop. It also provides a preview of exactly which files will be uploaded before you submit.

## Features

- ✅ **Automatic file filtering**: Excludes common unwanted files by default (`.git`, `.DS_Store`, `.gradle`, `.idea`, `.vscode`, logs, temp files, etc.)
- ✅ **Configurable exclusion patterns**: Customize which files to exclude via mod options
- ✅ **Preview before upload**: See exactly which files will be uploaded in the workshop submission screen
- ✅ **`.workshopignore` support**: Create `.workshopignore` files in your mod directories (similar to `.gitignore`) for project-specific exclusions
- ✅ **Recursive ignore files**: `.workshopignore` files are checked recursively up the directory tree
- ✅ **Comment support**: `.workshopignore` files support comments (lines starting with `#`) and empty lines

## Installation

1. **Prerequisites**: You must have [ZombieBuddy](https://github.com/zed-0xff/ZombieBuddy) installed and configured first
2. **Enable the mod**: Enable ZBetterWorkshopUpload in the Project Zomboid mod manager
3. **Configure (optional)**: Adjust exclusion patterns in the mod options if needed

## Usage

### Default Behavior

By default, ZBetterWorkshopUpload excludes the following patterns:
- `.DS_Store` (macOS system files)
- `.git*` (Git repository files)
- `.gradle` (Gradle build files)
- `.idea` (IntelliJ IDEA project files)
- `.vscode` (VS Code project files)
- `*.log` (Log files)
- `*.tmp` (Temporary files)
- `Thumbs.db` (Windows thumbnail cache)
- `tmp` (Temporary directories)

### Configuring Exclusion Patterns

1. Open the game's mod options
2. Select "ZBetterWorkshopUpload"
3. Edit the "Excluded Patterns" field
4. Separate patterns with semicolons (e.g., `.git; *.tmp; .DS_Store`)
5. Patterns support wildcards (`*` matches any sequence of characters)

### Using `.workshopignore` Files

For project-specific exclusions, create a `.workshopignore` file in any directory of your mod. The file format is similar to `.gitignore`:

```
# This is a comment
*.log
*.tmp
.DS_Store

# Exclude a specific directory
build/
dist/

# Exclude files matching a pattern
*.bak
*.swp
```

**Features:**
- Empty lines are ignored
- Lines starting with `#` are treated as comments
- Patterns support wildcards (`*`)
- Ignore files are checked recursively up the directory tree (most specific first)

**Example:**
```
# Exclude build artifacts
*.class
*.jar
build/
target/

# Exclude IDE files
.idea/
.vscode/
*.iml

# Exclude OS files
.DS_Store
Thumbs.db
```

### Preview Before Upload

When uploading a mod to the Steam Workshop, ZBetterWorkshopUpload adds a preview listbox showing exactly which files will be uploaded. This helps you verify that unwanted files are being excluded correctly.

## How It Works

ZBetterWorkshopUpload uses [ZombieBuddy](https://github.com/zed-0xff/ZombieBuddy) to patch the game's workshop submission process:

1. **Intercepts uploads**: Patches `SteamWorkshop.SubmitWorkshopItem()` to intercept upload requests
2. **Filters content**: Creates a filtered copy of your mod content, excluding unwanted files
3. **Temporary folder**: Uses a temporary filtered folder for the upload
4. **Cleanup**: Automatically cleans up temporary files after upload completes
5. **Preview integration**: Patches the workshop submission screen to show filtered file list

### Technical Details

- **Java patches**: Uses ZombieBuddy's `@Patch` annotations to intercept workshop methods
- **Lua integration**: Exposes Java functionality to Lua for the preview UI
- **Pattern matching**: Supports wildcard patterns for flexible file exclusion
- **Caching**: Caches `.workshopignore` file contents for performance

## Building

1. Navigate to the Java project directory:
   ```bash
   cd 42/media/java/client
   ```

2. Build the JAR:
   ```bash
   gradle build
   ```

3. The JAR will be created at `build/libs/client.jar`

## Project Structure

```
ZBetterWorkshopUpload/
├── 42/
│   ├── mod.info
│   └── media/
│       ├── java/
│       │   └── client/
│       │       ├── build.gradle
│       │       ├── src/
│       │       │   ├── Main.java
│       │       │   ├── ZBetterWorkshopUpload.java
│       │       │   └── WorkshopContentFilter.java
│       │       └── build/
│       │           └── libs/
│       │               └── client.jar
│       └── lua/
│           └── client/
│               ├── ZBetterWorkshopUpload.lua
│               └── ZBetterWorkshopUploadOptions.lua
├── common/
├── LICENSE.txt
└── README.md
```

## Requirements

- **Project Zomboid** (Build 42+)
- **ZombieBuddy** - Required framework for Java mods
- **Java 17** (required by the game)

## Troubleshooting

### Preview not showing files

- Ensure ZombieBuddy is properly installed and working
- Check that the mod is enabled in the mod manager
- Verify the Java JAR file is built and present

### Files still being uploaded that should be excluded

- Check your exclusion patterns in mod options
- Verify `.workshopignore` file syntax (if using)
- Ensure patterns match the file paths correctly (use wildcards if needed)

### Mod not working

- Verify ZombieBuddy is installed and the game shows `[ZB]` in the version string
- Check that the mod is enabled
- Rebuild the Java JAR file if you've made changes

## Links

- **GitHub**: https://github.com/zed-0xff/ZBetterWorkshopUpload
- **ZombieBuddy Framework**: [ZombieBuddy](https://github.com/zed-0xff/ZombieBuddy) - The framework this mod is built on

## License

See [LICENSE.txt](LICENSE.txt) file for details.

## Author

zed-0xff

## Disclaimer

This mod modifies the Steam Workshop upload process. Always review the preview list before uploading to ensure the correct files are being included. Use at your own risk.

