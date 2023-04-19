package fi.dy.masa.tweakeroo.renderer;

import net.minecraft.client.font.TextRenderer;
import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.debug.DebugRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.BlockPos;

public class OverlayRenderer
{
    // https://stackoverflow.com/questions/470690/how-to-automatically-generate-n-distinct-colors
    public static final int[] KELLY_COLORS = {
            0xFFB300,    // Vivid Yellow
            0x803E75,    // Strong Purple
            0xFF6800,    // Vivid Orange
            0xA6BDD7,    // Very Light Blue
            0xC10020,    // Vivid Red
            0xCEA262,    // Grayish Yellow
            0x817066,    // Medium Gray
            // The following don't work well for people with defective color vision
            0x007D34,    // Vivid Green
            0xF6768E,    // Strong Purplish Pink
            0x00538A,    // Strong Blue
            0xFF7A5C,    // Strong Yellowish Pink
            0x53377A,    // Strong Violet
            0xFF8E00,    // Vivid Orange Yellow
            0xB32851,    // Strong Purplish Red
            0xF4C800,    // Vivid Greenish Yellow
            0x7F180D,    // Strong Reddish Brown
            0x93AA00,    // Vivid Yellowish Green
            0x593315,    // Deep Yellowish Brown
            0xF13A13,    // Vivid Reddish Orange
            0x232C16     // Dark Olive Green
        };



        private static final double MAX_RENDER_DISTANCE = 256.0D;
        private static final float FONT_SIZE = 0.025F;
/* This doesnt work lol
        public static void drawItem(ItemStack stack, BlockPos pos,MatrixStack matrices, Vec3d offset)
        {
            MinecraftClient client = MinecraftClient.getInstance();
            Camera camera = client.gameRenderer.getCamera();
            if (camera.isReady() && client.getEntityRenderDispatcher().gameOptions != null && client.player != null) {
                double x = (double)pos.getX() + 0.5D;
                double y = (double)pos.getY() + 0.5D;
                double z = (double)pos.getZ() + 0.5D;
                if (client.player.squaredDistanceTo(x, y, z) > MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE)
                {
                    return;
                }
                double camX = camera.getPos().x;
                double camY = camera.getPos().y;
                double camZ = camera.getPos().z;
                RenderSystem.pushMatrix();
                RenderSystem.translatef((float)(x - camX), (float)(y - camY), (float)(z - camZ));
              //  RenderSystem.translatef((float)(-offset.getX()),(float)(-offset.getY()),(float)(-offset.getZ()));
                RenderSystem.normal3f(0.0F, 1.0F, 0.0F);
                RenderSystem.multMatrix(new Matrix4f(camera.getRotation()));
                RenderSystem.scalef(FONT_SIZE, -FONT_SIZE, FONT_SIZE);
                RenderSystem.enableTexture();
                RenderSystem.disableDepthTest();  // visibleThroughObjects
                RenderSystem.depthMask(true);
                RenderSystem.scalef(-1.0F, 1.0F, 1.0F);
                RenderSystem.enableAlphaTest();
    
                VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
                Matrix4f matrix4f = AffineTransformation.identity().getMatrix();
                client.getItemRenderer().renderItem(stack, ModelTransformation.Mode.FIXED, 15, OverlayTexture.DEFAULT_UV, matrices, immediate);
    
                immediate.draw();
    
                RenderSystem.enableDepthTest();
                RenderSystem.popMatrix();
            }
        }
     */
 	/**
	 * Borrowed from {@link DebugRenderer#drawString(String, double, double, double, int, float, boolean, float, boolean)}
	 */

	public static void drawString(String text, BlockPos pos, int color, float line)
	{
		MinecraftClient client = MinecraftClient.getInstance();
		Camera camera = client.gameRenderer.getCamera();
		if (camera.isReady() && client.getEntityRenderDispatcher().gameOptions != null && client.player != null) {
			double x = (double)pos.getX() + 0.5D;
			double y = (double)pos.getY() + 0.5D;
			double z = (double)pos.getZ() + 0.5D;
			if (client.player.squaredDistanceTo(x, y, z) > MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE)
			{
				return;
			}
			double camX = camera.getPos().x;
			double camY = camera.getPos().y;
			double camZ = camera.getPos().z;
            MatrixStack matrixStack = RenderSystem.getModelViewStack();
			matrixStack.push();
			matrixStack.translate((float)(x - camX), (float)(y - camY), (float)(z - camZ));
            matrixStack.multiplyPositionMatrix(new Matrix4f().rotation(camera.getRotation()));
            matrixStack.scale(FONT_SIZE, -FONT_SIZE, FONT_SIZE);
			RenderSystem.disableDepthTest();  // visibleThroughObjects
			RenderSystem.depthMask(true);
            matrixStack.scale(-1.0F, 1.0F, 1.0F);
			RenderSystem.applyModelViewMatrix();

			VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
			float renderX = -client.textRenderer.getWidth(text) * 0.5F;
			float renderY = client.textRenderer.getWrappedLinesHeight(text, Integer.MAX_VALUE) * (-0.5F + 1.25F * line);
			Matrix4f matrix4f = AffineTransformation.identity().getMatrix();
			client.textRenderer.draw(text, renderX, renderY, color, false, matrix4f, immediate, TextRenderer.TextLayerType.SEE_THROUGH, 0, 0xF000F0);
			immediate.draw();

			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			RenderSystem.enableDepthTest();
			matrixStack.pop();
		}
	}

}
