package com.safariking;

import com.safariking.stats.CritterRegion;
import com.safariking.stats.SafariCritterStatsMod;
import com.safariking.stats.SafariHudEditorScreen;
import com.safariking.stats.SafariLocationTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** A compact, responsive control panel shared by Mod Menu and /safariking. */
public final class KingConfigScreen extends Screen {
    private static final int GOLD = 0xFFFFCF70;
    private static final int MUTED = 0xFFA3AEC2;
    private static final int TEAL = 0xFF62E0C3;
    private final Screen parent;
    private final List<Button> featureButtons = new ArrayList<>();
    private int page;
    private int left;
    private int panelWidth;
    private int columnWidth;

    public KingConfigScreen(Screen parent) {
        super(Component.literal("Safari King"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        featureButtons.clear();
        panelWidth = Math.min(560, width - 24);
        left = (width - panelWidth) / 2;
        columnWidth = (panelWidth - 28) / 2;
        KingConfig config = SafariKingMod.CONFIG;

        addRenderableWidget(Button.builder(toggleText("Safari King", config.enabled), button -> {
            config.enabled = !config.enabled;
            config.save();
            SafariLocationTracker.onMasterEnabledChanged(SafariCritterStatsMod.CONFIG.isEnabled());
            HighlightTargetTracker.reset();
            CrystalHollowsChestTracker.reset();
            rebuildWidgets();
        }).bounds(left + panelWidth - 154, 15, 144, 20).build());

        String[] tabs = {"Stats & HUD", "Safari highlights", "Other areas"};
        int tabWidth = (panelWidth - 24) / 3;
        for (int i = 0; i < tabs.length; i++) {
            int target = i;
            Button tab = Button.builder(Component.literal(tabs[i]).withStyle(
                    i == page ? ChatFormatting.GOLD : ChatFormatting.GRAY), button -> {
                page = target;
                rebuildWidgets();
            }).bounds(left + 8 + i * (tabWidth + 4), 63, tabWidth, 20).build();
            addRenderableWidget(tab);
        }

        if (page == 0) {
            toggle(0, "Capture statistics", () -> config.statsEnabled,
                    value -> SafariCritterStatsMod.CONFIG.setEnabled(value),
                    "Count captures, Loot Share and shards. Starting again resets this run.");
            toggle(1, "Party responses", () -> config.respondToPartyCommands,
                    value -> SafariCritterStatsMod.CONFIG.setRespondToPartyCommands(value),
                    "Let teammates query your stats. Replies share a one-second message queue.");
            toggle(2, "Missing critters HUD", () -> config.showStatsHud,
                    value -> SafariCritterStatsMod.CONFIG.setShowHud(value),
                    "Show missing critters in your current Safari biome.");
            toggle(3, "Region done messages", () -> config.announceRegionCompletion,
                    value -> config.announceRegionCompletion = value,
                    "Announce a completed region once, with elapsed time.");
            Button editor = Button.builder(Component.literal("Move & resize HUD"), button ->
                    minecraft.gui.setScreen(new SafariHudEditorScreen(this)))
                    .bounds(left + 10, 153, panelWidth - 20, 20).build();
            editor.setTooltip(Tooltip.create(Component.literal("Drag to move. Scroll to resize from 50% to 250%.")));
            addRenderableWidget(editor);
        } else if (page == 1) {
            toggle(0, "Hideonwall", () -> config.hideonwall, value -> config.hideonwall = value, "Haunted");
            toggle(1, "Hideyho", () -> config.hideyho, value -> config.hideyho = value, "Haunted");
            toggle(2, "Duplico", () -> config.duplico, value -> config.duplico = value, "Haunted");
            toggle(3, "Rockmite", () -> config.rockmite, value -> config.rockmite = value, "Cavern");
            toggle(4, "Snoozle", () -> config.snoozleWalls, value -> config.snoozleWalls = value, "Cavern: entities received by the client");
            toggle(5, "Scrappy", () -> config.scrappy, value -> config.scrappy = value, "Cavern");
            toggle(6, "Hideonfloor", () -> config.hideonfloor, value -> config.hideonfloor = value, "Forest");
            toggle(7, "Floor Drops", () -> config.floorDrops, value -> config.floorDrops = value, "Only in your current Safari biome");
        } else {
            toggle(0, "Hideonleaf", () -> config.hideonleaf, value -> config.hideonleaf = value, "Galatea");
            toggle(1, "Hideonsun", () -> config.hideonsun, value -> config.hideonsun = value, "Torrhus Canyon");
            toggle(2, "Pangolin", () -> config.pangolin, value -> config.pangolin = value, "Torrhus Canyon");
            toggle(3, "Structure chests", () -> config.crystalHollowsStructureChests,
                    value -> config.crystalHollowsStructureChests = value, "Crystal Hollows: excludes powder-mining chests");
            toggle(4, "Mineshaft corpses", () -> config.glaciteMineshaftCorpses,
                    value -> config.glaciteMineshaftCorpses = value, "Unopened Glacite Mineshaft corpses");
            toggle(5, "Frozen Treasures", () -> config.frozenTreasures,
                    value -> config.frozenTreasures = value, "Glacial Cave: highlight the ice block containing treasure");
        }
        featureButtons.forEach(button -> button.active = config.enabled);
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(left + panelWidth - 100, height - 28, 90, 20).build());
    }

    private void toggle(int index, String label, BooleanSupplier getter,
                        Consumer<Boolean> setter, String description) {
        int x = left + 10 + (index % 2) * (columnWidth + 8);
        int y = 101 + (index / 2) * 25;
        Button button = Button.builder(toggleText(label, getter.getAsBoolean()), clicked -> {
            setter.accept(!getter.getAsBoolean());
            SafariKingMod.CONFIG.save();
            HighlightTargetTracker.reset();
            CrystalHollowsChestTracker.reset();
            clicked.setMessage(toggleText(label, getter.getAsBoolean()));
        }).bounds(x, y, columnWidth, 20).build();
        button.setTooltip(Tooltip.create(Component.literal(description)));
        featureButtons.add(addRenderableWidget(button));
    }

    private static Component toggleText(String label, boolean enabled) {
        return Component.literal(label + ": ").append(
                Component.literal(enabled ? "ON" : "OFF").withStyle(
                        enabled ? ChatFormatting.GREEN : ChatFormatting.GRAY));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xED0A101D);
        graphics.fill(left, 8, left + panelWidth, height - 5, 0xF0141E2D);
        graphics.fill(left, 8, left + panelWidth, 10, GOLD);
        graphics.text(font, title, left + 12, 19, GOLD, false);
        graphics.text(font, "Capture. Track. Explore.", left + 12, 37, MUTED, false);
        graphics.fill(left + 8, 89, left + panelWidth - 8, 91, 0xFF29374C);
        graphics.text(font, page == 0 ? "SAFARI STATISTICS" : page == 1 ? "SAFARI VISUAL HELPERS" : "EXPLORATION HELPERS",
                left + 12, 91, TEAL, false);

        if (page == 0 && height >= 310) {
            graphics.text(font, SafariCritterStatsMod.STATS.totalPartyMessage(), left + 12, 188, GOLD, false);
            graphics.text(font, "!miss [c/f/h/i]   !total   !prog", left + 12, 203, MUTED, false);
            int row = 0;
            for (CritterRegion region : CritterRegion.values()) {
                int y = 224 + row++ * 21;
                int total = region.critters().size();
                int caught = total - SafariCritterStatsMod.STATS.missingCritters(region).size();
                if (y + 17 > height - 53) break;
                graphics.text(font, region.displayName() + "  " + caught + "/" + total, left + 12, y, MUTED, false);
                int barLeft = left + 105;
                int barWidth = panelWidth - 120;
                graphics.fill(barLeft, y + 1, barLeft + barWidth, y + 7, 0xFF29374C);
                graphics.fill(barLeft, y + 1, barLeft + barWidth * caught / total, y + 7, TEAL);
            }
        }
        if (height >= 270) {
            graphics.text(font, "Includes Safari Critter Stats - install Safari King only.",
                    left + 12, height - 47, MUTED, false);
        }
        graphics.text(font, SafariLocationTracker.isInSafari() ? "Safari session active" : "Settings saved automatically",
                left + 12, height - 22, MUTED, false);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}
