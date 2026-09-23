package com.safariking;

import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.Locale;

/** Location authority comes from Hypixel's official Mod API. */
public final class KingLocationTracker {
    private static volatile boolean safari;
    private static volatile boolean galatea;
    private static volatile boolean torrhus;
    private static volatile boolean crystalHollows;
    private static volatile boolean glaciteMineshaft;
    private static volatile boolean winterIsland;
    private static volatile boolean glacialCave;
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
        crystalHollows = mode.equals("crystal_hollows") || map.contains("crystal hollows");
        glaciteMineshaft = mode.equals("mineshaft") || map.contains("glacite mineshaft");
        winterIsland = mode.equals("winter") || map.contains("jerry's workshop");
        glacialCave = false;
        if (!safari) safariZone = SafariZone.UNKNOWN;
        HighlightTargetTracker.resetSession();
    }

    public static void tick(Minecraft client) {
        glacialCave = winterIsland && isInGlacialCave(client);
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

    public static boolean isCrystalHollows() {
        return crystalHollows;
    }

    public static boolean isGlaciteMineshaft() {
        return glaciteMineshaft;
    }

    public static boolean isGlacialCave() {
        return glacialCave;
    }

    public static boolean isWinterIsland() {
        return winterIsland;
    }

    public static SafariZone safariZone() {
        return safariZone;
    }

    public static void reset() {
        safari = false;
        galatea = false;
        torrhus = false;
        crystalHollows = false;
        glaciteMineshaft = false;
        winterIsland = false;
        glacialCave = false;
        safariZone = SafariZone.UNKNOWN;
        HighlightTargetTracker.resetSession();
    }

    private static boolean isInGlacialCave(Minecraft client) {
        if (client.level == null) return false;

        Scoreboard scoreboard = client.level.getScoreboard();
        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective == null) return false;

        for (PlayerScoreEntry entry : scoreboard.listPlayerScores(objective)) {
            PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
            String line = team == null
                    ? entry.owner()
                    : team.getPlayerPrefix().getString() + entry.owner()
                    + team.getPlayerSuffix().getString();
            if (line.toLowerCase(Locale.ROOT).contains("glacial cave")) return true;
        }
        return false;
    }
}
