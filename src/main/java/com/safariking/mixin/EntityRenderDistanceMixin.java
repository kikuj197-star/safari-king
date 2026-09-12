package com.safariking.mixin;

import com.safariking.HighlightTargetTracker;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityRenderDistanceMixin {
    @Inject(method = "shouldRenderAtSqrDistance", at = @At("HEAD"), cancellable = true)
    private void safariKing$extendHighlightedEntityRange(double distanceSquared,
                                                         CallbackInfoReturnable<Boolean> cir) {
        if (HighlightTargetTracker.shouldForceRender((Entity) (Object) this, distanceSquared)) {
            cir.setReturnValue(true);
        }
    }
}
