package fi.dy.masa.tweakeroo.mixin;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.tweakeroo.tweaks.RenderTweaks;
import fi.dy.masa.tweakeroo.util.IMixinWindow;
import net.minecraft.client.WindowEventHandler;
import net.minecraft.client.util.Window;


@Mixin(Window.class)
public class MixinWindow implements IMixinWindow {

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

    private int yOffset = 0;
    private int originalFramebufferHeight = 1;

    // TODO: use invoke
    @Overwrite
	private void onFramebufferSizeChanged(long window, int width, int height) {
		if (window == this.handle) {
			int i = this.framebufferWidth;
			int j = this.framebufferHeight;
			if (width != 0 && height != 0) {
				this.framebufferWidth = width;
                this.originalFramebufferHeight = height;
                this.yOffset = RenderTweaks.getHeightOffsetWithAspectRatio(this.targetAspectRatio, this.framebufferWidth, height);
				this.framebufferHeight = height - yOffset;
				if (this.framebufferWidth != i || this.framebufferHeight != j) {
					this.eventHandler.onResolutionChanged();
				}
			}
		}
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
}
