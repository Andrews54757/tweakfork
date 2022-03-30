package fi.dy.masa.tweakeroo.mixin;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallbackI;
import org.lwjgl.glfw.GLFWDropCallbackI;
import org.lwjgl.glfw.GLFWMouseButtonCallbackI;
import org.lwjgl.glfw.GLFWScrollCallbackI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import fi.dy.masa.tweakeroo.config.FeatureToggle;
import net.minecraft.client.util.InputUtil;

@Mixin(InputUtil.class)
public class MixinInputUtil {
    

    // Correct mouse position when the window's aspect ratio is changed.
    @Overwrite
    public static void setMouseCallbacks(long handle, GLFWCursorPosCallbackI cursorPosCallback, GLFWMouseButtonCallbackI mouseButtonCallback, GLFWScrollCallbackI scrollCallback, GLFWDropCallbackI gLFWDropCallbackI) {
		GLFW.glfwSetCursorPosCallback(handle, (windowx, x, y)->{

            if (!FeatureToggle.TWEAK_STANDARD_ASPECT_RATIO.getBooleanValue()) {
                cursorPosCallback.invoke(windowx, x, y);
                return;
            }
            int[] iw = new int[1];
            int[] jw = new int[1];
            GLFW.glfwGetWindowSize(windowx, iw, jw);

            double newAspectRatio = (16.0 / 9.0);
            double oldAspectRatio = (double) iw[0] / (double) jw[0];
            double correctionFactor = Math.max(newAspectRatio / oldAspectRatio, 1.0);
            int offset = Math.max(jw[0] - (int)(iw[0] / newAspectRatio), 0);
            cursorPosCallback.invoke(windowx, x, (int)((y - offset) * correctionFactor));
        });
		GLFW.glfwSetMouseButtonCallback(handle, mouseButtonCallback);
		GLFW.glfwSetScrollCallback(handle, scrollCallback);
		GLFW.glfwSetDropCallback(handle, gLFWDropCallbackI);
	}
}
