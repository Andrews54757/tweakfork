package fi.dy.masa.tweakeroo.renderer;

import com.mojang.blaze3d.systems.RenderSystem;

import fi.dy.masa.malilib.util.Color4f;
import fi.dy.masa.malilib.util.EntityUtils;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.tweakeroo.config.Configs;
import fi.dy.masa.tweakeroo.util.MiscUtils;
import fi.dy.masa.tweakeroo.util.RayTraceUtils;
import fi.dy.masa.tweakeroo.util.SnapAimMode;
import net.minecraft.block.Block;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.minecraft.world.World;
import fi.dy.masa.malilib.util.EntityUtils;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.tweakeroo.config.Configs;
import fi.dy.masa.tweakeroo.mixin.IMixinAbstractHorseEntity;
import fi.dy.masa.tweakeroo.util.MiscUtils;
import fi.dy.masa.tweakeroo.util.RayTraceUtils;
import fi.dy.masa.tweakeroo.util.SnapAimMode;

public class RenderUtils
{
    private static long lastRotationChangeTime;

    public static void renderHotbarSwapOverlay(MinecraftClient mc, MatrixStack matrixStack)
    {
        PlayerEntity player = mc.player;

        if (player != null && mc.currentScreen == null)
        {
            final int scaledWidth = GuiUtils.getScaledWindowWidth();
            final int scaledHeight = GuiUtils.getScaledWindowHeight();
            final int offX = Configs.Generic.HOTBAR_SWAP_OVERLAY_OFFSET_X.getIntegerValue();
            final int offY = Configs.Generic.HOTBAR_SWAP_OVERLAY_OFFSET_Y.getIntegerValue();
            int startX = offX;
            int startY = offY;

            fi.dy.masa.malilib.config.HudAlignment align = (fi.dy.masa.malilib.config.HudAlignment) Configs.Generic.HOTBAR_SWAP_OVERLAY_ALIGNMENT.getOptionListValue();

            switch (align)
            {
                case TOP_RIGHT:
                    startX = (int) scaledWidth - offX - 9 * 18;
                    break;
                case BOTTOM_LEFT:
                    startY = (int) scaledHeight - offY - 3 * 18;
                    break;
                case BOTTOM_RIGHT:
                    startX = (int) scaledWidth - offX - 9 * 18;
                    startY = (int) scaledHeight - offY - 3 * 18;
                    break;
                case CENTER:
                    startX = (int) scaledWidth / 2 - offX - 9 * 18 / 2;
                    startY = (int) scaledHeight / 2 - offY - 3 * 18 / 2;
                    break;
                default:
            }

            int x = startX;
            int y = startY;
            TextRenderer textRenderer = mc.textRenderer;

            Matrix4f modelViewMatrix = RenderSystem.getModelViewMatrix().copy();
            fi.dy.masa.malilib.render.RenderUtils.color(1f, 1f, 1f, 1f);
            fi.dy.masa.malilib.render.RenderUtils.bindTexture(HandledScreen.BACKGROUND_TEXTURE);
            fi.dy.masa.malilib.render.RenderUtils.drawTexturedRect(x - 1, y - 1, 7, 83, 9 * 18, 3 * 18);

            textRenderer.drawWithShadow(matrixStack, "1", x - 10, y +  4, 0xFFFFFF);
            textRenderer.drawWithShadow(matrixStack, "2", x - 10, y + 22, 0xFFFFFF);
            textRenderer.drawWithShadow(matrixStack, "3", x - 10, y + 40, 0xFFFFFF);

            for (int row = 1; row <= 3; row++)
            {
                for (int column = 0; column < 9; column++)
                {
                    ItemStack stack = player.getInventory().getStack(row * 9 + column);

                    if (stack.isEmpty() == false)
                    {
                        fi.dy.masa.malilib.render.InventoryOverlay.renderStackAt(stack, x, y, 1, mc);
                    }

                    x += 18;
                }

                y += 18;
                x = startX;
            }

            RenderSystem.getModelViewMatrix().load(modelViewMatrix);
        }
    }

