package com.safariking;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Highlights static structure chests while excluding powder-mining chests. */
public final class CrystalHollowsChestTracker {
    public static final int STRUCTURE_CHEST_COLOR = 0xFFD54A;

    private static final String POWDER_CHEST_MESSAGE = "You uncovered a treasure chest!";
    private static final int SCAN_CHUNK_RADIUS = 8;
    private static final int POWDER_CHEST_WAIT_TICKS = 60;
    private static final double POWDER_CHEST_MAX_DISTANCE_SQUARED = 12.0 * 12.0;

    private static Set<BlockPos> previousChests = Set.of();
    private static final Set<BlockPos> powderChests = new HashSet<>();
    private static volatile List<HighlightTargetTracker.HighlightBox> boxes = List.of();
    private static int waitingForPowderChests;
    private static int powderChestWaitTicks;
    private static int scanCooldown;

    private CrystalHollowsChestTracker() {
    }

    public static void onGameMessage(String message) {
        if (!KingLocationTracker.isCrystalHollows() || message == null) return;
        if (POWDER_CHEST_MESSAGE.equals(message.trim())) {
            waitingForPowderChests++;
            powderChestWaitTicks = POWDER_CHEST_WAIT_TICKS;
            scanCooldown = 0;
        }
    }

    public static void tick(Minecraft client) {
        if (!SafariKingMod.CONFIG.crystalHollowsStructureChests
                || !KingLocationTracker.isCrystalHollows()
                || client.level == null
                || client.player == null) {
            reset();
            return;
        }

        if (waitingForPowderChests == 0 && scanCooldown-- > 0) return;
        scanCooldown = 10;

        Set<BlockPos> currentChests = collectLoadedChests(client);
        powderChests.retainAll(currentChests);

        if (waitingForPowderChests > 0) {
            Vec3 playerPosition = client.player.position();
            List<BlockPos> spawnedNearby = currentChests.stream()
                    .filter(pos -> !previousChests.contains(pos))
                    .filter(pos -> Vec3.atCenterOf(pos).distanceToSqr(playerPosition)
                            <= POWDER_CHEST_MAX_DISTANCE_SQUARED)
                    .sorted(Comparator.comparingDouble(pos -> Vec3.atCenterOf(pos).distanceToSqr(playerPosition)))
                    .toList();

            int excluded = Math.min(waitingForPowderChests, spawnedNearby.size());
            for (int index = 0; index < excluded; index++) {
                powderChests.add(spawnedNearby.get(index));
            }
            waitingForPowderChests -= excluded;
        }

        if (waitingForPowderChests > 0 && --powderChestWaitTicks <= 0) {
            waitingForPowderChests = 0;
        }

        List<HighlightTargetTracker.HighlightBox> nextBoxes = new ArrayList<>();
        for (BlockPos pos : currentChests) {
            if (!powderChests.contains(pos)) {
                nextBoxes.add(new HighlightTargetTracker.HighlightBox(new AABB(pos).inflate(0.01),
                        STRUCTURE_CHEST_COLOR));
            }
        }

        previousChests = Set.copyOf(currentChests);
        boxes = List.copyOf(nextBoxes);
    }

    public static List<HighlightTargetTracker.HighlightBox> boxes() {
        return boxes;
    }

    public static void reset() {
        previousChests = Set.of();
        powderChests.clear();
        boxes = List.of();
        waitingForPowderChests = 0;
        powderChestWaitTicks = 0;
        scanCooldown = 0;
    }

    private static Set<BlockPos> collectLoadedChests(Minecraft client) {
        Set<BlockPos> result = new HashSet<>();
        int centerChunkX = client.player.blockPosition().getX() >> 4;
        int centerChunkZ = client.player.blockPosition().getZ() >> 4;
        int radius = Math.min(SCAN_CHUNK_RADIUS, client.options.getEffectiveRenderDistance());

        for (int chunkX = centerChunkX - radius; chunkX <= centerChunkX + radius; chunkX++) {
            for (int chunkZ = centerChunkZ - radius; chunkZ <= centerChunkZ + radius; chunkZ++) {
                LevelChunk chunk = client.level.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
                if (chunk == null) continue;
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity instanceof ChestBlockEntity && !blockEntity.isRemoved()) {
                        result.add(blockEntity.getBlockPos().immutable());
                    }
                }
            }
        }
        return result;
    }
}
