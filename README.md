Poon Client Enhanced — Fabric 1.21.11
Independent client-side Minecraft mod project. It is a new implementation and does not bundle CatLean code.
Included modules (v0.3.0)
Click GUI: Right Shift opens/closes the menu; Escape closes it. Five tabs, draggable panel, dark UI, toggle switches.
Visual: Fullbright, watermark.
Combat visuals: Local entity glow toggle and target name/distance HUD. These are visual helpers, not automated combat.
Movement: Sprint Assist while moving forward.
HUD: Coordinates, FPS, ping (when connected to a server), horizontal movement speed, and target info.
Utility: Auto Respawn.
Settings persistence: toggles are saved in `config/poonclient.properties` and restored on next launch.
Optional keybinds: Right Shift opens menu. Sprint, Fullbright, and Coordinates have configurable keybind entries in Minecraft's Controls menu; they default to unbound.
Cloud build: GitHub Actions installs Java 21 and builds the mod. No JDK installation is needed on your PC.
Build on GitHub (no local Java required)
Recommended if you do not want to install Java: use GitHub Actions below. The included `build.bat` is optional and requires JDK 21 installed locally.
Download and extract this ZIP.
Create a GitHub repository, for example `PoonClient-Enhanced`.
Upload the contents of the extracted `PoonClient-Enhanced` folder to the repository root. The root must contain `src`, `.github`, `build.gradle`, `settings.gradle`, and `gradle.properties` together.
Open Actions. Select Build Poon Client JAR. If it has not started automatically, click Run workflow.
Wait for the run to finish with a green check. Open the run and download the PoonClient-JAR artifact under Artifacts.
Extract the downloaded artifact ZIP. Use `poon-client-0.3.0.jar` (do not use a `-sources.jar` file).
Copy the JAR to `%appdata%\.minecraft\mods`.
Launch Minecraft Java 1.21.11 with Fabric Loader and a matching Fabric API version installed.
Optional local build on Windows
If JDK 21 is already installed, double-click `build.bat`. It downloads Gradle into `%LOCALAPPDATA%\PoonClientBuild`, builds the mod, and opens `build\libs`. You do not need this script when using GitHub Actions.
If the build fails
Open the failed run, expand Build mod, and copy the first error and its surrounding lines. If compilation succeeds but artifact upload fails, check that the workflow is running from the repository root and that `build/libs/` contains the built JAR.
Notes
This project has not been verified against every launcher, server, or mod combination. The GitHub Actions green check confirms compilation, not in-game behavior on every setup.
Entity glow is a local render hint and may not behave the same for every entity or server.
This is not a byte-for-byte recreation of CatLean and does not include automated PvP combat routines.
