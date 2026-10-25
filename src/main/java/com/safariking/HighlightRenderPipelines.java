package com.safariking;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.resources.Identifier;

import java.util.Optional;

/** Pipeline construction kept separate from registration so it can be checked without a game window. */
final class HighlightRenderPipelines {
    private HighlightRenderPipelines() {}

    static RenderPipeline xrayLines(RenderPipeline.Snippet linesSnippet) {
        return RenderPipeline.builder(linesSnippet)
                .withLocation(Identifier.fromNamespaceAndPath("safariking", "xray_lines"))
                .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH)
                .withPrimitiveTopology(PrimitiveTopology.LINES)
                // Since 26.3 LINES_SNIPPET leaves color outputs unspecified, just like vanilla LINES.
                .withColorTargetState(ColorTargetState.DEFAULT)
                .withDepthStencilState(Optional.empty())
                .build();
    }
}
