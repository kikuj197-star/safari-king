package com.safariking;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Field;
import java.util.Optional;

/** Regression for the 26.3 Render Frame color-attachment mismatch. */
public final class HighlightRenderPipelinesTest {
    public static void main(String[] args) throws ReflectiveOperationException {
        Field field = RenderPipelines.class.getDeclaredField("LINES_SNIPPET");
        field.setAccessible(true);
        RenderPipeline.Snippet snippet = (RenderPipeline.Snippet) field.get(null);

        // Reproduce the released 2.0.0 pipeline using the actual Minecraft snippet.
        RenderPipeline broken = RenderPipeline.builder(snippet)
                .withLocation(Identifier.fromNamespaceAndPath("safariking", "test_old_lines"))
                .withDepthStencilState(Optional.empty())
                .build();
        if (!broken.getColorTargetStates().isEmpty()) {
            throw new AssertionError("Expected the 26.3 snippet to omit color targets");
        }

        RenderPipeline fixed = HighlightRenderPipelines.xrayLines(snippet);
        int mainPassTargets = RenderPipelines.LINES.getColorTargetStates().size();
        if (mainPassTargets != 1 || fixed.getColorTargetStates().size() != mainPassTargets) {
            throw new AssertionError("Highlight outputs do not match the vanilla main render pass");
        }
        if (!ColorTargetState.DEFAULT.equals(fixed.getColorTargetStates().getFirst())) {
            throw new AssertionError("Outline needs the normal color attachment write state");
        }
        if (fixed.getColorTargetStates().getFirst().blendFunction().isPresent()) {
            throw new AssertionError("Opaque outlines must remain in the solid custom-geometry pass");
        }
        if (fixed.getDepthStencilState() != null) {
            throw new AssertionError("Through-wall rendering must keep depth testing disabled");
        }
        if (fixed.getPrimitiveTopology() != PrimitiveTopology.LINES
                || fixed.getVertexFormatBinding(0) != DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH) {
            throw new AssertionError("Outline geometry format changed unexpectedly");
        }
        System.out.println("26.3 render pipeline regression passed: old=0 outputs, fixed=1 output; depth disabled.");
    }
}
