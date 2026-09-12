package com.safariking;

import com.mojang.authlib.properties.Property;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** On-demand, local before/after snapshots used to identify hidden Safari entities. */
public final class EntityDiagnostics {
    private static final double CAPTURE_RADIUS = 64.0;
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final DateTimeFormatter REPORT_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static Snapshot before;

    private EntityDiagnostics() {
    }

    public static int captureBefore() {
        Minecraft client = Minecraft.getInstance();
        Snapshot snapshot = capture(client);
        if (snapshot == null) return fail(client, "Join a world before taking a diagnostic snapshot.");
        before = snapshot;
        return success(client, "BEFORE saved locally in memory: " + snapshot.entities().size()
                + " entities within " + (int) CAPTURE_RADIUS + " blocks.");
    }

    public static int captureAfter() {
        Minecraft client = Minecraft.getInstance();
        if (before == null) return fail(client, "Run /skingdiag before first.");
        Snapshot after = capture(client);
        if (after == null) return fail(client, "Join a world before taking a diagnostic snapshot.");

        try {
            Path directory = FabricLoader.getInstance().getConfigDir().resolve("safari-king");
            Files.createDirectories(directory);
            Path report = directory.resolve("entity-diagnostic-" + FILE_TIME.format(LocalDateTime.now()) + ".txt");
            Files.writeString(report, buildReport(before, after), StandardCharsets.UTF_8);
            before = null;
            return success(client, "AFTER captured. Report: " + report.toAbsolutePath());
        } catch (IOException exception) {
            SafariKingMod.LOGGER.error("Could not write Safari King entity diagnostic", exception);
            return fail(client, "Could not write the report; see latest.log.");
        }
    }

    public static int clear() {
        before = null;
        return success(Minecraft.getInstance(), "Stored BEFORE snapshot cleared.");
    }

    public static int usage() {
        return success(Minecraft.getInstance(), "Use /skingdiag before, then open the wall, then /skingdiag after.");
    }

    public static void reset() {
        before = null;
    }

