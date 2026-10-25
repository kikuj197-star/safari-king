package com.safariking.stats;

import com.safariking.SafariKingMod;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPartyInfoPacket;
import net.hypixel.modapi.packet.impl.serverbound.ServerboundPartyInfoPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Set;

/** Uses the official roster, loaded player positions, and validated Loot Share activity. */
public final class SafariPartyTracker {
    private static final long ROSTER_INTERVAL_MILLIS = 10_000;
    private static final SafariPartyProgress PROGRESS = new SafariPartyProgress();
    private static volatile long nextRosterAt;
    private static volatile long nextPositionsAt;
    private static volatile long generation;

    private SafariPartyTracker() {}

    public static void initialize() {
        HypixelModAPI.getInstance().createHandler(ClientboundPartyInfoPacket.class, packet -> {
            Minecraft client = Minecraft.getInstance();
            long receivedGeneration = generation;
            Set<java.util.UUID> roster = Set.copyOf(packet.getMemberMap().keySet());
            client.execute(() -> {
                if (receivedGeneration != generation || !active() || client.player == null) return;
                PROGRESS.replaceMembers(packet.isInParty() ? roster : Set.of(), client.player.getUUID());
            });
        });
    }

    private static boolean active() {
        return SafariCritterStatsMod.CONFIG.isEnabled() && SafariLocationTracker.isInSafari()
            && SafariKingMod.CONFIG.showPartyProgress;
    }

    public static void tick(Minecraft client) {
        if (!active() || client.player == null || client.level == null || client.getConnection() == null) return;
        long now = System.currentTimeMillis();
        if (now >= nextRosterAt) {
            nextRosterAt = now + ROSTER_INTERVAL_MILLIS;
            HypixelModAPI.getInstance().sendPacket(new ServerboundPartyInfoPacket());
        }
        if (now < nextPositionsAt) return;
        nextPositionsAt = now + 250;
        for (java.util.UUID id : PROGRESS.memberIds()) {
            var info = client.getConnection().getPlayerInfo(id);
            if (info != null) PROGRESS.identify(id, info.getProfile().name());
            var player = client.level.getPlayerByUUID(id);
            if (player == null) continue;
            PROGRESS.identify(id, player.getGameProfile().name());
            BlockPos position = player.blockPosition();
            if (!client.level.hasChunkAt(position)) continue;
            SafariZone zone = SafariZoneTracker.isSpawnCoordinates(position.getX(), position.getY(), position.getZ())
                ? SafariZone.CENTER
                : client.level.getBiome(position).unwrapKey()
                    .map(key -> SafariZone.fromBiomeId(key.identifier().toString()))
                    .orElse(SafariZone.UNKNOWN);
            PROGRESS.observePosition(id, zone, now);
        }
    }

    public static void onCapture(String message, CaptureEvent event) {
        if (!active()) return;
        ChatParser.lootSharePlayer(message).ifPresent(player ->
            PROGRESS.observeCapture(player, event.critter(), System.currentTimeMillis()));
    }

    public static List<SafariPartyProgress.Row> rows() {
        return active() ? PROGRESS.rows(SafariCritterStatsMod.STATS, System.currentTimeMillis()) : List.of();
    }

    public static void reset() {
        generation++;
        nextRosterAt = 0;
        nextPositionsAt = 0;
        PROGRESS.reset();
    }
}
