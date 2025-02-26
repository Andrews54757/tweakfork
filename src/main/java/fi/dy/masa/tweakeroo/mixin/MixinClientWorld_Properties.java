package fi.dy.masa.tweakeroo.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.HeightLimitView;
import fi.dy.masa.tweakeroo.config.Configs;
import fi.dy.masa.tweakeroo.config.FeatureToggle;

@Mixin(ClientWorld.Properties.class)
public class MixinClientWorld_Properties
{
    @Inject(method = "getSkyDarknessHeight", at = @At("HEAD"), cancellable = true)
    private void tweakeroo_overrideSkyDarknessHeight(HeightLimitView world, CallbackInfoReturnable<Double> cir)
    {
        // Disable the dark sky effect in normal situations
        // by moving the y threshold below the bottom of the world
        if (Configs.Disable.DISABLE_SKY_DARKNESS.getBooleanValue())
        {
            cir.setReturnValue(world.getBottomY() - 2.0);
        }
    }

    @Inject(method = "getTimeOfDay", at = @At("HEAD"), cancellable = true)
    private void timeOfDayOverride(CallbackInfoReturnable<Long> ci) {
        if (FeatureToggle.TWEAK_DAY_CYCLE_OVERRIDE.getBooleanValue()) {
            ci.setReturnValue((long) Configs.Generic.DAY_CYCLE_OVERRIDE_TIME.getIntegerValue());
        }
    }
}
