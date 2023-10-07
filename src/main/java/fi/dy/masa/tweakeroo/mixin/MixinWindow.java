package fi.dy.masa.tweakeroo.mixin;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fi.dy.masa.tweakeroo.config.Configs;
import fi.dy.masa.tweakeroo.config.FeatureToggle;
import fi.dy.masa.tweakeroo.tweaks.RenderTweaks;
import fi.dy.masa.tweakeroo.util.IMixinWindow;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.WindowEventHandler;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.Window;


@Mixin(Window.class)
public class MixinWindow implements IMixinWindow {

    @Unique
    double targetAspectRatio = 16.0 / 9.0;

    @Shadow @Final
	private WindowEventHandler eventHandler;

    @Shadow @Final
	private long handle;

    @Shadow
	private int framebufferWidth;

    @Shadow
	private int framebufferHeight;

    @Shadow
    private double scaleFactor;

    @Shadow
    private int width;


    @Shadow private int height;
    @Unique
    private int yOffset = 0;
    @Unique
    private int originalFramebufferHeight = 1;


    


    @ModifyVariable(method = "onFramebufferSizeChanged", at=@At("HEAD"), ordinal = 1)
    private int tweakfork$offsetWithAspectRatio(int height2) {
        this.yOffset = RenderTweaks.getHeightOffsetWithAspectRatio(this.targetAspectRatio, this.framebufferWidth, height2);
        this.originalFramebufferHeight = height2;
        return height2 - yOffset;
    }







    @Inject(method = "updateFramebufferSize", at = @At("RETURN"))
	private void updateFramebufferSizeInject(CallbackInfo ci) {
        this.yOffset = RenderTweaks.getHeightOffsetWithAspectRatio(this.targetAspectRatio, this.framebufferWidth, this.framebufferHeight);
        this.originalFramebufferHeight = this.framebufferHeight;
        this.framebufferHeight = this.framebufferHeight - this.yOffset;
	}

    @Override
    public int getYOffset() {
        return this.yOffset;
    }

    @Override 
    public int getOriginalScaledHeight() {
        int j = (int)((double)this.originalFramebufferHeight / scaleFactor);
        return (double)this.originalFramebufferHeight / scaleFactor > (double)j ? j + 1 : j;
    }

    @Override
    public int getOriginalFramebufferHeight() {
        return this.originalFramebufferHeight;
    }

    @Inject(method = "getScaleFactor", at = @At("HEAD"), cancellable = true)
    private void tweakeroo_customGuiScaleGetScale(CallbackInfoReturnable<Double> cir)
    {
        if (FeatureToggle.TWEAK_CUSTOM_INVENTORY_GUI_SCALE.getBooleanValue() &&
            MinecraftClient.getInstance().currentScreen instanceof HandledScreen<?>)
        {
            int scale = Configs.Generic.CUSTOM_INVENTORY_GUI_SCALE.getIntegerValue();

            if (scale > 0)
            {
                cir.setReturnValue((double) scale);
            }
        }
    }

    @Inject(method = "getScaledWidth", at = @At("HEAD"), cancellable = true)
    private void tweakeroo_customGuiScaleGetWidth(CallbackInfoReturnable<Integer> cir)
    {
        if (FeatureToggle.TWEAK_CUSTOM_INVENTORY_GUI_SCALE.getBooleanValue() &&
            MinecraftClient.getInstance().currentScreen instanceof HandledScreen<?>)
        {
            int scale = Configs.Generic.CUSTOM_INVENTORY_GUI_SCALE.getIntegerValue();

            if (scale > 0)
            {
                cir.setReturnValue((int) Math.ceil((double) width / scale));
            }
        }
    }

    @Inject(method = "getScaledHeight", at = @At("HEAD"), cancellable = true)
    private void tweakeroo_customGuiScaleGetHeight(CallbackInfoReturnable<Integer> cir)
    {
        if (FeatureToggle.TWEAK_CUSTOM_INVENTORY_GUI_SCALE.getBooleanValue() &&
            MinecraftClient.getInstance().currentScreen instanceof HandledScreen<?>)
        {
            int scale = Configs.Generic.CUSTOM_INVENTORY_GUI_SCALE.getIntegerValue();

            if (scale > 0)
            {
                cir.setReturnValue((int) Math.ceil((double) height / scale));
            }
        }
    }
}
