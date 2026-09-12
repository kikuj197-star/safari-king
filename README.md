# Safari King

Client-side visual enhancements for Minecraft 26.1.2 on Hypixel SkyBlock.

Features are location-gated and only inspect entities and chunks already sent
to the client:

- Haunted Safari: Hideonwall and Hideyho
- Cavern Safari: Snoozle entities already sent to the client, and Scrappy
- Forest Safari: Hideonfloor
- Current Critter Safari biome: Floor Drop String displays
- Torrhus Canyon Pangolin Hideaways: Pangolins

Use `/safariking` or Mod Menu to configure individual helpers.

To compare what the server sends before and after opening a Snoozle wall, stand
near it and run `/skingdiag before`, open the wall without catching the target,
then run `/skingdiag after`. The mod writes a local report under
`config/safari-king/`. These snapshots only run when requested and are not sent
to the server.

Tracked entities use Minecraft's vanilla glow pass, so their outline remains
visible behind blocks like Skyblocker's Rockmite Mound highlight. Highlighted
targets are kept renderable up to 128 blocks when the server has sent them.
No world coordinates are cached: a highlight disappears as soon as its entity
is no longer present on the client. Haunted matching is deliberately strict so
one Hideonwall or Hideyho cannot highlight its surrounding animation entities.

This mod does not move the player, use items, capture mobs, or send gameplay
commands. Hypixel modifications are used at your own risk.
