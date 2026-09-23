package com.safariking;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.safariking.mixin.RenderPipelinesAccessor;
import com.safariking.mixin.RenderTypeAccessor;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

public final class WorldHighlightRenderer {
    private static final RenderType XRAY_LINES = createXrayLines();
    private static final RenderType XRAY_FILLED_BOX = createXrayFilledBox();

    private WorldHighlightRenderer() {
    }

    public static void register() {
        LevelRenderEvents.COLLECT_SUBMITS.register(WorldHighlightRenderer::render);
    }

    private static void render(LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        if (!SafariKingMod.CONFIG.enabled || client.level == null || client.player == null) return;

        List<HighlightTargetTracker.HighlightBox> entityBoxes = HighlightTargetTracker.boxes();
        if (!entityBoxes.isEmpty()) submitBoxes(context, entityBoxes, XRAY_LINES);
        List<HighlightTargetTracker.HighlightBox> frozenTreasureBoxes =
                HighlightTargetTracker.frozenTreasureBoxes();
        if (!frozenTreasureBoxes.isEmpty()) {
            submitFilledBoxes(context, frozenTreasureBoxes, XRAY_FILLED_BOX);
            submitBoxes(context, frozenTreasureBoxes, XRAY_LINES);
        }
        List<HighlightTargetTracker.HighlightBox> chestBoxes = CrystalHollowsChestTracker.boxes();
        if (!chestBoxes.isEmpty()) submitBoxes(context, chestBoxes, XRAY_LINES);
    }

    private static RenderType createXrayLines() {
        RenderPipeline pipeline = RenderPipelinesAccessor.safariking$register(
                RenderPipeline.builder(RenderPipelinesAccessor.safariking$getLinesSnippet())
                .withLocation(Identifier.fromNamespaceAndPath(SafariKingMod.MOD_ID, "xray_lines"))
                .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH)
                .withPrimitiveTopology(PrimitiveTopology.LINES)
                .withDepthStencilState(Optional.empty())
                .build());
        RenderSetup setup = RenderSetup.builder(pipeline)
                .setLayeringTransform(LayeringTransform.NO_LAYERING)
                .createRenderSetup();
        return RenderTypeAccessor.safariking$create("safariking_xray_lines", setup);
    }

    private static RenderType createXrayFilledBox() {
        RenderPipeline pipeline = RenderPipelinesAccessor.safariking$register(
                RenderPipeline.builder(RenderPipelinesAccessor.safariking$getDebugFilledSnippet())
                        .withLocation(Identifier.fromNamespaceAndPath(
                                SafariKingMod.MOD_ID, "xray_filled_box"))
                        .withDepthStencilState(Optional.empty())
                        .build());
        RenderSetup setup = RenderSetup.builder(pipeline).createRenderSetup();
        return RenderTypeAccessor.safariking$create("safariking_xray_filled_box", setup);
    }

    private static void submitFilledBoxes(LevelRenderContext context,
                                          List<HighlightTargetTracker.HighlightBox> boxes,
                                          RenderType renderType) {
        Vec3 camera = context.levelState().cameraRenderState.pos;
        PoseStack pose = context.poseStack();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        context.submitNodeCollector().submitCustomGeometry(pose, renderType,
                (entry, buffer) -> {
                    for (HighlightTargetTracker.HighlightBox box : boxes) {
                        submitFilledBox(entry, buffer, box.bounds(), 0x5055FF55);
                    }
                });
        pose.popPose();
    }

    private static void submitFilledBox(PoseStack.Pose pose, VertexConsumer buffer,
                                        AABB box, int color) {
        float x0 = (float) box.minX;
        float y0 = (float) box.minY;
        float z0 = (float) box.minZ;
        float x1 = (float) box.maxX;
        float y1 = (float) box.maxY;
        float z1 = (float) box.maxZ;

        addQuad(pose, buffer, color, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        addQuad(pose, buffer, color, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);
        addQuad(pose, buffer, color, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        addQuad(pose, buffer, color, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);
        addQuad(pose, buffer, color, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);
        addQuad(pose, buffer, color, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
    }

    private static void addQuad(PoseStack.Pose pose, VertexConsumer buffer, int color,
                                float ax, float ay, float az, float bx, float by, float bz,
                                float cx, float cy, float cz, float dx, float dy, float dz) {
        buffer.addVertex(pose, ax, ay, az).setColor(color);
        buffer.addVertex(pose, bx, by, bz).setColor(color);
        buffer.addVertex(pose, cx, cy, cz).setColor(color);
        buffer.addVertex(pose, dx, dy, dz).setColor(color);
    }

    private static void submitBoxes(LevelRenderContext context,
                                    List<HighlightTargetTracker.HighlightBox> entityBoxes,
                                    net.minecraft.client.renderer.rendertype.RenderType renderType) {
        Vec3 camera = context.levelState().cameraRenderState.pos;
        PoseStack pose = context.poseStack();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        context.submitNodeCollector().submitCustomGeometry(pose, renderType,
                (entry, buffer) -> {
                    for (HighlightTargetTracker.HighlightBox box : entityBoxes) {
                        submitBox(entry, buffer, box.bounds(), 0xFF000000 | box.color(), 3.0f);
                    }
                });
        pose.popPose();
    }

    private static void submitBox(PoseStack.Pose pose, VertexConsumer buffer, AABB box,
                                  int color, float width) {
        float x0 = (float) box.minX;
        float y0 = (float) box.minY;
        float z0 = (float) box.minZ;
        float x1 = (float) box.maxX;
        float y1 = (float) box.maxY;
        float z1 = (float) box.maxZ;
        addLine(pose, buffer, color, width, x0, y0, z0, x1, y0, z0);
        addLine(pose, buffer, color, width, x1, y0, z0, x1, y0, z1);
        addLine(pose, buffer, color, width, x1, y0, z1, x0, y0, z1);
        addLine(pose, buffer, color, width, x0, y0, z1, x0, y0, z0);
        addLine(pose, buffer, color, width, x0, y1, z0, x1, y1, z0);
        addLine(pose, buffer, color, width, x1, y1, z0, x1, y1, z1);
        addLine(pose, buffer, color, width, x1, y1, z1, x0, y1, z1);
        addLine(pose, buffer, color, width, x0, y1, z1, x0, y1, z0);
        addLine(pose, buffer, color, width, x0, y0, z0, x0, y1, z0);
        addLine(pose, buffer, color, width, x1, y0, z0, x1, y1, z0);
        addLine(pose, buffer, color, width, x1, y0, z1, x1, y1, z1);
        addLine(pose, buffer, color, width, x0, y0, z1, x0, y1, z1);
    }

    private static void addLine(PoseStack.Pose pose, VertexConsumer buffer, int color, float width,
                                float ax, float ay, float az, float bx, float by, float bz) {
        Vector3f normal = new Vector3f(bx - ax, by - ay, bz - az);
        if (normal.lengthSquared() < 1.0E-5f) normal.set(0.0f, 1.0f, 0.0f);
        else normal.normalize();
        buffer.addVertex(pose, ax, ay, az).setColor(color)
                .setNormal(pose, normal.x(), normal.y(), normal.z()).setLineWidth(width);
        buffer.addVertex(pose, bx, by, bz).setColor(color)
                .setNormal(pose, normal.x(), normal.y(), normal.z()).setLineWidth(width);
    }
}
