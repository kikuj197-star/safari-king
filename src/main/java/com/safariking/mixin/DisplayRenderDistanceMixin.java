package com.safariking.mixin;

import com.safariking.HighlightTargetTracker;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Display.class)
public abstract class DisplayRenderDistanceMixin {
    @Inject(method = "shouldRenderAtSqrDistance", at = @At("HEAD"), cancellable = true)
    private void safariKing$extendHighlightedDisplayRange(double distanceSquared,
                                                          CallbackInfoReturnable<Boolean> cir) {
        Entity entity = (Entity) (Object) this;
        if (HighlightTargetTracker.shouldForceRender(entity, distanceSquared)) {
            cir.setReturnValue(true);
        }
    }
}
