# Safari King

Client-side visual enhancements for Minecraft 26.1.2 on Hypixel SkyBlock.

Features are location-gated and only inspect entities and chunks already sent
to the client:

- Haunted Safari: Hideonwall, Hideyho, and Duplico
- Cavern Safari: Snoozle entities already sent to the client, Scrappy, and Rockmite Mounds
- Forest Safari: Hideonfloor
- Current Critter Safari biome: Floor Drop String displays
- Galatea: Hideonleaf
- Torrhus Canyon: Hideonsun and Pangolins in Pangolin Hideaways

Use `/safariking` or Mod Menu to configure individual helpers.

Tracked entities use Minecraft's vanilla glow pass, so their outline remains
visible behind blocks. Highlighted
targets are kept renderable up to 128 blocks when the server has sent them.
No world coordinates are cached: a highlight disappears as soon as its entity
is no longer present on the client. Haunted matching is deliberately strict so
one Hideonwall or Hideyho cannot highlight its surrounding animation entities.

This mod does not move the player, use items, capture mobs, or send gameplay
commands. Hypixel modifications are used at your own risk.
