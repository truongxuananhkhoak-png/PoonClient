# Poon Client — Fabric 1.21.11

This is an early client-side mod scaffold, not a finished all-feature client.

## Build on Windows

1. Install a 64-bit JDK 21 and make sure `java -version` works in Command Prompt.
2. Keep the computer connected to the Internet for the first build.
3. Extract this project to a normal writable folder (avoid OneDrive-protected folders if possible).
4. Double-click `build.bat`.
5. If the build succeeds, the mod JAR will be in `build\libs\`.
6. Copy the remapped mod JAR (normally `poon-client-0.1.0.jar`, not `-sources.jar`) into the `mods` folder of a Minecraft 1.21.11 Fabric instance. Install Fabric API for 1.21.11 too.

The script downloads Gradle into `%LOCALAPPDATA%\PoonClientBuild`; it does not install Gradle system-wide.

## Current functionality / limitations

- Right Shift opens/closes a draggable ClickGUI.
- Sprint toggle is a basic client-side sprint helper.
- Entity Glow toggle attempts to apply glowing to rendered entities client-side; server behavior and rendering limitations may vary.
- Coordinates HUD is currently only a placeholder toggle and does not draw coordinates yet.
- This is an early scaffold. It does not implement every feature from the reference TikTok video and has not been tested in-game.
