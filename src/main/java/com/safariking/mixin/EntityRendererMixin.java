package com.safariking.mixin;

import com.safariking.HighlightTargetTracker;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityRenderer.class, priority = 900)
public class EntityRendererMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void safariKing$applyHighlight(Entity entity, EntityRenderState state,
                                    float partialTick, CallbackInfo ci) {
        int color = HighlightTargetTracker.colorFor(entity);
        if (color != 0) state.outlineColor = ARGB.opaque(color);
    }
}
