package com.safariking;

import com.mojang.authlib.properties.Property;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HighlightTargetTracker {
    public static final int HIDEONWALL_COLOR = 0xD050FF;
    public static final int HIDEYHO_COLOR = 0xFFD54A;
    public static final int DUPLICO_COLOR = 0xFF5555;
    public static final int SNOOZLE_COLOR = 0x55FF55;
    public static final int SCRAPPY_COLOR = 0xFF5C70;
    public static final int ROCKMITE_COLOR = 0x55FFFF;
    public static final int HIDEONLEAF_COLOR = 0x55FF55;
    public static final int HIDEONSUN_COLOR = 0xFFAA00;
    public static final int PANGOLIN_COLOR = 0xFF9B42;
    public static final int HIDEONFLOOR_COLOR = 0xFF4DFF;
    public static final int FLOOR_DROP_COLOR = 0x35E6FF;
    public static final int LAPIS_CORPSE_COLOR = 0x5555FF;
    public static final int TUNGSTEN_CORPSE_COLOR = 0xAAAAAA;
    public static final int UMBER_CORPSE_COLOR = 0xFFAA00;
    public static final int VANGUARD_CORPSE_COLOR = 0x55FFFF;
    public static final int FROZEN_TREASURE_COLOR = 0x55FF55;
    public static final double FORCED_RENDER_DISTANCE = 128.0;

    private static final String HIDEYHO_TEXTURE =
            "ewogICJ0aW1lc3RhbXAiIDogMTc4MjgzMjk3MDkzNywKICAicHJvZmlsZUlkIiA6ICJmZDIwMGYwMDE4OTI0NzgxODI5OWIzZjE5Yzc4Y2E3MSIsCiAgInByb2ZpbGVOYW1lIiA6ICJ0dXNnIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzM1MDRmMWYyMzI3YTUxMTBlNjQzYmI4NjY3MDgyNTEyODE1ZmE0MzRhMjllZDM3ZjRjYTgzYmIxNmQyZGI1MzMiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==";

    private static final String ROCKMITE_TEXTURE_HASH =
            "5dbaab74d1acd0abe9d04abe9928725de5d4495fcb63b647228caf6944c20800";
    private static final Pattern CORPSE_LOOT_PATTERN = Pattern.compile(
            "\\b(LAPIS|TUNGSTEN|UMBER|VANGUARD) CORPSE LOOT!");

    private static volatile Map<Integer, Integer> colors = Map.of();
    private static volatile List<HighlightBox> boxes = List.of();
    private static final Set<BlockPos> openedCorpses = new HashSet<>();

    private HighlightTargetTracker() {
    }

    public static void tick(Minecraft client) {
        if (client.level == null || client.player == null) {
            reset();
            return;
        }

        KingConfig config = SafariKingMod.CONFIG;
        boolean safari = KingLocationTracker.isSafari();
        boolean galatea = KingLocationTracker.isGalatea();
        boolean torrhus = KingLocationTracker.isTorrhus();
        boolean glaciteMineshaft = KingLocationTracker.isGlaciteMineshaft();
        boolean glacialCave = KingLocationTracker.isGlacialCave();
        if (!safari && !galatea && !torrhus && !glaciteMineshaft && !glacialCave) {
            reset();
            return;
        }

        Map<Integer, Integer> nextColors = new HashMap<>();
        List<HighlightBox> nextBoxes = new ArrayList<>();
        SafariZone zone = KingLocationTracker.safariZone();

        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity.isRemoved() || entity == client.player || entity instanceof Display.TextDisplay) continue;

            int color = 0;
            if (glacialCave && config.frozenTreasures && entity instanceof ArmorStand stand
                    && isFrozenTreasure(stand)) {
                color = FROZEN_TREASURE_COLOR;
                BlockPos ice = stand.blockPosition().above(2);
                nextBoxes.add(new HighlightBox(new AABB(ice).inflate(0.01), color));
            } else if (glaciteMineshaft && config.glaciteMineshaftCorpses && entity instanceof ArmorStand stand) {
                color = corpseColor(stand);
            } else if (entity instanceof ArmorStand) {
                continue;
            } else if (config.floorDrops && safari && isSafariBiome(zone) && zoneAt(entity) == zone
                    && isFloorDropDisplay(entity)) {
                color = FLOOR_DROP_COLOR;
            } else if (safari && zone == SafariZone.HAUNTED) {
                if (config.hideonwall && isHideonwall(entity)) {
                    color = HIDEONWALL_COLOR;
                } else if (config.hideyho && entity instanceof RemotePlayer && isHideyho(entity)) {
                    color = HIDEYHO_COLOR;
                } else if (config.duplico && isDuplico(entity)) {
                    color = DUPLICO_COLOR;
                }
            } else if (safari && zone == SafariZone.CAVERN) {
                String label = entityName(entity) + " " + nearbyLabel(entity);
                if (config.rockmite && isRockmite(entity)) {
                    color = ROCKMITE_COLOR;
                } else if (config.snoozleWalls && entity instanceof Sniffer && containsName(label, "snoozle")) {
                    color = SNOOZLE_COLOR;
                } else if (config.scrappy && entity instanceof RemotePlayer && containsName(label, "scrappy")) {
                    color = SCRAPPY_COLOR;
                }
            } else if (safari && zone == SafariZone.FOREST && config.hideonfloor) {
                if (isHideonfloor(entity)) {
                    color = HIDEONFLOOR_COLOR;
                }
            } else if (galatea && config.hideonleaf && isHideonleaf(entity)) {
                color = HIDEONLEAF_COLOR;
            } else if (torrhus) {
                if (config.hideonsun && isHideonsun(entity)) {
                    color = HIDEONSUN_COLOR;
                } else if (config.pangolin && entity instanceof Armadillo) {
                    color = PANGOLIN_COLOR;
                }
            }

            if (color != 0) {
                nextColors.put(entity.getId(), color);
                nextBoxes.add(new HighlightBox(entity.getBoundingBox().inflate(0.06), color));
            }
        }

        colors = Map.copyOf(nextColors);
        boxes = List.copyOf(nextBoxes);
    }

    public static int colorFor(Entity entity) {
        return colors.getOrDefault(entity.getId(), 0);
    }

    public static boolean shouldForceRender(Entity entity, double distanceSquared) {
        return colorFor(entity) != 0
                && distanceSquared <= FORCED_RENDER_DISTANCE * FORCED_RENDER_DISTANCE;
    }

    public static List<HighlightBox> boxes() {
        return boxes;
    }

    public static void onGameMessage(String message) {
        if (!KingLocationTracker.isGlaciteMineshaft()) return;

        Matcher matcher = CORPSE_LOOT_PATTERN.matcher(message.toUpperCase(Locale.ROOT));
        if (!matcher.find()) return;

        String expectedItemId = switch (matcher.group(1)) {
            case "LAPIS" -> "LAPIS_ARMOR_HELMET";
            case "TUNGSTEN" -> "MINERAL_HELMET";
            case "UMBER" -> "ARMOR_OF_YOG_HELMET";
            case "VANGUARD" -> "VANGUARD_HELMET";
            default -> "";
        };

        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;

        ArmorStand nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof ArmorStand stand)
                    || openedCorpses.contains(stand.blockPosition())
                    || !corpseItemId(stand).equals(expectedItemId)) continue;

            double distance = stand.distanceToSqr(client.player);
            if (distance < nearestDistance) {
                nearest = stand;
                nearestDistance = distance;
            }
        }

        if (nearest != null) {
            openedCorpses.add(nearest.blockPosition());
            tick(client);
        }
    }

    public static void reset() {
        colors = Map.of();
        boxes = List.of();
    }

    public static void resetSession() {
        openedCorpses.clear();
        reset();
    }

    private static boolean isHideonwall(Entity entity) {
        if (entity instanceof Shulker shulker && shulker.getColor() == DyeColor.PURPLE) return true;
        return entity instanceof Display.ItemDisplay display
                && displayItem(display).is(Items.DYED_SHULKER_BOX.purple());
    }

    private static int corpseColor(ArmorStand stand) {
        if (openedCorpses.contains(stand.blockPosition())) return 0;
        return switch (corpseItemId(stand)) {
            case "LAPIS_ARMOR_HELMET" -> LAPIS_CORPSE_COLOR;
            case "MINERAL_HELMET" -> TUNGSTEN_CORPSE_COLOR;
            case "ARMOR_OF_YOG_HELMET" -> UMBER_CORPSE_COLOR;
            case "VANGUARD_HELMET" -> VANGUARD_CORPSE_COLOR;
            default -> 0;
        };
    }

    private static String corpseItemId(ArmorStand stand) {
        if (stand.hasCustomName() || stand.isInvisible() || stand.showBasePlate()) return "";

        ItemStack helmet = stand.getItemBySlot(EquipmentSlot.HEAD);
        CustomData customData = helmet.get(DataComponents.CUSTOM_DATA);
        return customData == null ? "" : customData.copyTag().getStringOr("id", "");
    }

    private static boolean isFrozenTreasure(ArmorStand stand) {
        ItemStack helmet = stand.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.isEmpty()) return false;

        int equippedItems = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.MAINHAND, EquipmentSlot.FEET, EquipmentSlot.LEGS,
                EquipmentSlot.CHEST, EquipmentSlot.HEAD, EquipmentSlot.OFFHAND}) {
            if (!stand.getItemBySlot(slot).isEmpty()) equippedItems++;
        }
        if (equippedItems != 1) return false;
        if (helmet.is(Items.PLAYER_HEAD) && helmet.getHoverName().getString().endsWith("Head")) return false;

        BlockPos ice = stand.blockPosition().above(2);
        return stand.level().getBlockState(ice).is(Blocks.ICE)
                || stand.level().getBlockState(ice).is(Blocks.PACKED_ICE);
    }

    private static boolean isHideonfloor(Entity entity) {
        if (entity instanceof Shulker shulker && shulker.getColor() == DyeColor.GREEN) return true;
        return entity instanceof Display.ItemDisplay display
                && displayItem(display).is(Items.DYED_SHULKER_BOX.green());
    }

    private static boolean isHideonleaf(Entity entity) {
        return entity instanceof Shulker shulker && shulker.getColor() == DyeColor.GREEN;
    }

    private static boolean isHideonsun(Entity entity) {
        if (!(entity instanceof Shulker shulker)) return false;
        DyeColor color = shulker.getColor();
        return color == DyeColor.YELLOW || color == DyeColor.ORANGE || color == DyeColor.BROWN;
    }

    private static boolean isHideyho(Entity entity) {
        if (!(entity instanceof RemotePlayer player)) return false;
        for (Property property : player.getGameProfile().properties().get("textures")) {
            if (HIDEYHO_TEXTURE.equals(property.value())) return true;
        }
        return containsName(entityName(entity), "hideyho");
    }

    private static boolean isDuplico(Entity entity) {
        if (!(entity instanceof Display.ItemDisplay display)
                || display.getPosRotInterpolationDuration() != 3) return false;
        ItemStack stack = displayItem(display);
        return !stack.is(Items.PLAYER_HEAD) && !stack.is(Items.DYED_SHULKER_BOX.purple());
    }

    private static boolean isRockmite(Entity entity) {
        if (!(entity instanceof Display.ItemDisplay display)
                || display.getPosRotInterpolationDuration() != 0) return false;
        ItemStack stack = displayItem(display);
        if (!stack.is(Items.PLAYER_HEAD)) return false;

        ResolvableProfile profile = stack.get(DataComponents.PROFILE);
        if (profile == null) return false;
        for (Property property : profile.partialProfile().properties().get("textures")) {
            if (property != null && textureContains(property.value(), ROCKMITE_TEXTURE_HASH)) return true;
        }
        return false;
    }

    private static boolean textureContains(String encodedTexture, String expectedHash) {
        if (encodedTexture == null || encodedTexture.isBlank()) return false;
        try {
            String decoded = new String(Base64.getDecoder().decode(encodedTexture), StandardCharsets.UTF_8);
            return decoded.toLowerCase(Locale.ROOT).contains(expectedHash);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static boolean isFloorDropDisplay(Entity entity) {
        if (!(entity instanceof Display.ItemDisplay display) || !displayItem(display).is(Items.STRING)) {
            return false;
        }

        int count = 0;
        AABB search = display.getBoundingBox().inflate(1.0);
        for (Display.ItemDisplay nearby : entity.level().getEntitiesOfClass(Display.ItemDisplay.class, search)) {
            if (nearby.blockPosition().equals(display.blockPosition()) && displayItem(nearby).is(Items.STRING)) {
                count++;
                if (count >= 3) return true;
            }
        }
        return false;
    }

    private static ItemStack displayItem(Display.ItemDisplay display) {
        Display.ItemDisplay.ItemRenderState state = display.itemRenderState();
        return state == null ? ItemStack.EMPTY : state.itemStack();
    }

    private static boolean isSafariBiome(SafariZone zone) {
        return zone == SafariZone.CAVERN || zone == SafariZone.FOREST
                || zone == SafariZone.HAUNTED || zone == SafariZone.ICY;
    }

    private static SafariZone zoneAt(Entity entity) {
        return entity.level().getBiome(entity.blockPosition()).unwrapKey()
                .map(key -> SafariZone.fromBiomeId(key.identifier().toString()))
                .orElse(SafariZone.UNKNOWN);
    }

    private static String nearbyLabel(Entity entity) {
        AABB search = entity.getBoundingBox().inflate(0.8, 2.8, 0.8);
        StringBuilder result = new StringBuilder();
        for (ArmorStand stand : entity.level().getEntitiesOfClass(ArmorStand.class, search, ArmorStand::hasCustomName)) {
            append(result, stand.getCustomName());
        }
        for (Display.TextDisplay display : entity.level().getEntitiesOfClass(Display.TextDisplay.class, search)) {
            Display.TextDisplay.TextRenderState state = display.textRenderState();
            if (state != null) append(result, state.text());
        }
        return result.toString();
    }

    private static String entityName(Entity entity) {
        return entity.hasCustomName() && entity.getCustomName() != null
                ? entity.getCustomName().getString()
                : "";
    }

    private static void append(StringBuilder target, Component component) {
        if (component == null) return;
        if (!target.isEmpty()) target.append(' ');
        target.append(component.getString());
    }

    private static boolean containsName(String text, String expected) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(expected);
    }

    public record HighlightBox(AABB bounds, int color) {
    }
}
