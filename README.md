# Safari King

Safari statistics and visual helpers for Hypixel SkyBlock, with separate Fabric builds for Minecraft 26.2 and 26.3.

> **Safari King 2.0 includes Safari Critter Stats. Do not install both mods.**
> In particular, the Minecraft 26.2 versions overlap in capture counting, HUD and party replies. Remove `safari-critter-stats` before installing Safari King 2.0. Fabric will report the duplicate functionality as a mod conflict.
> Older Safari King 1.x releases do not include the statistics module.

## Safari statistics

- Track captures, Loot Share, unique species, total critters and total shards from server chat.
- Correctly handle Sparkling captures and Hideyho's special reward message.
- Automatically detect the current Safari biome and show its missing critters.
- Reset the run when entering a new Safari server; stop responding outside Safari.
- Announce each region's completion once, including elapsed time from the start of the run.
- Preserve the one-second shared reply interval and one-second duplicate-query cooldown.

### Party commands

Send these in Party Chat:

| Command | Response |
| --- | --- |
| `!unique`, `!uniq`, `!missing`, `!miss` | Missing count and up to five English names |
| `!miss i` / `!unique ice` | Region-specific missing count and up to five names |
| `!total` | Total captured critters and shards |
| `!progress` / `!prog` | Every region's progress and completion time |

Region aliases: `c/cave/cavern`, `f/forest`, `h/haunt/haunted`, `i/ice/icy`. Matching ignores case.

Examples:

```text
Missing 12: Cavernfish, Flitter, Shyworm, Driftling, Chuckwalla
Icy Missing 9: Strongarm, Tepid, Polaris, Shuddersquid, Billygoat
Total: 28 critters | 67 shards
Progress: Cavern done (2m 5s) | Forest 4/9 | Haunted 0/10 | Icy 1/9
Icy done (6m 12s)
```

With Party responses enabled, your own queries reach Party Chat so teammates can see them. With that option disabled, your own queries are intercepted locally and answered to Party Chat; teammates' queries are ignored. Pending duplicate queries are merged.

## Visual helpers

- Haunted Safari: Hideonwall, Hideyho and Duplico.
- Cavern Safari: Snoozle entities sent to the client, Scrappy and Rockmite Mounds.
- Forest Safari: Hideonfloor.
- Current Safari biome: Floor Drops.
- Galatea: Hideonleaf.
- Torrhus Canyon: Hideonsun and Pangolin.
- Crystal Hollows: static structure reward chests, excluding powder-mining chests.
- Glacite Mineshafts: unopened Lapis, Tungsten, Umber and Vanguard corpses.
- Glacial Cave: Frozen Treasure ice blocks, with through-wall fill and outline.

Helpers use only entities and blocks already sent to the client. They do not extend the server's entity tracking range. No entity coordinates are persisted after unloading.

## Settings UI

Open `/safariking`, `/sking` or Safari King's Mod Menu settings.

The navy, gold and teal control panel has three tabs: **Stats & HUD**, **Safari highlights**, and **Other areas**. Features have individual switches plus a master switch. Settings save immediately in `config/safari-king.json`.

Use **Move & resize HUD** to drag the HUD and scroll over it to scale from 50% to 250%. The statistics tab also shows capture/shard totals and region progress when there is enough screen space.

Turning statistics or the master switch off stops counting and queued replies. Re-enabling starts a fresh run if you are in Safari.

## Installation

Install the JAR matching your Minecraft version, Fabric Loader, Fabric API and the official Hypixel Mod API (1.0.2 or later). Mod Menu is optional. Skyblocker and Safari Critter Stats are not required.

Use the 26.2 asset only with Minecraft 26.2, and the 26.3 asset only with Minecraft 26.3.

This mod does not move the player, use items or capture mobs automatically. Party commands send chat messages.

## Build

Java 25 is required:

```shell
./gradlew clean build
```

The build runs the capture parser, statistics and party-queue regression checks. Output is in `build/libs/`.

Source branches: `mc26.2` and `mc26.3`.
