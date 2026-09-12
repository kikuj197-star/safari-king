package com.safariking;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SafariKingMod implements ClientModInitializer {
    public static final String MOD_ID = "safariking";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final KingConfig CONFIG = KingConfig.load();

    @Override
    public void onInitializeClient() {
        KingLocationTracker.initialize();
        WorldHighlightRenderer.register();

        ClientTickEvents.END_CLIENT_TICK.register(SafariKingMod::tick);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            KingLocationTracker.reset();
            EntityDiagnostics.reset();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            KingLocationTracker.reset();
            EntityDiagnostics.reset();
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("safariking")
                    .executes(context -> openConfig()));
            dispatcher.register(ClientCommands.literal("sking")
                    .executes(context -> openConfig()));
            dispatcher.register(ClientCommands.literal("skingdiag")
                    .executes(context -> EntityDiagnostics.usage())
                    .then(ClientCommands.literal("before")
                            .executes(context -> EntityDiagnostics.captureBefore()))
                    .then(ClientCommands.literal("after")
                            .executes(context -> EntityDiagnostics.captureAfter()))
                    .then(ClientCommands.literal("clear")
                            .executes(context -> EntityDiagnostics.clear())));
        });

        LOGGER.info("[SafariKing] Ready. Use /safariking to configure helpers or /skingdiag for entity snapshots.");
    }

    private static int openConfig() {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> client.setScreen(new KingConfigScreen(client.screen)));
        return 1;
    }

    private static void tick(Minecraft client) {
        KingLocationTracker.tick(client);
        if (!CONFIG.enabled) {
            HighlightTargetTracker.reset();
            return;
        }
        HighlightTargetTracker.tick(client);
    }
}