    public static void renderInventoryOverlay(MinecraftClient mc, MatrixStack matrixStack)
    {
        World world = fi.dy.masa.malilib.util.WorldUtils.getBestWorld(mc);
        Entity cameraEntity = EntityUtils.getCameraEntity();

        if (cameraEntity == mc.player && world instanceof ServerWorld)
        {
            // We need to get the player from the server world (if available, ie. in single player),
            // so that the player itself won't be included in the ray trace
            Entity serverPlayer = world.getPlayerByUuid(mc.player.getUuid());

            if (serverPlayer != null)
            {
                cameraEntity = serverPlayer;
            }
        }

        HitResult trace = RayTraceUtils.getRayTraceFromEntity(world, cameraEntity, false);

        if (trace == null)
        {
            return;
        }

        Inventory inv = null;
        ShulkerBoxBlock block = null;
        LivingEntity entityLivingBase = null;

        if (trace.getType() == HitResult.Type.BLOCK)
        {
            BlockPos pos = ((BlockHitResult) trace).getBlockPos();
            Block blockTmp = world.getBlockState(pos).getBlock();

            if (blockTmp instanceof ShulkerBoxBlock)
            {
                block = (ShulkerBoxBlock) blockTmp;
            }

            inv = fi.dy.masa.malilib.util.InventoryUtils.getInventory(world, pos);
        }
        else if (trace.getType() == HitResult.Type.ENTITY)
        {
            Entity entity = ((EntityHitResult) trace).getEntity();

            if (entity instanceof LivingEntity)
            {
                entityLivingBase = (LivingEntity) entity;
            }

            if (entity instanceof Inventory)
            {
                inv = (Inventory) entity;
            }
            else if (entity instanceof VillagerEntity)
            {
                inv = ((VillagerEntity) entity).getInventory();
            }
            else if (entity instanceof AbstractHorseEntity)
            {
                inv = ((IMixinAbstractHorseEntity) entity).tweakeroo_getHorseInventory();
            }
        }

        final int xCenter = GuiUtils.getScaledWindowWidth() / 2;
        final int yCenter = GuiUtils.getScaledWindowHeight() / 2;
        int x = xCenter - 52 / 2;
        int y = yCenter - 92;

        if (inv != null && inv.size() > 0)
        {
            final boolean isHorse = (entityLivingBase instanceof AbstractHorseEntity);
            final int totalSlots = isHorse ? inv.size() - 2 : inv.size();
            final int firstSlot = isHorse ? 2 : 0;

            final fi.dy.masa.malilib.render.InventoryOverlay.InventoryRenderType type = (entityLivingBase instanceof VillagerEntity) ? fi.dy.masa.malilib.render.InventoryOverlay.InventoryRenderType.VILLAGER : fi.dy.masa.malilib.render.InventoryOverlay.getInventoryType(inv);
            final fi.dy.masa.malilib.render.InventoryOverlay.InventoryProperties props = fi.dy.masa.malilib.render.InventoryOverlay.getInventoryPropsTemp(type, totalSlots);
            final int rows = (int) Math.ceil((double) totalSlots / props.slotsPerRow);
            int xInv = xCenter - (props.width / 2);
            int yInv = yCenter - props.height - 6;

            if (rows > 6)
            {
                yInv -= (rows - 6) * 18;
                y -= (rows - 6) * 18;
            }

            if (entityLivingBase != null)
            {
                x = xCenter - 55;
                xInv = xCenter + 2;
                yInv = Math.min(yInv, yCenter - 92);
            }

            fi.dy.masa.malilib.render.RenderUtils.setShulkerboxBackgroundTintColor(block, Configs.Generic.SHULKER_DISPLAY_BACKGROUND_COLOR.getBooleanValue());

            if (isHorse)
            {
                fi.dy.masa.malilib.render.InventoryOverlay.renderInventoryBackground(type, xInv, yInv, 1, 2, mc);
                fi.dy.masa.malilib.render.InventoryOverlay.renderInventoryStacks(type, inv, xInv + props.slotOffsetX, yInv + props.slotOffsetY, 1, 0, 2, mc);
                xInv += 32 + 4;
            }

            if (totalSlots > 0)
            {
                fi.dy.masa.malilib.render.InventoryOverlay.renderInventoryBackground(type, xInv, yInv, props.slotsPerRow, totalSlots, mc);
                fi.dy.masa.malilib.render.InventoryOverlay.renderInventoryStacks(type, inv, xInv + props.slotOffsetX, yInv + props.slotOffsetY, props.slotsPerRow, firstSlot, totalSlots, mc);
            }
        }

        if (entityLivingBase != null)
        {
            fi.dy.masa.malilib.render.InventoryOverlay.renderEquipmentOverlayBackground(x, y, entityLivingBase, matrixStack);
            fi.dy.masa.malilib.render.InventoryOverlay.renderEquipmentStacks(entityLivingBase, x, y, mc);
        }
    }

    public static void renderPlayerInventoryOverlay(MinecraftClient mc)
    {
        int x = GuiUtils.getScaledWindowWidth() / 2 - 176 / 2;
        int y = GuiUtils.getScaledWindowHeight() / 2 + 10;
        int slotOffsetX = 8;
        int slotOffsetY = 8;
        fi.dy.masa.malilib.render.InventoryOverlay.InventoryRenderType type = fi.dy.masa.malilib.render.InventoryOverlay.InventoryRenderType.GENERIC;

        fi.dy.masa.malilib.render.RenderUtils.color(1f, 1f, 1f, 1f);

        fi.dy.masa.malilib.render.InventoryOverlay.renderInventoryBackground(type, x, y, 9, 27, mc);
        fi.dy.masa.malilib.render.InventoryOverlay.renderInventoryStacks(type, mc.player.getInventory(), x + slotOffsetX, y + slotOffsetY, 9, 9, 27, mc);
    }

    public static void renderHotbarScrollOverlay(MinecraftClient mc)
    {
        Inventory inv = mc.player.getInventory();
        final int xCenter = GuiUtils.getScaledWindowWidth() / 2;
        final int yCenter = GuiUtils.getScaledWindowHeight() / 2;
        final int x = xCenter - 176 / 2;
        final int y = yCenter + 6;
        fi.dy.masa.malilib.render.InventoryOverlay.InventoryRenderType type = fi.dy.masa.malilib.render.InventoryOverlay.InventoryRenderType.GENERIC;

        fi.dy.masa.malilib.render.RenderUtils.color(1f, 1f, 1f, 1f);

        fi.dy.masa.malilib.render.InventoryOverlay.renderInventoryBackground(type, x, y     , 9, 27, mc);
        fi.dy.masa.malilib.render.InventoryOverlay.renderInventoryBackground(type, x, y + 70, 9,  9, mc);

        // Main inventory
        fi.dy.masa.malilib.render.InventoryOverlay.renderInventoryStacks(type, inv, x + 8, y +  8, 9, 9, 27, mc);
        // Hotbar
        fi.dy.masa.malilib.render.InventoryOverlay.renderInventoryStacks(type, inv, x + 8, y + 78, 9, 0,  9, mc);

        int currentRow = Configs.Internal.HOTBAR_SCROLL_CURRENT_ROW.getIntegerValue();
        fi.dy.masa.malilib.render.RenderUtils.drawOutline(x + 5, y + currentRow * 18 + 5, 9 * 18 + 4, 22, 2, 0xFFFF2020);
    }

