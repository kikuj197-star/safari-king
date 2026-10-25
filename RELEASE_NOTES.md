# Safari King 2.1.0

- Added up to three teammate rows below the missing-critter HUD, showing the teammate's biome and its shared Safari collection progress.
- Loaded player positions update four times per second. Loot Share provides recent biome activity for distant teammates. `*` means last observed within 30 seconds; expired locations show `Unknown ?/?`.
- Party membership refreshes through the official Hypixel Mod API. Teammates do not need this mod, and the HUD sends no chat messages.
- Added a **Teammate progress** toggle in **Stats & HUD**. Leaving Safari or starting a new run clears party locations.
- Added the Safari King crown icon.

Separate JARs are provided for Minecraft 26.2 and 26.3. **Safari King includes Safari Critter Stats; do not install both mods.**

Requires Java 25, Fabric Loader, Fabric API and Hypixel Mod API 1.0.2+. Mod Menu is optional.

---


# Safari King 2.0.1 - Minecraft 26.3

Fix a render-frame crash when a visual helper starts drawing, including on entering Torrhus Canyon. Minecraft 26.3's line snippet no longer provides a color target; Safari King now explicitly supplies the same single color target as vanilla lines while keeping depth testing disabled for through-wall outlines.

Added a regression check against the actual Minecraft 26.3 pipeline templates. Statistics, HUD, party queries and individual highlight settings are unchanged. Requires the same dependencies as 2.0.0.

## Included in Safari King 2.0

Safari Critter Stats is now built into Safari King for Minecraft 26.2 and 26.3.

**Do not install Safari Critter Stats alongside this release.** The Minecraft 26.2 mods duplicate capture counting, missing-critter HUD and party responses. Remove the standalone stats JAR first; Fabric now checks for this conflict.

- Capture and Loot Share statistics, total critters and shards, and unique-species tracking.
- Missing-critter HUD with automatic Safari biome detection and drag/scroll layout editor.
- Party queries: `!unique`, `!uniq`, `!missing`, `!miss`, `!total`, `!progress`, `!prog`.
- Region-specific queries, five-name output limit and one-second queued replies.
- Region completion announcements and elapsed times; counters reset for each Safari visit.
- Redesigned settings UI with Stats & HUD, Safari highlights and Other areas tabs.
- Existing Safari, Galatea, Torrhus, Crystal Hollows, Mineshaft and Glacial Cave visual helpers.

Requires Java 25, Fabric API, Fabric Loader and Hypixel Mod API 1.0.2+. Mod Menu is optional.

Pick the asset for your exact Minecraft series. The 26.2 and 26.3 files are separate builds.
