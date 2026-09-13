package com.safariking;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class KingConfigScreen extends Screen {
    private static final int BUTTON_HEIGHT = 20;
    private final Screen parent;
    private final List<CycleButton<Boolean>> featureButtons = new ArrayList<>();

    public KingConfigScreen(Screen parent) {
        super(Component.literal("Safari King"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        KingConfig config = SafariKingMod.CONFIG;
        int totalWidth = Math.min(500, width - 30);
        int gap = 12;
        int columnWidth = (totalWidth - gap) / 2;
        int left = (width - totalWidth) / 2;
        int right = left + columnWidth + gap;
        int firstY = 78;

        StringWidget heading = new StringWidget(title, font);
        heading.setX((width - heading.getWidth()) / 2);
        heading.setY(20);
        addRenderableWidget(heading);

        addRenderableWidget(new MultiLineTextWidget(
                20,
                42,
                Component.literal("Highlights only run in their matching Safari biome or Torrhus area."),
                font).setMaxWidth(width - 40).setCentered(true));

        CycleButton<Boolean> master = addRenderableWidget(CycleButton.onOffBuilder(config.enabled).create(
                left,
                firstY,
                totalWidth,
                BUTTON_HEIGHT,
                Component.literal("Safari King enabled"),
                (button, enabled) -> {
                    config.enabled = enabled;
                    featureButtons.forEach(feature -> feature.active = enabled);
                    config.save();
                }));

        addFeature(left, firstY + 32, columnWidth, "Haunted: Hideonwall", config.hideonwall,
                value -> config.hideonwall = value);
        addFeature(left, firstY + 56, columnWidth, "Haunted: Hideyho", config.hideyho,
                value -> config.hideyho = value);
        addFeature(left, firstY + 80, columnWidth, "Haunted: Duplico", config.duplico,
                value -> config.duplico = value);
        addFeature(left, firstY + 104, columnWidth, "Forest: Hideonfloor", config.hideonfloor,
                value -> config.hideonfloor = value);
        addFeature(left, firstY + 128, columnWidth, "Current Safari biome: Floor Drops", config.floorDrops,
                value -> config.floorDrops = value);

        addFeature(right, firstY + 32, columnWidth, "Cavern: Snoozle", config.snoozleWalls,
                value -> config.snoozleWalls = value);
        addFeature(right, firstY + 56, columnWidth, "Cavern: Scrappy", config.scrappy,
                value -> config.scrappy = value);
        addFeature(right, firstY + 80, columnWidth, "Cavern: Rockmite", config.rockmite,
                value -> config.rockmite = value);
        addFeature(right, firstY + 104, columnWidth, "Torrhus: Pangolin", config.pangolin,
                value -> config.pangolin = value);

        featureButtons.forEach(button -> button.active = master.getValue());

        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(left, height - 32, totalWidth, BUTTON_HEIGHT)
                .build());
    }

    private void addFeature(int x, int y, int width, String name, boolean initial, Consumer<Boolean> setter) {
        CycleButton<Boolean> button = addRenderableWidget(CycleButton.onOffBuilder(initial).create(
                x,
                y,
                width,
                BUTTON_HEIGHT,
                Component.literal(name),
                (cycle, value) -> {
                    setter.accept(value);
                    SafariKingMod.CONFIG.save();
                    HighlightTargetTracker.reset();
                }));
        featureButtons.add(button);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}
