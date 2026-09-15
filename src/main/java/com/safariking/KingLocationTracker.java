package com.safariking;

import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

import java.util.Locale;

/** Location authority comes from Hypixel's official Mod API. */
public final class KingLocationTracker {
    private static volatile boolean safari;
    private static volatile boolean galatea;
    private static volatile boolean torrhus;
    private static volatile SafariZone safariZone = SafariZone.UNKNOWN;

    private KingLocationTracker() {
    }

    public static void initialize() {
        HypixelModAPI api = HypixelModAPI.getInstance();
        api.createHandler(ClientboundLocationPacket.class, KingLocationTracker::onLocation);
        api.subscribeToEventPacket(ClientboundLocationPacket.class);
    }

    private static void onLocation(ClientboundLocationPacket packet) {
        String mode = packet.getMode().orElse("").toLowerCase(Locale.ROOT);
        String map = packet.getMap().orElse("").toLowerCase(Locale.ROOT);
        safari = mode.equals("safari") || map.contains("critter safari") || map.equals("safari");
        galatea = mode.equals("foraging_2") || map.contains("moonglade marsh") || map.contains("galatea");
        torrhus = mode.equals("foraging_3") || map.contains("torrhus canyon");
        if (!safari) safariZone = SafariZone.UNKNOWN;
        HighlightTargetTracker.reset();
    }

    public static void tick(Minecraft client) {
        if (!safari || client.player == null || client.level == null) {
            safariZone = SafariZone.UNKNOWN;
            return;
        }

        BlockPos pos = client.player.blockPosition();
        if (pos.getX() >= -58 && pos.getX() <= -41
                && pos.getY() >= 67 && pos.getY() <= 75
                && pos.getZ() >= 19 && pos.getZ() <= 27) {
            safariZone = SafariZone.CENTER;
            return;
        }

        Holder<Biome> biome = client.level.getBiome(pos);
        safariZone = biome.unwrapKey()
                .map(key -> SafariZone.fromBiomeId(key.identifier().toString()))
                .orElse(SafariZone.UNKNOWN);
    }

    public static boolean isSafari() {
        return safari;
    }

    public static boolean isTorrhus() {
        return torrhus;
    }

    public static boolean isGalatea() {
        return galatea;
    }

    public static SafariZone safariZone() {
        return safariZone;
    }

    public static void reset() {
        safari = false;
        galatea = false;
        torrhus = false;
        safariZone = SafariZone.UNKNOWN;
        HighlightTargetTracker.reset();
    }
}
