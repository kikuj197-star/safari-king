package com.safariking;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class KingConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("safari-king.json");

    public boolean enabled = true;
    public boolean hideonwall = true;
    public boolean hideyho = true;
    public boolean snoozleWalls = true;
    public boolean scrappy = true;
    public boolean pangolin = true;
    public boolean hideonfloor = true;
    public boolean floorDrops = true;

    public static KingConfig load() {
        if (!Files.isRegularFile(PATH)) return new KingConfig();
        try (Reader reader = Files.newBufferedReader(PATH)) {
            KingConfig result = GSON.fromJson(reader, KingConfig.class);
            return result == null ? new KingConfig() : result;
        } catch (Exception exception) {
            SafariKingMod.LOGGER.warn("[SafariKing] Could not read config; using defaults.", exception);
            return new KingConfig();
        }
    }

    public synchronized void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Path temporary = PATH.resolveSibling(PATH.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(temporary)) {
                GSON.toJson(this, writer);
            }
            Files.move(temporary, PATH, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception exception) {
            SafariKingMod.LOGGER.error("[SafariKing] Could not save config.", exception);
        }
    }
}