    private static Snapshot capture(Minecraft client) {
        if (client.level == null || client.player == null) return null;
        Vec3 playerPos = client.player.position();
        double maxDistanceSquared = CAPTURE_RADIUS * CAPTURE_RADIUS;
        List<EntityInfo> found = new ArrayList<>();

        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity.isRemoved() || entity == client.player
                    || entity.distanceToSqr(client.player) > maxDistanceSquared) continue;
            found.add(describe(entity, playerPos));
        }

        found.sort(Comparator.comparing(EntityInfo::type)
                .thenComparing(EntityInfo::name)
                .thenComparingDouble(EntityInfo::distance)
                .thenComparing(EntityInfo::uuid));
        Map<UUID, EntityInfo> byUuid = new LinkedHashMap<>();
        for (EntityInfo info : found) byUuid.put(info.uuid(), info);
        return new Snapshot(LocalDateTime.now(), KingLocationTracker.safariZone(), playerPos, byUuid);
    }

    private static EntityInfo describe(Entity entity, Vec3 playerPos) {
        Vec3 pos = entity.position();
        double distance = Math.sqrt(pos.distanceToSqr(playerPos));
        String type = String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
        String name = clean(entity.getName().getString());
        String customName = entity.hasCustomName() && entity.getCustomName() != null
                ? clean(entity.getCustomName().getString()) : "";
        String text = "";
        String item = "";
        String texture = "";

        if (entity instanceof Display.TextDisplay display) {
            Display.TextDisplay.TextRenderState state = display.textRenderState();
            if (state != null) text = clean(state.text().getString());
        }
        if (entity instanceof Display.ItemDisplay display) {
            Display.ItemDisplay.ItemRenderState state = display.itemRenderState();
            ItemStack stack = state == null ? ItemStack.EMPTY : state.itemStack();
            if (!stack.isEmpty()) {
                item = BuiltInRegistries.ITEM.getKey(stack.getItem())
                        + " x" + stack.getCount()
                        + " hover=\"" + clean(stack.getHoverName().getString()) + "\""
                        + " components=" + clean(stack.getComponents().toString());
                texture = headTexture(stack);
            }
        }
        if (entity instanceof RemotePlayer player) {
            texture = player.getGameProfile().properties().get("textures").stream()
                    .filter(property -> property != null && property.value() != null)
                    .map(Property::value)
                    .reduce((left, right) -> left + "," + right)
                    .orElse("");
        }

        BlockPos blockPos = entity.blockPosition();
        String block = String.valueOf(BuiltInRegistries.BLOCK.getKey(
                entity.level().getBlockState(blockPos).getBlock()));
        String blockBelow = String.valueOf(BuiltInRegistries.BLOCK.getKey(
                entity.level().getBlockState(blockPos.below()).getBlock()));

        String details = String.format(Locale.ROOT,
                "id=%d uuid=%s type=%s class=%s pos=(%.3f,%.3f,%.3f) distance=%.2f "
                        + "name=\"%s\" custom=\"%s\" text=\"%s\" invisible=%s noGravity=%s "
                        + "size=(%.3f,%.3f) block=%s below=%s item={%s} texture={%s}",
                entity.getId(), entity.getUUID(), type, entity.getClass().getName(),
                pos.x(), pos.y(), pos.z(), distance, name, customName, text,
                entity.isInvisible(), entity.isNoGravity(), entity.getBbWidth(), entity.getBbHeight(),
                block, blockBelow, item, texture);
        return new EntityInfo(entity.getUUID(), type, name, distance, details);
    }

    private static String headTexture(ItemStack stack) {
        ResolvableProfile profile = stack.get(DataComponents.PROFILE);
        if (profile == null) return "";
        return profile.partialProfile().properties().get("textures").stream()
                .filter(property -> property != null && property.value() != null)
                .map(Property::value)
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    private static String buildReport(Snapshot beforeSnapshot, Snapshot afterSnapshot) {
        StringBuilder report = new StringBuilder();
        report.append("Safari King entity diagnostic\n")
                .append("Capture radius: ").append((int) CAPTURE_RADIUS).append(" blocks\n")
                .append("BEFORE: ").append(REPORT_TIME.format(beforeSnapshot.time()))
                .append(" zone=").append(beforeSnapshot.zone())
                .append(" player=").append(formatPos(beforeSnapshot.playerPos())).append('\n')
                .append("AFTER:  ").append(REPORT_TIME.format(afterSnapshot.time()))
                .append(" zone=").append(afterSnapshot.zone())
                .append(" player=").append(formatPos(afterSnapshot.playerPos())).append("\n\n");

        List<EntityInfo> added = new ArrayList<>();
        List<EntityInfo> removed = new ArrayList<>();
        List<String> changed = new ArrayList<>();
        for (Map.Entry<UUID, EntityInfo> entry : afterSnapshot.entities().entrySet()) {
            EntityInfo old = beforeSnapshot.entities().get(entry.getKey());
            if (old == null) added.add(entry.getValue());
            else if (!old.details().equals(entry.getValue().details())) {
                changed.add("BEFORE " + old.details() + "\nAFTER  " + entry.getValue().details());
            }
        }
        for (Map.Entry<UUID, EntityInfo> entry : beforeSnapshot.entities().entrySet()) {
            if (!afterSnapshot.entities().containsKey(entry.getKey())) removed.add(entry.getValue());
        }

        report.append("=== ADDED (possible newly revealed/spawned entities): ")
                .append(added.size()).append(" ===\n");
        appendEntities(report, added);
        report.append("\n=== REMOVED: ").append(removed.size()).append(" ===\n");
        appendEntities(report, removed);
        report.append("\n=== CHANGED: ").append(changed.size()).append(" ===\n");
        for (String line : changed) report.append(line).append('\n');
        report.append("\n=== COMPLETE BEFORE SNAPSHOT: ")
                .append(beforeSnapshot.entities().size()).append(" ===\n");
        appendEntities(report, beforeSnapshot.entities().values());
        report.append("\n=== COMPLETE AFTER SNAPSHOT: ")
                .append(afterSnapshot.entities().size()).append(" ===\n");
        appendEntities(report, afterSnapshot.entities().values());
        return report.toString();
    }

    private static void appendEntities(StringBuilder target, Iterable<EntityInfo> entities) {
        for (EntityInfo info : entities) target.append(info.details()).append('\n');
    }

    private static String formatPos(Vec3 pos) {
        return String.format(Locale.ROOT, "(%.3f,%.3f,%.3f)", pos.x(), pos.y(), pos.z());
    }

    private static String clean(String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ');
    }

    private static int success(Minecraft client, String message) {
        if (client.player != null) client.player.sendSystemMessage(
                Component.literal("[Safari King] " + message));
        return 1;
    }

    private static int fail(Minecraft client, String message) {
        if (client.player != null) client.player.sendSystemMessage(
                Component.literal("[Safari King] " + message));
        return 0;
    }

    private record Snapshot(LocalDateTime time, SafariZone zone, Vec3 playerPos,
                            Map<UUID, EntityInfo> entities) {
    }

    private record EntityInfo(UUID uuid, String type, String name, double distance, String details) {
    }
}