    public static float getLavaFogDistance(Entity entity, float originalFog)
    {
        if (entity instanceof LivingEntity living)
        {
            final int resp = EnchantmentHelper.getRespiration(living);
            final int aqua = EnchantmentHelper.getEquipmentLevel(Enchantments.AQUA_AFFINITY, living);
            float fog = originalFog;

            if (aqua > 0)
            {
                fog *= 1.6f;
            }

            if (resp > 0)
            {
                fog *= (float) resp * 1.6f;
            }

            return Math.max(fog, originalFog);
        }

        return originalFog;
    }

    public static void renderDirectionsCursor(float zLevel, float partialTicks)
    {
        MinecraftClient mc = MinecraftClient.getInstance();

        int width = GuiUtils.getScaledWindowWidth();
        int height = GuiUtils.getScaledWindowHeight();
        Camera camera = mc.gameRenderer.getCamera();
        MatrixStack matrixStack = RenderSystem.getModelViewStack();
        matrixStack.push();
        matrixStack.translate(width / 2.0, height / 2.0, zLevel);
        matrixStack.multiply(Vec3f.NEGATIVE_X.getDegreesQuaternion(camera.getPitch()));
        matrixStack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(camera.getYaw()));
        matrixStack.scale(-1.0F, -1.0F, -1.0F);
        RenderSystem.applyModelViewMatrix();
        RenderSystem.renderCrosshair(10);
        matrixStack.pop();
        RenderSystem.applyModelViewMatrix();
    }

    public static void notifyRotationChanged()
    {
        lastRotationChangeTime = System.currentTimeMillis();
    }

    public static void renderSnapAimAngleIndicator(MatrixStack matrixStack)
    {
        long current = System.currentTimeMillis();

        if (current - lastRotationChangeTime < 750)
        {
            MinecraftClient mc = MinecraftClient.getInstance();
            final int xCenter = GuiUtils.getScaledWindowWidth() / 2;
            final int yCenter = GuiUtils.getScaledWindowHeight() / 2;
            SnapAimMode mode = (SnapAimMode) Configs.Generic.SNAP_AIM_MODE.getOptionListValue();

            if (mode != SnapAimMode.PITCH)
            {
                renderSnapAimAngleIndicatorYaw(xCenter, yCenter, 80, 12, mc, matrixStack);
            }

            if (mode != SnapAimMode.YAW)
            {
                renderSnapAimAngleIndicatorPitch(xCenter, yCenter, 12, 50, mc, matrixStack);
            }
        }
    }

    private static void renderSnapAimAngleIndicatorYaw(int xCenter, int yCenter, int width, int height, MinecraftClient mc, MatrixStack matrixStack)
    {
        double step = Configs.Generic.SNAP_AIM_YAW_STEP.getDoubleValue();
        double realYaw = MathHelper.floorMod(MiscUtils.getLastRealYaw(), 360.0D);
        double snappedYaw = MiscUtils.calculateSnappedAngle(realYaw, step);
        double startYaw = snappedYaw - (step / 2.0);
        final int x = xCenter - width / 2;
        final int y = yCenter + 10;
        int lineX = x + (int) ((MathHelper.wrapDegrees(realYaw - startYaw)) / step * width);
        TextRenderer textRenderer = mc.textRenderer;

        fi.dy.masa.malilib.render.RenderUtils.color(1f, 1f, 1f, 1f);

        int bgColor = Configs.Generic.SNAP_AIM_INDICATOR_COLOR.getIntegerValue();

        // Draw the main box
        fi.dy.masa.malilib.render.RenderUtils.drawOutlinedBox(x, y, width, height, bgColor, 0xFFFFFFFF);

        String str = MathHelper.wrapDegrees(snappedYaw) + "°";
        textRenderer.draw(matrixStack, str, xCenter - textRenderer.getWidth(str) / 2, y + height + 2, 0xFFFFFFFF);

        str = "<  " + MathHelper.wrapDegrees(snappedYaw - step) + "°";
        textRenderer.draw(matrixStack, str, x - textRenderer.getWidth(str), y + height + 2, 0xFFFFFFFF);

        str = MathHelper.wrapDegrees(snappedYaw + step) + "°  >";
        textRenderer.draw(matrixStack, str, x + width, y + height + 2, 0xFFFFFFFF);

        if (Configs.Generic.SNAP_AIM_ONLY_CLOSE_TO_ANGLE.getBooleanValue())
        {
            double threshold = Configs.Generic.SNAP_AIM_THRESHOLD_YAW.getDoubleValue();
            int snapThreshOffset = (int) (width * threshold / step);

            // Draw the middle region
            fi.dy.masa.malilib.render.RenderUtils.drawRect(xCenter - snapThreshOffset, y, snapThreshOffset * 2, height, 0x6050FF50);

            if (threshold < (step / 2.0))
            {
                fi.dy.masa.malilib.render.RenderUtils.drawRect(xCenter - snapThreshOffset, y, 2, height, 0xFF20FF20);
                fi.dy.masa.malilib.render.RenderUtils.drawRect(xCenter + snapThreshOffset, y, 2, height, 0xFF20FF20);
            }
        }

        // Draw the current angle indicator
        fi.dy.masa.malilib.render.RenderUtils.drawRect(lineX, y, 2, height, 0xFFFFFFFF);
    }

    private static void renderSnapAimAngleIndicatorPitch(int xCenter, int yCenter, int width, int height,
            MinecraftClient mc, MatrixStack matrixStack)
    {
        double step = Configs.Generic.SNAP_AIM_PITCH_STEP.getDoubleValue();
        int limit = Configs.Generic.SNAP_AIM_PITCH_OVERSHOOT.getBooleanValue() ? 180 : 90;
        //double realPitch = MathHelper.clamp(MathHelper.wrapDegrees(MiscUtils.getLastRealPitch()), -limit, limit);
        double realPitch = MathHelper.wrapDegrees(MiscUtils.getLastRealPitch());
        double snappedPitch;

        if (realPitch < 0)
        {
            snappedPitch = -MiscUtils.calculateSnappedAngle(-realPitch, step);
        }
        else
        {
            snappedPitch = MiscUtils.calculateSnappedAngle(realPitch, step);
        }

        snappedPitch = MathHelper.clamp(MathHelper.wrapDegrees(snappedPitch), -limit, limit);

        int x = xCenter - width / 2;
        int y = yCenter - height - 10;

        renderPitchIndicator(x, y, width, height, realPitch, snappedPitch, step, true, mc, matrixStack);
    }

    public static void renderPitchLockIndicator(MinecraftClient mc, MatrixStack matrixStack)
    {
        final int xCenter = GuiUtils.getScaledWindowWidth() / 2;
        final int yCenter = GuiUtils.getScaledWindowHeight() / 2;
        int width = 12;
        int height = 50;
        int x = xCenter - width / 2;
        int y = yCenter - height - 10;
        double currentPitch = mc.player.getPitch();
        double centerPitch = 0;
        double indicatorRange = 180;

        renderPitchIndicator(x, y, width, height, currentPitch, centerPitch, indicatorRange, false, mc, matrixStack);
    }

    private static void renderPitchIndicator(int x, int y, int width, int height,
            double currentPitch, double centerPitch, double indicatorRange, boolean isSnapRange,
            MinecraftClient mc, MatrixStack matrixStack)
    {
        double startPitch = centerPitch - (indicatorRange / 2.0);
        double printedRange = isSnapRange ? indicatorRange : indicatorRange / 2;
        int lineY = y + (int) ((MathHelper.wrapDegrees(currentPitch) - startPitch) / indicatorRange * height);
        double angleUp = centerPitch - printedRange;
        double angleDown = centerPitch + printedRange;

        fi.dy.masa.malilib.render.RenderUtils.color(1f, 1f, 1f, 1f);
        TextRenderer textRenderer = mc.textRenderer;

        if (isSnapRange)
        {
            String strUp   = String.format("%6.1f° ^", MathHelper.wrapDegrees(angleUp));
            String strDown = String.format("%6.1f° v", MathHelper.wrapDegrees(angleDown));
            textRenderer.draw(matrixStack, strUp, x + width + 4, y - 4, 0xFFFFFFFF);
            textRenderer.draw(matrixStack, strDown, x + width + 4, y + height - 4, 0xFFFFFFFF);

            String str = String.format("%6.1f°", MathHelper.wrapDegrees(isSnapRange ? centerPitch : currentPitch));
            textRenderer.draw(matrixStack, str, x + width + 4, y + height / 2 - 4, 0xFFFFFFFF);
        }
        else
        {
            String str = String.format("%4.1f°", MathHelper.wrapDegrees(isSnapRange ? centerPitch : currentPitch));
            textRenderer.draw(matrixStack, str, x + width + 4, lineY - 4, 0xFFFFFFFF);
        }

        int bgColor = Configs.Generic.SNAP_AIM_INDICATOR_COLOR.getIntegerValue();
        // Draw the main box
        fi.dy.masa.malilib.render.RenderUtils.drawOutlinedBox(x, y, width, height, bgColor, 0xFFFFFFFF);

        int yCenter = y + height / 2 - 1;

        if (isSnapRange && Configs.Generic.SNAP_AIM_ONLY_CLOSE_TO_ANGLE.getBooleanValue())
        {
            double step = Configs.Generic.SNAP_AIM_YAW_STEP.getDoubleValue();
            double threshold = Configs.Generic.SNAP_AIM_THRESHOLD_PITCH.getDoubleValue();
            int snapThreshOffset = (int) ((double) height * threshold / indicatorRange);

            fi.dy.masa.malilib.render.RenderUtils.drawRect(x, yCenter - snapThreshOffset, width, snapThreshOffset * 2, 0x6050FF50);

            if (threshold < (step / 2.0))
            {
                fi.dy.masa.malilib.render.RenderUtils.drawRect(x, yCenter - snapThreshOffset, width, 2, 0xFF20FF20);
                fi.dy.masa.malilib.render.RenderUtils.drawRect(x, yCenter + snapThreshOffset, width, 2, 0xFF20FF20);
            }
        }
        else if (isSnapRange == false)
        {
            fi.dy.masa.malilib.render.RenderUtils.drawRect(x + 1, yCenter - 1, width - 2, 2, 0xFFC0C0C0);
        }

        // Draw the current angle indicator
        fi.dy.masa.malilib.render.RenderUtils.drawRect(x, lineY - 1, width, 2, 0xFFFFFFFF);
    }


    public static void renderBlockOutline(BlockPos pos, float expand, float lineWidth, Color4f color, MinecraftClient mc)
    {
        RenderSystem.lineWidth(lineWidth);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        startDrawingLines(buffer);
       
        drawBlockBoundingBoxOutlinesBatchedLines(pos, color, expand, buffer, mc);

        tessellator.draw();
    }
    
    static void startDrawingLines(BufferBuilder buffer)
    {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
      
        RenderSystem.applyModelViewMatrix();
        buffer.begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
    }
    
    public static void drawBlockBoundingBoxOutlinesBatchedLines(BlockPos pos, Color4f color,
            double expand, BufferBuilder buffer, MinecraftClient mc)
    {
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        final double dx = cameraPos.x;
        final double dy = cameraPos.y;
        final double dz = cameraPos.z;

        double minX = pos.getX() - dx - expand;
        double minY = pos.getY() - dy - expand;
        double minZ = pos.getZ() - dz - expand;
        double maxX = pos.getX() - dx + expand + 1;
        double maxY = pos.getY() - dy + expand + 1;
        double maxZ = pos.getZ() - dz + expand + 1;

        fi.dy.masa.malilib.render.RenderUtils.drawBoxAllEdgesBatchedLines(minX, minY, minZ, maxX, maxY, maxZ, color, buffer);
    }

    public static void drawConnectingLineBatchedLines(BlockPos pos1, BlockPos pos2, boolean center,
            Color4f color, BufferBuilder buffer, MinecraftClient mc)
    {
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        final double dx = cameraPos.x;
        final double dy = cameraPos.y;
        final double dz = cameraPos.z;

        double x1 = pos1.getX() - dx;
        double y1 = pos1.getY() - dy;
        double z1 = pos1.getZ() - dz;
        double x2 = pos2.getX() - dx;
        double y2 = pos2.getY() - dy;
        double z2 = pos2.getZ() - dz;

        if (center)
        {
            x1 += 0.5;
            y1 += 0.5;
            z1 += 0.5;
            x2 += 0.5;
            y2 += 0.5;
            z2 += 0.5;
        }

        buffer.vertex(x1, y1, z1).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(x2, y2, z2).color(color.r, color.g, color.b, color.a).next();
    }

    public static void renderBlockOutlineOverlapping(BlockPos pos, float expand, float lineWidth,
            Color4f color1, Color4f color2, Color4f color3, MatrixStack matrices, MinecraftClient mc)
    {
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        final double dx = cameraPos.x;
        final double dy = cameraPos.y;
        final double dz = cameraPos.z;

        final double minX = pos.getX() - dx - expand;
        final double minY = pos.getY() - dy - expand;
        final double minZ = pos.getZ() - dz - expand;
        final double maxX = pos.getX() - dx + expand + 1;
        final double maxY = pos.getY() - dy + expand + 1;
        final double maxZ = pos.getZ() - dz + expand + 1;

        RenderSystem.lineWidth(lineWidth);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        startDrawingLines(buffer);

        // Min corner
        buffer.vertex(minX, minY, minZ).color(color1.r, color1.g, color1.b, color1.a).next();
        buffer.vertex(maxX, minY, minZ).color(color1.r, color1.g, color1.b, color1.a).next();

        buffer.vertex(minX, minY, minZ).color(color1.r, color1.g, color1.b, color1.a).next();
        buffer.vertex(minX, maxY, minZ).color(color1.r, color1.g, color1.b, color1.a).next();

        buffer.vertex(minX, minY, minZ).color(color1.r, color1.g, color1.b, color1.a).next();
        buffer.vertex(minX, minY, maxZ).color(color1.r, color1.g, color1.b, color1.a).next();

        // Max corner
        buffer.vertex(minX, maxY, maxZ).color(color2.r, color2.g, color2.b, color2.a).next();
        buffer.vertex(maxX, maxY, maxZ).color(color2.r, color2.g, color2.b, color2.a).next();

        buffer.vertex(maxX, minY, maxZ).color(color2.r, color2.g, color2.b, color2.a).next();
        buffer.vertex(maxX, maxY, maxZ).color(color2.r, color2.g, color2.b, color2.a).next();

        buffer.vertex(maxX, maxY, minZ).color(color2.r, color2.g, color2.b, color2.a).next();
        buffer.vertex(maxX, maxY, maxZ).color(color2.r, color2.g, color2.b, color2.a).next();

        // The rest of the edges
        buffer.vertex(minX, maxY, minZ).color(color3.r, color3.g, color3.b, color3.a).next();
        buffer.vertex(maxX, maxY, minZ).color(color3.r, color3.g, color3.b, color3.a).next();

        buffer.vertex(minX, minY, maxZ).color(color3.r, color3.g, color3.b, color3.a).next();
        buffer.vertex(maxX, minY, maxZ).color(color3.r, color3.g, color3.b, color3.a).next();

        buffer.vertex(maxX, minY, minZ).color(color3.r, color3.g, color3.b, color3.a).next();
        buffer.vertex(maxX, maxY, minZ).color(color3.r, color3.g, color3.b, color3.a).next();

        buffer.vertex(minX, minY, maxZ).color(color3.r, color3.g, color3.b, color3.a).next();
        buffer.vertex(minX, maxY, maxZ).color(color3.r, color3.g, color3.b, color3.a).next();

        buffer.vertex(maxX, minY, minZ).color(color3.r, color3.g, color3.b, color3.a).next();
        buffer.vertex(maxX, minY, maxZ).color(color3.r, color3.g, color3.b, color3.a).next();

        buffer.vertex(minX, maxY, minZ).color(color3.r, color3.g, color3.b, color3.a).next();
        buffer.vertex(minX, maxY, maxZ).color(color3.r, color3.g, color3.b, color3.a).next();

        tessellator.draw();
    }

    public static void renderAreaOutline(BlockPos pos1, BlockPos pos2, float lineWidth,
            Color4f colorX, Color4f colorY, Color4f colorZ, MinecraftClient mc)
    {
        RenderSystem.lineWidth(lineWidth);

        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        final double dx = cameraPos.x;
        final double dy = cameraPos.y;
        final double dz = cameraPos.z;

        double minX = Math.min(pos1.getX(), pos2.getX()) - dx;
        double minY = Math.min(pos1.getY(), pos2.getY()) - dy;
        double minZ = Math.min(pos1.getZ(), pos2.getZ()) - dz;
        double maxX = Math.max(pos1.getX(), pos2.getX()) - dx + 1;
        double maxY = Math.max(pos1.getY(), pos2.getY()) - dy + 1;
        double maxZ = Math.max(pos1.getZ(), pos2.getZ()) - dz + 1;

        drawBoundingBoxEdges(minX, minY, minZ, maxX, maxY, maxZ, colorX, colorY, colorZ);
    }

    private static void drawBoundingBoxEdges(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, Color4f colorX, Color4f colorY, Color4f colorZ)
    {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        startDrawingLines(bufferbuilder);

        drawBoundingBoxLinesX(bufferbuilder, minX, minY, minZ, maxX, maxY, maxZ, colorX);
        drawBoundingBoxLinesY(bufferbuilder, minX, minY, minZ, maxX, maxY, maxZ, colorY);
        drawBoundingBoxLinesZ(bufferbuilder, minX, minY, minZ, maxX, maxY, maxZ, colorZ);

        tessellator.draw();
    }

    private static void drawBoundingBoxLinesX(BufferBuilder buffer, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, Color4f color)
    {
        buffer.vertex(minX, minY, minZ).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(maxX, minY, minZ).color(color.r, color.g, color.b, color.a).next();

        buffer.vertex(minX, maxY, minZ).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(maxX, maxY, minZ).color(color.r, color.g, color.b, color.a).next();

        buffer.vertex(minX, minY, maxZ).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(maxX, minY, maxZ).color(color.r, color.g, color.b, color.a).next();

        buffer.vertex(minX, maxY, maxZ).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(maxX, maxY, maxZ).color(color.r, color.g, color.b, color.a).next();
    }

    private static void drawBoundingBoxLinesY(BufferBuilder buffer, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, Color4f color)
    {
        buffer.vertex(minX, minY, minZ).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(minX, maxY, minZ).color(color.r, color.g, color.b, color.a).next();

        buffer.vertex(maxX, minY, minZ).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(maxX, maxY, minZ).color(color.r, color.g, color.b, color.a).next();

        buffer.vertex(minX, minY, maxZ).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(minX, maxY, maxZ).color(color.r, color.g, color.b, color.a).next();

        buffer.vertex(maxX, minY, maxZ).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(maxX, maxY, maxZ).color(color.r, color.g, color.b, color.a).next();
    }

    private static void drawBoundingBoxLinesZ(BufferBuilder buffer, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, Color4f color)
    {
        buffer.vertex(minX, minY, minZ).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(minX, minY, maxZ).color(color.r, color.g, color.b, color.a).next();

        buffer.vertex(maxX, minY, minZ).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(maxX, minY, maxZ).color(color.r, color.g, color.b, color.a).next();

        buffer.vertex(minX, maxY, minZ).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(minX, maxY, maxZ).color(color.r, color.g, color.b, color.a).next();

        buffer.vertex(maxX, maxY, minZ).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(maxX, maxY, maxZ).color(color.r, color.g, color.b, color.a).next();
    }

    public static void renderAreaSides(BlockPos pos1, BlockPos pos2, Color4f color, MatrixStack matrices, MinecraftClient mc)
    {
        RenderSystem.enableBlend();
        RenderSystem.disableCull();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        renderAreaSidesBatched(pos1, pos2, color, 0.002, buffer, mc);

        tessellator.draw();

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /**
     * Assumes a BufferBuilder in GL_QUADS mode has been initialized
     */
    public static void renderAreaSidesBatched(BlockPos pos1, BlockPos pos2, Color4f color,
            double expand, BufferBuilder buffer, MinecraftClient mc)
    {
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        final double dx = cameraPos.x;
        final double dy = cameraPos.y;
        final double dz = cameraPos.z;
        double minX = Math.min(pos1.getX(), pos2.getX()) - dx - expand;
        double minY = Math.min(pos1.getY(), pos2.getY()) - dy - expand;
        double minZ = Math.min(pos1.getZ(), pos2.getZ()) - dz - expand;
        double maxX = Math.max(pos1.getX(), pos2.getX()) + 1 - dx + expand;
        double maxY = Math.max(pos1.getY(), pos2.getY()) + 1 - dy + expand;
        double maxZ = Math.max(pos1.getZ(), pos2.getZ()) + 1 - dz + expand;

        fi.dy.masa.malilib.render.RenderUtils.drawBoxAllSidesBatchedQuads(minX, minY, minZ, maxX, maxY, maxZ, color, buffer);
    }

    public static void renderAreaOutlineNoCorners(BlockPos pos1, BlockPos pos2,
            float lineWidth, Color4f colorX, Color4f colorY, Color4f colorZ, MinecraftClient mc)
    {
        final int xMin = Math.min(pos1.getX(), pos2.getX());
        final int yMin = Math.min(pos1.getY(), pos2.getY());
        final int zMin = Math.min(pos1.getZ(), pos2.getZ());
        final int xMax = Math.max(pos1.getX(), pos2.getX());
        final int yMax = Math.max(pos1.getY(), pos2.getY());
        final int zMax = Math.max(pos1.getZ(), pos2.getZ());

        final double expand = 0.001;
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        final double dx = cameraPos.x;
        final double dy = cameraPos.y;
        final double dz = cameraPos.z;

        final double dxMin = -dx - expand;
        final double dyMin = -dy - expand;
        final double dzMin = -dz - expand;
        final double dxMax = -dx + expand;
        final double dyMax = -dy + expand;
        final double dzMax = -dz + expand;

        final double minX = xMin + dxMin;
        final double minY = yMin + dyMin;
        final double minZ = zMin + dzMin;
        final double maxX = xMax + dxMax;
        final double maxY = yMax + dyMax;
        final double maxZ = zMax + dzMax;

        int start, end;

        RenderSystem.lineWidth(lineWidth);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        startDrawingLines(buffer);

        // Edges along the X-axis
        start = (pos1.getX() == xMin && pos1.getY() == yMin && pos1.getZ() == zMin) || (pos2.getX() == xMin && pos2.getY() == yMin && pos2.getZ() == zMin) ? xMin + 1 : xMin;
        end   = (pos1.getX() == xMax && pos1.getY() == yMin && pos1.getZ() == zMin) || (pos2.getX() == xMax && pos2.getY() == yMin && pos2.getZ() == zMin) ? xMax : xMax + 1;

        if (end > start)
        {
            buffer.vertex(start + dxMin, minY, minZ).color(colorX.r, colorX.g, colorX.b, colorX.a).next();
            buffer.vertex(end   + dxMax, minY, minZ).color(colorX.r, colorX.g, colorX.b, colorX.a).next();
        }

        start = (pos1.getX() == xMin && pos1.getY() == yMax && pos1.getZ() == zMin) || (pos2.getX() == xMin && pos2.getY() == yMax && pos2.getZ() == zMin) ? xMin + 1 : xMin;
        end   = (pos1.getX() == xMax && pos1.getY() == yMax && pos1.getZ() == zMin) || (pos2.getX() == xMax && pos2.getY() == yMax && pos2.getZ() == zMin) ? xMax : xMax + 1;

        if (end > start)
        {
            buffer.vertex(start + dxMin, maxY + 1, minZ).color(colorX.r, colorX.g, colorX.b, colorX.a).next();
            buffer.vertex(end   + dxMax, maxY + 1, minZ).color(colorX.r, colorX.g, colorX.b, colorX.a).next();
        }

        start = (pos1.getX() == xMin && pos1.getY() == yMin && pos1.getZ() == zMax) || (pos2.getX() == xMin && pos2.getY() == yMin && pos2.getZ() == zMax) ? xMin + 1 : xMin;
        end   = (pos1.getX() == xMax && pos1.getY() == yMin && pos1.getZ() == zMax) || (pos2.getX() == xMax && pos2.getY() == yMin && pos2.getZ() == zMax) ? xMax : xMax + 1;

        if (end > start)
        {
            buffer.vertex(start + dxMin, minY, maxZ + 1).color(colorX.r, colorX.g, colorX.b, colorX.a).next();
            buffer.vertex(end   + dxMax, minY, maxZ + 1).color(colorX.r, colorX.g, colorX.b, colorX.a).next();
        }

        start = (pos1.getX() == xMin && pos1.getY() == yMax && pos1.getZ() == zMax) || (pos2.getX() == xMin && pos2.getY() == yMax && pos2.getZ() == zMax) ? xMin + 1 : xMin;
        end   = (pos1.getX() == xMax && pos1.getY() == yMax && pos1.getZ() == zMax) || (pos2.getX() == xMax && pos2.getY() == yMax && pos2.getZ() == zMax) ? xMax : xMax + 1;

        if (end > start)
        {
            buffer.vertex(start + dxMin, maxY + 1, maxZ + 1).color(colorX.r, colorX.g, colorX.b, colorX.a).next();
            buffer.vertex(end   + dxMax, maxY + 1, maxZ + 1).color(colorX.r, colorX.g, colorX.b, colorX.a).next();
        }

        // Edges along the Y-axis
        start = (pos1.getX() == xMin && pos1.getY() == yMin && pos1.getZ() == zMin) || (pos2.getX() == xMin && pos2.getY() == yMin && pos2.getZ() == zMin) ? yMin + 1 : yMin;
        end   = (pos1.getX() == xMin && pos1.getY() == yMax && pos1.getZ() == zMin) || (pos2.getX() == xMin && pos2.getY() == yMax && pos2.getZ() == zMin) ? yMax : yMax + 1;

        if (end > start)
        {
            buffer.vertex(minX, start + dyMin, minZ).color(colorY.r, colorY.g, colorY.b, colorY.a).next();
            buffer.vertex(minX, end   + dyMax, minZ).color(colorY.r, colorY.g, colorY.b, colorY.a).next();
        }

        start = (pos1.getX() == xMax && pos1.getY() == yMin && pos1.getZ() == zMin) || (pos2.getX() == xMax && pos2.getY() == yMin && pos2.getZ() == zMin) ? yMin + 1 : yMin;
        end   = (pos1.getX() == xMax && pos1.getY() == yMax && pos1.getZ() == zMin) || (pos2.getX() == xMax && pos2.getY() == yMax && pos2.getZ() == zMin) ? yMax : yMax + 1;

        if (end > start)
        {
            buffer.vertex(maxX + 1, start + dyMin, minZ).color(colorY.r, colorY.g, colorY.b, colorY.a).next();
            buffer.vertex(maxX + 1, end   + dyMax, minZ).color(colorY.r, colorY.g, colorY.b, colorY.a).next();
        }

        start = (pos1.getX() == xMin && pos1.getY() == yMin && pos1.getZ() == zMax) || (pos2.getX() == xMin && pos2.getY() == yMin && pos2.getZ() == zMax) ? yMin + 1 : yMin;
        end   = (pos1.getX() == xMin && pos1.getY() == yMax && pos1.getZ() == zMax) || (pos2.getX() == xMin && pos2.getY() == yMax && pos2.getZ() == zMax) ? yMax : yMax + 1;

        if (end > start)
        {
            buffer.vertex(minX, start + dyMin, maxZ + 1).color(colorY.r, colorY.g, colorY.b, colorY.a).next();
            buffer.vertex(minX, end   + dyMax, maxZ + 1).color(colorY.r, colorY.g, colorY.b, colorY.a).next();
        }

        start = (pos1.getX() == xMax && pos1.getY() == yMin && pos1.getZ() == zMax) || (pos2.getX() == xMax && pos2.getY() == yMin && pos2.getZ() == zMax) ? yMin + 1 : yMin;
        end   = (pos1.getX() == xMax && pos1.getY() == yMax && pos1.getZ() == zMax) || (pos2.getX() == xMax && pos2.getY() == yMax && pos2.getZ() == zMax) ? yMax : yMax + 1;

        if (end > start)
        {
            buffer.vertex(maxX + 1, start + dyMin, maxZ + 1).color(colorY.r, colorY.g, colorY.b, colorY.a).next();
            buffer.vertex(maxX + 1, end   + dyMax, maxZ + 1).color(colorY.r, colorY.g, colorY.b, colorY.a).next();
        }

        // Edges along the Z-axis
        start = (pos1.getX() == xMin && pos1.getY() == yMin && pos1.getZ() == zMin) || (pos2.getX() == xMin && pos2.getY() == yMin && pos2.getZ() == zMin) ? zMin + 1 : zMin;
        end   = (pos1.getX() == xMin && pos1.getY() == yMin && pos1.getZ() == zMax) || (pos2.getX() == xMin && pos2.getY() == yMin && pos2.getZ() == zMax) ? zMax : zMax + 1;

        if (end > start)
        {
            buffer.vertex(minX, minY, start + dzMin).color(colorZ.r, colorZ.g, colorZ.b, colorZ.a).next();
            buffer.vertex(minX, minY, end   + dzMax).color(colorZ.r, colorZ.g, colorZ.b, colorZ.a).next();
        }

        start = (pos1.getX() == xMax && pos1.getY() == yMin && pos1.getZ() == zMin) || (pos2.getX() == xMax && pos2.getY() == yMin && pos2.getZ() == zMin) ? zMin + 1 : zMin;
        end   = (pos1.getX() == xMax && pos1.getY() == yMin && pos1.getZ() == zMax) || (pos2.getX() == xMax && pos2.getY() == yMin && pos2.getZ() == zMax) ? zMax : zMax + 1;

        if (end > start)
        {
            buffer.vertex(maxX + 1, minY, start + dzMin).color(colorZ.r, colorZ.g, colorZ.b, colorZ.a).next();
            buffer.vertex(maxX + 1, minY, end   + dzMax).color(colorZ.r, colorZ.g, colorZ.b, colorZ.a).next();
        }

        start = (pos1.getX() == xMin && pos1.getY() == yMax && pos1.getZ() == zMin) || (pos2.getX() == xMin && pos2.getY() == yMax && pos2.getZ() == zMin) ? zMin + 1 : zMin;
        end   = (pos1.getX() == xMin && pos1.getY() == yMax && pos1.getZ() == zMax) || (pos2.getX() == xMin && pos2.getY() == yMax && pos2.getZ() == zMax) ? zMax : zMax + 1;

        if (end > start)
        {
            buffer.vertex(minX, maxY + 1, start + dzMin).color(colorZ.r, colorZ.g, colorZ.b, colorZ.a).next();
            buffer.vertex(minX, maxY + 1, end   + dzMax).color(colorZ.r, colorZ.g, colorZ.b, colorZ.a).next();
        }

        start = (pos1.getX() == xMax && pos1.getY() == yMax && pos1.getZ() == zMin) || (pos2.getX() == xMax && pos2.getY() == yMax && pos2.getZ() == zMin) ? zMin + 1 : zMin;
        end   = (pos1.getX() == xMax && pos1.getY() == yMax && pos1.getZ() == zMax) || (pos2.getX() == xMax && pos2.getY() == yMax && pos2.getZ() == zMax) ? zMax : zMax + 1;

        if (end > start)
        {
            buffer.vertex(maxX + 1, maxY + 1, start + dzMin).color(colorZ.r, colorZ.g, colorZ.b, colorZ.a).next();
            buffer.vertex(maxX + 1, maxY + 1, end   + dzMax).color(colorZ.r, colorZ.g, colorZ.b, colorZ.a).next();
        }

        tessellator.draw();
    }


   
    
}
