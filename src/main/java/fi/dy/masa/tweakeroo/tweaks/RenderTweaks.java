package fi.dy.masa.tweakeroo.tweaks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.malilib.util.Color4f;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.restrictions.UsageRestriction.ListType;
import fi.dy.masa.tweakeroo.Tweakeroo;
import fi.dy.masa.tweakeroo.config.Configs;
import fi.dy.masa.tweakeroo.config.FeatureToggle;
import fi.dy.masa.tweakeroo.config.Hotkeys;
import fi.dy.masa.tweakeroo.items.ItemList;
import fi.dy.masa.tweakeroo.mixin.MixinPistonBlock;
import fi.dy.masa.tweakeroo.renderer.OverlayRenderer;
import fi.dy.masa.tweakeroo.renderer.RenderUtils;
import fi.dy.masa.tweakeroo.util.IMixinWindow;
import fi.dy.masa.tweakeroo.util.MiscUtils;
import fi.dy.masa.tweakeroo.world.FakeChunk;
import fi.dy.masa.tweakeroo.world.FakeWorld;
import net.minecraft.block.AbstractChestBlock;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.BrewingStandBlock;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.CraftingTableBlock;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.DropperBlock;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.block.piston.PistonHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.Shader;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

public class RenderTweaks {

    private static ConcurrentHashMap<Long, ListMapEntry> SELECTIVE_BLACKLIST = new ConcurrentHashMap<Long, ListMapEntry>();
    private static ConcurrentHashMap<Long, ListMapEntry> SELECTIVE_WHITELIST = new ConcurrentHashMap<Long, ListMapEntry>();

    private static ConcurrentHashMap<Long, ListMapEntry> CACHED_LIST = new ConcurrentHashMap<Long, ListMapEntry>();

    public static final int PASSTHROUGH = 1024;

    private static Color4f colorPos1 = new Color4f(1f, 0.0625f, 0.0625f);
    private static Color4f colorPos2 = new Color4f(0.0625f, 0.0625f, 1f);
    private static Color4f sideColor = Color4f.fromColor(0x30FFFFFF);
    private static Color4f colorOverlapping = new Color4f(1f, 0.0625f, 1f);
    private static Color4f colorX = new Color4f(1f, 0.25f, 0.25f);
    private static Color4f colorY = new Color4f(0.25f, 1f, 0.25f);
    private static Color4f colorZ = new Color4f(0.25f, 0.25f, 1f);
    private static Color4f colorLooking = new Color4f(1.0f, 1.0f, 1.0f, 0.6f);
    private static Color4f colorWhitelist = new Color4f(0.1f, 0.7f, 0.1f, 0.25f);
    private static Color4f colorBlacklist = new Color4f(0.7f, 0.1f, 0.1f, 0.25f);
    private static Color4f colorSearch = new Color4f(0.9f, 0f, 0.7f, 0.25f);

    public static Selection AREA_SELECTION = new Selection();

    public static BlockPos posLookingAt = null;

    public static Framebuffer endframebuffer = new SimpleFramebuffer(1, 1, true, MinecraftClient.IS_SYSTEM_MAC);

    public static ConcurrentHashMap<Long, ContainerEntry> CONTAINERCACHE = new ConcurrentHashMap<Long, ContainerEntry>();
    public static ArrayList<ContainerEntry> CONTAINERS_WAITING = new ArrayList<ContainerEntry>();
    public static int CURRENT_CONTAINER = 0;
    public static long LAST_CHECK = 0;
    private static ScreenHandlerType<?> CURRENT_SCREEN_TYPE = null;
    private static int CURRENT_SYNC_ID = -1;
    private static HashMap<Long, ArrayList<Item>> CACHED_OVERLAY_DATA = new HashMap<Long, ArrayList<Item>>();

    private static ListType previousType = (ListType) Configs.Lists.SELECTIVE_BLOCKS_LIST_TYPE.getOptionListValue();
    private static boolean previousSelectiveToggle = FeatureToggle.TWEAK_SELECTIVE_BLOCKS_RENDERING.getBooleanValue();

    private static FakeWorld fakeWorld = null;

    public static void resetWorld(DynamicRegistryManager registry, int loadDistance) {
        fakeWorld = new FakeWorld(registry, loadDistance);
    }

    public static FakeWorld getFakeWorld() {
        return fakeWorld;
    }

    public static void onTick() {
        // Dumb rendundancy due to replaymod
        MinecraftClient mc = MinecraftClient.getInstance();
        if (FeatureToggle.TWEAK_AREA_SELECTOR.getBooleanValue()) {
            if (mc.options.attackKey.isPressed()) {
                select(false);
            }

            if (mc.options.useKey.isPressed()) {
                select(true);
            }
        }

    }

    public static void scanContainersNearby() {

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player.isSneaking())
            return;
        long now = System.currentTimeMillis();
        if (!CONTAINERS_WAITING.isEmpty()) {

            if (now - LAST_CHECK < 1000 * 5) {
                return;
            }

            onDesync();
        }
        int reach = Math.min((int) mc.interactionManager.getReachDistance(), 6);
        BlockPos.Mutable tempPos = new BlockPos.Mutable();
        BlockPos playerPos = MiscUtils.getPlayerHeadPos(mc.player);
        CURRENT_CONTAINER = 0;
        CONTAINERS_WAITING.clear();
        LAST_CHECK = now;
        for (int ox = -reach; ox < reach; ox++) {
            for (int oy = -reach; oy < reach; oy++) {
                for (int oz = -reach; oz < reach; oz++) {
                    tempPos.set(playerPos.getX() + ox, playerPos.getY() + oy, playerPos.getZ() + oz);

                    if (!MiscUtils.isInReach(tempPos, mc.player, reach)
                            || !CONTAINERCACHE.containsKey(tempPos.asLong()))
                        continue;
                    ContainerEntry entry = CONTAINERCACHE.get(tempPos.asLong());
                    if (entry.status == 2)
                        continue;

                    BlockEntity blockEntity = mc.world.getBlockEntity(tempPos);
                    BlockState state = mc.world.getBlockState(tempPos);
                    Block block = state.getBlock();
                    boolean valid = true;
                    if (block instanceof ChestBlock) {
                        valid = !ChestBlock.isChestBlocked(mc.world, tempPos);
                        if (valid && state.get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE) {
                            if (ChestBlock.isChestBlocked(mc.world, tempPos.offset(ChestBlock.getFacing(state))))
                                valid = false;
                        }
                        if (state.get(ChestBlock.CHEST_TYPE) == ChestType.LEFT) {
                            entry.skipCount = true;
                        }
                    } else if (block instanceof ShulkerBoxBlock) {
                        ShulkerBoxBlockEntity lv1 = (ShulkerBoxBlockEntity) blockEntity;
                        boolean flag;
                        if (lv1.getAnimationStage() == ShulkerBoxBlockEntity.AnimationStage.CLOSED) {
                            flag = mc.world.isSpaceEmpty(ShulkerEntity
                                    .calculateBoundingBox((Direction) state.get(ShulkerBoxBlock.FACING), 0.0F, 0.5F)
                                    .offset(tempPos).contract(1.0E-6D));
                        } else {
                            flag = true;
                        }

                        valid = flag || mc.player.isSpectator();
                    }

                    if (!valid) {
                        entry.status = 3;
                        continue;
                    }

                    if (blockEntity == null || !(blockEntity instanceof LockableContainerBlockEntity))
                        continue;

                    entry.status = 1;
                    CONTAINERS_WAITING.add(entry);
                    BlockHitResult hitResult = new BlockHitResult(new Vec3d(0.5, 0.5, 0.5), Direction.UP, entry.pos,
                            false);
                    mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, hitResult);

                }
            }
        }

    }

    public static int getHeightOffsetWithAspectRatio(double r, int w, int currentHeight) {
        if (!FeatureToggle.TWEAK_STANDARD_ASPECT_RATIO.getBooleanValue()) {
            return 0;
        }

        int newHeight = (int) ((double) w / r);

        return Math.max(0, (currentHeight - newHeight));
    }

    public static void render(MatrixStack matrices) {
        MinecraftClient mc = MinecraftClient.getInstance();
        float expand = 0.001f;
        float lineWidthBlockBox = 2f;

        if (FeatureToggle.TWEAK_CONTAINER_SCAN.getBooleanValue()) {
            scanContainersNearby();
        }
        if (FeatureToggle.TWEAK_CONTAINER_SCAN_COUNTS.getBooleanValue()
                || (FeatureToggle.TWEAK_CONTAINER_SCAN.getBooleanValue()
                        && !Configs.Disable.DISABLE_CONTAINER_SCAN_OUTLINES.getBooleanValue())) {

            matrices.push();
            fi.dy.masa.malilib.render.RenderUtils.color(1f, 1f, 1f, 1f);
            fi.dy.masa.malilib.render.RenderUtils.setupBlend();
            RenderSystem.disableDepthTest();
            // RenderSystem.disableLighting();
            // RenderSystem.depthMask(false);
            RenderSystem.disableTexture();
            // RenderSystem.alphaFunc(GL11.GL_GREATER, 0.01F);

            RenderSystem.enablePolygonOffset();
            RenderSystem.polygonOffset(-1.2f, -0.2f);

            if (!Configs.Disable.DISABLE_CONTAINER_SCAN_OUTLINES.getBooleanValue())
                renderUnknownContainerBoxes(matrices);

            if (FeatureToggle.TWEAK_CONTAINER_SCAN_COUNTS.getBooleanValue())
                renderContainerBoxesInfo(matrices);

            RenderSystem.polygonOffset(0f, 0f);
            RenderSystem.disablePolygonOffset();
            matrices.pop();
            RenderSystem.enableTexture();

        }

        if (CACHED_OVERLAY_DATA.size() > 0) {

            matrices.push();
            fi.dy.masa.malilib.render.RenderUtils.color(1f, 1f, 1f, 1f);
            fi.dy.masa.malilib.render.RenderUtils.setupBlend();
            RenderSystem.disableDepthTest();
            // RenderSystem.disableLighting();
            RenderSystem.depthMask(false);
            RenderSystem.disableTexture();
            // RenderSystem.alphaFunc(GL11.GL_GREATER, 0.01F);

            RenderSystem.enablePolygonOffset();
            RenderSystem.polygonOffset(-1.2f, -0.2f);

            renderSearchedContainerBoxes(matrices);

            renderSearchedContainerIcons(matrices);

            RenderSystem.polygonOffset(0f, 0f);
            RenderSystem.disablePolygonOffset();
            matrices.pop();
            RenderSystem.enableTexture();

        }
        if (FeatureToggle.TWEAK_AREA_SELECTOR.getBooleanValue()
                || FeatureToggle.TWEAK_SELECTIVE_BLOCKS_RENDER_OUTLINE.getBooleanValue()) {

            if (FeatureToggle.TWEAK_AREA_SELECTOR.getBooleanValue())
                updateLookingAt();

            matrices.push();
            fi.dy.masa.malilib.render.RenderUtils.color(1f, 1f, 1f, 1f);
            fi.dy.masa.malilib.render.RenderUtils.setupBlend();
            RenderSystem.disableDepthTest();
            // RenderSystem.disableLighting();
            // RenderSystem.depthMask(false);
            RenderSystem.disableTexture();
            // RenderSystem.alphaFunc(GL11.GL_GREATER, 0.01F);

            RenderSystem.enablePolygonOffset();
            RenderSystem.polygonOffset(-1.2f, -0.2f);

            if (FeatureToggle.TWEAK_SELECTIVE_BLOCKS_RENDER_OUTLINE.getBooleanValue()) {
                renderLists(matrices);
            }
            if (FeatureToggle.TWEAK_AREA_SELECTOR.getBooleanValue()) {
                if (posLookingAt != null) {
                    RenderUtils.renderBlockOutline(posLookingAt, expand, lineWidthBlockBox, colorLooking, mc);
                }
                renderSelection(matrices, AREA_SELECTION);
            }

            RenderSystem.polygonOffset(0f, 0f);
            RenderSystem.disablePolygonOffset();
            matrices.pop();
            RenderSystem.enableTexture();
            RenderSystem.depthMask(true);

        }

    }

    private static void renderContainerBoxesInfo(MatrixStack matrices) {
        MinecraftClient mc = MinecraftClient.getInstance();
        String fullIndicator = Formatting.GREEN + " •";
        for (ContainerEntry entry : CONTAINERCACHE.values()) {
            if (entry.status == 2) { // TODO && !CACHED_OVERLAY_DATA.containsKey(entry.pos.asLong()) -> when icons
                                     // are done

                if (entry.itemCount < Configs.Generic.CONTAINER_SCAN_MIN_ITEMS.getIntegerValue()
                        || entry.typeCount < Configs.Generic.CONTAINER_SCAN_MIN_TYPES.getIntegerValue()) {
                    if (MiscUtils.isInReach(entry.pos, mc.player, 10)) {
                        if (entry.itemCount == 0) {
                            OverlayRenderer.drawString("Empty", entry.pos, sideColor.intValue, -0.5F);
                        } else {
                            OverlayRenderer.drawString(entry.itemCount + " items" + (entry.isFull ? " •" : ""),
                                    entry.pos, sideColor.intValue, -0.5F);
                            OverlayRenderer.drawString(entry.typeCount + " types" + (entry.areSlotsCovered ? " •" : ""),
                                    entry.pos, sideColor.intValue, 0.5F);
                        }
                    }
                    continue;
                }

                OverlayRenderer.drawString(entry.itemCount + " items" + (entry.isFull ? fullIndicator : ""), entry.pos,
                        Formatting.GOLD.getColorValue(), -0.5F);
                OverlayRenderer.drawString(entry.typeCount + " types" + (entry.areSlotsCovered ? fullIndicator : ""),
                        entry.pos, Formatting.GOLD.getColorValue(), 0.5F);

            }

        }

    }

    private static void renderLists(MatrixStack matrices) {
        float expand = 0.001f;
        float lineWidthBlockBox = 2f;
        MinecraftClient mc = MinecraftClient.getInstance();

        for (ListMapEntry entry : SELECTIVE_BLACKLIST.values()) {
            RenderUtils.renderBlockOutline(entry.currentPosition, expand, lineWidthBlockBox, colorBlacklist, mc);
        }
        for (ListMapEntry entry : SELECTIVE_WHITELIST.values()) {
            RenderUtils.renderBlockOutline(entry.currentPosition, expand, lineWidthBlockBox, colorWhitelist, mc);
        }
    }

    private static void renderUnknownContainerBoxes(MatrixStack matrices) {
        float expand = 0.001f;
        float lineWidthBlockBox = 2f;
        MinecraftClient mc = MinecraftClient.getInstance();

        for (ContainerEntry entry : CONTAINERCACHE.values()) {
            if (entry.status == 0) {
                RenderUtils.renderBlockOutline(entry.pos, expand, lineWidthBlockBox, colorWhitelist, mc);
            } else if (entry.status == 3) {
                RenderUtils.renderBlockOutline(entry.pos, expand, lineWidthBlockBox, colorBlacklist, mc);
            } else if (entry.status == 1) {
                RenderUtils.renderBlockOutline(entry.pos, expand, lineWidthBlockBox, colorOverlapping, mc);
            }
        }
    }

    private static void renderSearchedContainerBoxes(MatrixStack matrices) {
        float expand = 0.001f;
        MinecraftClient mc = MinecraftClient.getInstance();

        for (Entry<Long, ArrayList<Item>> entry : CACHED_OVERLAY_DATA.entrySet()) {
            BlockPos pos = BlockPos.fromLong(entry.getKey());
            RenderUtils.renderBlockOutline(pos, expand, 8, colorSearch,
                    mc);
            RenderUtils.renderAreaSides(pos, pos, colorSearch, matrices, mc);
        }

    }

    private static void renderSearchedContainerIcons(MatrixStack matrices) { // TODO figure out item icon rendering
        /*
         * MinecraftClient mc = MinecraftClient.getInstance();
         * 
         * for (Entry<Long, ArrayList<Item>> entry : CACHED_OVERLAY_DATA.entrySet()) { }
         * 
         */
    }

    public static void updateLookingAt() {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.crosshairTarget != null && mc.crosshairTarget instanceof BlockHitResult) {
            posLookingAt = ((BlockHitResult) mc.crosshairTarget).getBlockPos();

            // use offset
            if (Hotkeys.AREA_SELECTION_OFFSET.getKeybind().isKeybindHeld())
                posLookingAt = posLookingAt.offset(((BlockHitResult) mc.crosshairTarget).getSide());
        } else {
            posLookingAt = null;
        }
    }

    public static void select(boolean pos2) {
        if (posLookingAt == null)
            return;
        if (pos2) {
            AREA_SELECTION.pos2 = posLookingAt;
        } else {
            AREA_SELECTION.pos1 = posLookingAt;
        }
    }

    public static boolean isInSelection(BlockPos pos) {
        int minX = Math.min(AREA_SELECTION.pos1.getX(), AREA_SELECTION.pos2.getX());
        int minY = Math.min(AREA_SELECTION.pos1.getY(), AREA_SELECTION.pos2.getY());
        int minZ = Math.min(AREA_SELECTION.pos1.getZ(), AREA_SELECTION.pos2.getZ());
        int maxX = Math.max(AREA_SELECTION.pos1.getX(), AREA_SELECTION.pos2.getX());
        int maxY = Math.max(AREA_SELECTION.pos1.getY(), AREA_SELECTION.pos2.getY());
        int maxZ = Math.max(AREA_SELECTION.pos1.getZ(), AREA_SELECTION.pos2.getZ());

        return !(pos.getX() < minX || pos.getX() > maxX || pos.getY() < minY || pos.getY() > maxY || pos.getZ() < minZ
                || pos.getZ() > maxZ);
    }

    public static void addSelectionToList() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null)
            return;
        if (AREA_SELECTION.pos1 == null || AREA_SELECTION.pos2 == null) {
            InfoUtils.printActionbarMessage("Please set an area first");
            return;
        }
        ListType type = (ListType) Configs.Lists.SELECTIVE_BLOCKS_LIST_TYPE.getOptionListValue();

        if (type == ListType.NONE) {
            InfoUtils.printActionbarMessage("No list selected");
            return;
        }

        Iterator<BlockPos> iterator = BlockPos.iterate(AREA_SELECTION.pos1, AREA_SELECTION.pos2).iterator();
        int count = 0;
        ConcurrentHashMap<Long, ListMapEntry> list = (type == ListType.WHITELIST) ? SELECTIVE_WHITELIST
                : SELECTIVE_BLACKLIST;

        while (iterator.hasNext()) {
            BlockPos pos = iterator.next().toImmutable();

            if (Configs.Generic.AREA_SELECTION_USE_ALL.getBooleanValue() || !mc.world.getBlockState(pos).isAir()) {
                if (!list.containsKey(pos.asLong())) {
                    list.put(pos.asLong(), new ListMapEntry(pos));
                    count++;
                }
            }
        }
        rebuildStrings();
        InfoUtils.printActionbarMessage("Added " + count + " blocks");
    }

    public static void showPos(BlockPos pos) {

    }

    public static void hidePos(BlockPos pos) {

    }

    public static void removeSelectionFromList() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null)
            return;
        if (AREA_SELECTION.pos1 == null || AREA_SELECTION.pos2 == null) {
            InfoUtils.printActionbarMessage("Please set an area first");
            return;
        }
        ListType type = (ListType) Configs.Lists.SELECTIVE_BLOCKS_LIST_TYPE.getOptionListValue();

        if (type == ListType.NONE) {
            InfoUtils.printActionbarMessage("No list selected");
            return;
        }

        Iterator<BlockPos> iterator = BlockPos.iterate(AREA_SELECTION.pos1, AREA_SELECTION.pos2).iterator();
        int count = 0;
        ConcurrentHashMap<Long, ListMapEntry> list = (type == ListType.WHITELIST) ? SELECTIVE_WHITELIST
                : SELECTIVE_BLACKLIST;

        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            if (list.containsKey(pos.asLong())) {
                list.remove(pos.asLong());
                count++;
            }

        }
        rebuildStrings();
        InfoUtils.printActionbarMessage("Removed " + count + " blocks");
    }

    // From litematica
    public static void renderSelection(MatrixStack matrices, Selection selection) {

        BlockPos pos1 = selection.pos1;
        BlockPos pos2 = selection.pos2;
        if (pos1 == null && pos2 == null) {
            return;
        }
        float expand = 0.001f;
        float lineWidthBlockBox = 2f;
        float lineWidthArea = 1.5f;

        MinecraftClient mc = MinecraftClient.getInstance();

        if (pos1 != null && pos2 != null) {
            if (pos1.equals(pos2) == false) {
                RenderUtils.renderAreaOutlineNoCorners(pos1, pos2, lineWidthArea, colorX, colorY, colorZ, mc);

                RenderUtils.renderAreaSides(pos1, pos2, sideColor, matrices, mc);

                RenderUtils.renderBlockOutline(pos1, expand, lineWidthBlockBox, colorPos1, mc);
                RenderUtils.renderBlockOutline(pos2, expand, lineWidthBlockBox, colorPos2, mc);
            } else {
                RenderUtils.renderBlockOutlineOverlapping(pos1, expand, lineWidthBlockBox, colorPos1, colorPos2,
                        colorOverlapping, matrices, mc);
            }
        } else {
            if (pos1 != null) {
                RenderUtils.renderBlockOutline(pos1, expand, lineWidthBlockBox, colorPos1, mc);
            }

            if (pos2 != null) {
                RenderUtils.renderBlockOutline(pos2, expand, lineWidthBlockBox, colorPos2, mc);
            }
        }

    }

    public static void onPistonEvent(BlockState state, World world, BlockPos pos, int type, int data) {

        if (!Configs.Generic.SELECTIVE_BLOCKS_TRACK_PISTONS.getBooleanValue()
                || (!FeatureToggle.TWEAK_SELECTIVE_BLOCKS_RENDERING.getBooleanValue()
                        && !FeatureToggle.TWEAK_SELECTIVE_BLOCKS_RENDER_OUTLINE.getBooleanValue())
                || (SELECTIVE_WHITELIST.size() == 0 && SELECTIVE_BLACKLIST.size() == 0))
            return;

        if (type == 2)
            return;

        Direction pushDirection = Direction.byId(data & 7);

        PistonHandler pistonHandler = new PistonHandler(world, pos, pushDirection, type == 0);

        BlockState state2 = null;
        BlockEntity entity = null;
        BlockEntity entity2 = null;

        if (type != 0 && !((MixinPistonBlock) state.getBlock()).getSticky())
            return; // non sticky pistons do nothing

        if (type != 0) {

            state2 = world.getBlockState(pos.offset(pushDirection)); // piston head
            entity = world.getBlockEntity(pos);
            entity2 = world.getBlockEntity(pos.offset(pushDirection));
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.FORCE_STATE);
            world.setBlockState(pos.offset(pushDirection), Blocks.AIR.getDefaultState(), Block.FORCE_STATE);
        }
        boolean moveSuccess = pistonHandler.calculatePush();

        if (type != 0) {
            world.setBlockState(pos, state, Block.FORCE_STATE);
            world.setBlockState(pos.offset(pushDirection), state2, Block.FORCE_STATE);
            if (entity != null) {
                world.addBlockEntity(entity);
            }
            if (entity2 != null) {
                world.addBlockEntity(entity2);
            }
        }

        boolean attatchedWhitelist = SELECTIVE_WHITELIST
                .containsKey(pos.offset(pushDirection, (type == 0) ? 1 : 2).asLong());
        boolean attatchedBlacklist = SELECTIVE_BLACKLIST
                .containsKey(pos.offset(pushDirection, (type == 0) ? 1 : 2).asLong());

        boolean whitelisted = SELECTIVE_WHITELIST.containsKey(pos.asLong());
        boolean blacklisted = SELECTIVE_BLACKLIST.containsKey(pos.asLong());

        if (type == 0) { // extending
            if (whitelisted) {
                if (attatchedWhitelist) {
                    SELECTIVE_WHITELIST.get(pos.offset(pushDirection).asLong()).preserve = true;
                } else {
                    SELECTIVE_WHITELIST.put(pos.offset(pushDirection, 1).asLong(),
                            new ListMapEntry(pos.offset(pushDirection, 1)));
                }
            }
            if (blacklisted) {
                if (attatchedBlacklist) {
                    SELECTIVE_BLACKLIST.get(pos.offset(pushDirection).asLong()).preserve = true;
                } else {
                    SELECTIVE_BLACKLIST.put(pos.offset(pushDirection, 1).asLong(),
                            new ListMapEntry(pos.offset(pushDirection, 1)));
                }
            }
        }
        if (moveSuccess) {
            // List<BlockPos> brokenBlocks = pistonHandler.getBrokenBlocks();
            List<BlockPos> movedBlocks = pistonHandler.getMovedBlocks();

            ArrayList<ListMapEntry> toMoveWhitelist = new ArrayList<ListMapEntry>();
            ArrayList<ListMapEntry> toMoveBlacklist = new ArrayList<ListMapEntry>();

            ArrayList<ListMapEntry> toAddWhitelist = new ArrayList<ListMapEntry>();
            ArrayList<ListMapEntry> toAddBlacklist = new ArrayList<ListMapEntry>();

            for (BlockPos p : movedBlocks) {
                long key = p.asLong();
                if (SELECTIVE_WHITELIST.containsKey(key)) {
                    ListMapEntry entry = SELECTIVE_WHITELIST.get(key);
                    toMoveWhitelist.add(entry);

                    SELECTIVE_WHITELIST.remove(key);
                    if (entry.preserve) {
                        entry.preserve = false;
                        toAddWhitelist.add(new ListMapEntry(p));
                    }
                }

                if (SELECTIVE_BLACKLIST.containsKey(key)) {
                    ListMapEntry entry = SELECTIVE_BLACKLIST.get(key);
                    toMoveBlacklist.add(entry);

                    SELECTIVE_BLACKLIST.remove(key);
                    if (entry.preserve) {
                        entry.preserve = false;
                        toAddBlacklist.add(new ListMapEntry(p));
                    }
                }
            }

            for (ListMapEntry p : toMoveWhitelist) {
                p.currentPosition = p.currentPosition.offset(pushDirection, (type == 0) ? 1 : -1);
                if (SELECTIVE_WHITELIST.containsKey(p.currentPosition.asLong()))
                    p.preserve = true;
                SELECTIVE_WHITELIST.put(p.currentPosition.asLong(), p);
            }

            for (ListMapEntry p : toMoveBlacklist) {
                p.currentPosition = p.currentPosition.offset(pushDirection, (type == 0) ? 1 : -1);
                if (SELECTIVE_BLACKLIST.containsKey(p.currentPosition.asLong()))
                    p.preserve = true;
                SELECTIVE_BLACKLIST.put(p.currentPosition.asLong(), p);
            }

            for (ListMapEntry p : toAddWhitelist) {
                SELECTIVE_WHITELIST.put(p.currentPosition.asLong(), p);
            }

            for (ListMapEntry p : toAddBlacklist) {
                SELECTIVE_BLACKLIST.put(p.currentPosition.asLong(), p);
            }
        }
        reloadSelective();
    }

    public static boolean isPositionValidForRendering(BlockPos pos) {
        return isPositionValidForRendering(pos.asLong());
    }

    public static boolean isPositionValidForRendering(long key) {
        if (!FeatureToggle.TWEAK_SELECTIVE_BLOCKS_RENDERING.getBooleanValue()) {
            return true;
        }

        switch ((ListType) Configs.Lists.SELECTIVE_BLOCKS_LIST_TYPE.getOptionListValue()) {
            case NONE:
                return true;
            case WHITELIST:
                return SELECTIVE_WHITELIST.containsKey(key);
            case BLACKLIST:
                return !SELECTIVE_BLACKLIST.containsKey(key);
        }

        return false;
    }

    public static void rebuildLists() {
        SELECTIVE_BLACKLIST.clear();
        SELECTIVE_WHITELIST.clear();
        putMapFromString(SELECTIVE_BLACKLIST, Configs.Lists.SELECTIVE_BLOCKS_BLACKLIST.getStringValue());
        putMapFromString(SELECTIVE_WHITELIST, Configs.Lists.SELECTIVE_BLOCKS_WHITELIST.getStringValue());

        reloadSelective();

    }

    public static void updateSelectiveAtPos(BlockPos pos) {
        MinecraftClient mc = MinecraftClient.getInstance();
        BlockState state = mc.world.getBlockState(pos);
        if (RenderTweaks.isPositionValidForRendering(pos)) {
            if (state.isAir()) {
                BlockState originalState = fakeWorld.getBlockState(pos);
                if (!originalState.isAir()) {
                    BlockEntity be = fakeWorld.getBlockEntity(pos);
                    fakeWorld.setBlockState(pos, Blocks.AIR.getDefaultState());
                    mc.world.setBlockState(pos, originalState,
                            Block.NOTIFY_ALL | Block.FORCE_STATE | PASSTHROUGH);
                    if (be != null)
                        mc.world.addBlockEntity(be);
                }
            }
        } else {
            if (!state.isAir()) {
                BlockEntity be = mc.world.getBlockEntity(pos);
                mc.world.setBlockState(pos, Blocks.AIR.getDefaultState(),
                        Block.NOTIFY_ALL | Block.FORCE_STATE | PASSTHROUGH);
                setFakeBlockState(mc.world, pos, state, be);
            }
        }
    }

    public static void reloadSelective() {
        MinecraftClient.getInstance().execute(() -> {
            reloadSelectiveInternal();
        });
    }

    public static void reloadSelectiveInternal() {

        MinecraftClient mc = MinecraftClient.getInstance();
        ListType listtype = (ListType) Configs.Lists.SELECTIVE_BLOCKS_LIST_TYPE.getOptionListValue();
        boolean toggle = FeatureToggle.TWEAK_SELECTIVE_BLOCKS_RENDERING.getBooleanValue();
        if (mc.world == null) {
            CACHED_LIST.clear();
            if (listtype != ListType.NONE) {
                ConcurrentHashMap<Long, ListMapEntry> list = (listtype == ListType.WHITELIST) ? SELECTIVE_WHITELIST
                        : SELECTIVE_BLACKLIST;
                Iterator<ListMapEntry> iterator = list.values().iterator();
                while (iterator.hasNext()) {
                    ListMapEntry entry = iterator.next();
                    CACHED_LIST.put(entry.currentPosition.asLong(), entry);
                }
            }

            previousSelectiveToggle = toggle;
            previousType = listtype;
            return;
        }
        if (listtype != previousType || toggle != previousSelectiveToggle) {
            ChunkPos center = fakeWorld.getChunkManager().getChunkMapCenter();
            int radius = fakeWorld.getChunkManager().getRadius();

            BlockPos.Mutable pos = new BlockPos.Mutable();

            for (int cx = center.x - radius; cx <= center.x + radius; cx++) {
                for (int cz = center.z - radius; cz <= center.z + radius; cz++) {

                    WorldChunk chunk = (WorldChunk) mc.world.getChunkManager().getChunk(cx, cz, ChunkStatus.FULL,
                            false);
                    FakeChunk fakeChunk = fakeWorld.getChunkManager().getChunkIfExists(cx, cz);
                    if (chunk != null && fakeChunk != null) {
                        ChunkPos cpos = chunk.getPos();
                        ChunkSection[] sections = chunk.getSectionArray();
                        ChunkSection[] fakeSections = fakeChunk.getSectionArray();
                        for (int i = 0; i < sections.length; i++) {
                            ChunkSection section = sections[i];
                            if (!section.isEmpty() || !fakeSections[i].isEmpty()) {
                                for (int x = 0; x < 16; x++) {
                                    for (int y = 0; y < 16; y++) {
                                        for (int z = 0; z < 16; z++) {
                                            pos.set(x + cpos.getStartX(), y + section.getYOffset(),
                                                    z + cpos.getStartZ());
                                            updateSelectiveAtPos(pos);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            CACHED_LIST.clear();
            if (listtype != ListType.NONE) {
                ConcurrentHashMap<Long, ListMapEntry> list = (listtype == ListType.WHITELIST) ? SELECTIVE_WHITELIST
                        : SELECTIVE_BLACKLIST;
                Iterator<ListMapEntry> iterator = list.values().iterator();
                while (iterator.hasNext()) {
                    ListMapEntry entry = iterator.next();
                    CACHED_LIST.put(entry.currentPosition.asLong(), entry);
                }
            }
        } else if (listtype != ListType.NONE) {
            ConcurrentHashMap<Long, ListMapEntry> list = (listtype == ListType.WHITELIST) ? SELECTIVE_WHITELIST
                    : SELECTIVE_BLACKLIST;
            Iterator<ListMapEntry> iterator = CACHED_LIST.values().iterator();
            while (iterator.hasNext()) {
                ListMapEntry entry = iterator.next();
                if (!list.containsKey(entry.currentPosition.asLong())) {
                    updateSelectiveAtPos(entry.currentPosition);
                    iterator.remove();
                }
            }

            iterator = list.values().iterator();
            while (iterator.hasNext()) {
                ListMapEntry entry = iterator.next();
                if (!CACHED_LIST.containsKey(entry.currentPosition.asLong())) {
                    updateSelectiveAtPos(entry.currentPosition);
                    CACHED_LIST.put(entry.currentPosition.asLong(), entry);
                }
            }
        }

        previousSelectiveToggle = toggle;
        previousType = listtype;
    }

    public static void onLightUpdateEvent(int chunkX, int chunkZ, CallbackInfo ci) {

        if (true || !FeatureToggle.TWEAK_SELECTIVE_BLOCKS_RENDERING.getBooleanValue()) {
            return;
        }

        ListType listtype = (ListType) Configs.Lists.SELECTIVE_BLOCKS_LIST_TYPE.getOptionListValue();

        if (listtype == ListType.NONE) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean found = false;
        if (mc != null && mc.world != null && mc.world.getLightingProvider() != null) {

            ConcurrentHashMap<Long, ListMapEntry> list = (listtype == ListType.WHITELIST) ? SELECTIVE_WHITELIST
                    : SELECTIVE_BLACKLIST;

            int minX = chunkX * 16 - 1;
            int minZ = chunkZ * 16 - 1;
            int maxX = chunkX * 16 + 16;
            int maxZ = chunkZ * 16 + 16;

            for (ListMapEntry entry : list.values()) {

                int x = entry.currentPosition.getX();
                int z = entry.currentPosition.getZ();
                if (x >= minX && z >= minZ && x <= maxX && z <= maxZ) {
                    found = true;
                    break;
                }
            }
        }
        if (found) {
            ci.cancel();
        }

    }

    public static void rebuildStrings() {
        String whitelist = getStringFromMap(SELECTIVE_WHITELIST);
        String blacklist = getStringFromMap(SELECTIVE_BLACKLIST);

        Configs.Lists.SELECTIVE_BLOCKS_WHITELIST.setValueFromString(whitelist);
        Configs.Lists.SELECTIVE_BLOCKS_BLACKLIST.setValueFromString(blacklist);
    }

    public static void putMapFromString(ConcurrentHashMap<Long, ListMapEntry> map, String str) {

        String[] parts = str.split("\\|");

        for (int i = 0; i < parts.length; i++) {
            String[] nums = parts[i].split(",");
            if (nums.length < 3)
                continue;

            try {
                int x = Integer.parseInt(nums[0]);
                int y = Integer.parseInt(nums[1]);
                int z = Integer.parseInt(nums[2]);
                // System.out.println(x + "," + y + "," + z);
                BlockPos pos = new BlockPos(x, y, z);
                map.put(pos.asLong(), new ListMapEntry(pos, true));
            } catch (NumberFormatException e) {

                Tweakeroo.logger.warn("Error while parsing int: " + e.toString());
            }
        }
    }

    public static String getStringFromMap(ConcurrentHashMap<Long, ListMapEntry> map) {

        Iterator<ListMapEntry> iterator = map.values().iterator();

        ArrayList<String> entries = new ArrayList<String>();

        while (iterator.hasNext()) {
            ListMapEntry entry = iterator.next();
            entries.add(entry.originalPosition.getX() + "," + entry.originalPosition.getY() + ","
                    + entry.originalPosition.getZ());
        }
        return String.join("|", entries);
    }

    static class ListMapEntry {
        public final BlockPos originalPosition;
        public BlockPos currentPosition;
        public boolean preserve = false;

        ListMapEntry(BlockPos pos) {
            originalPosition = pos;
            currentPosition = pos;
        }

        ListMapEntry(BlockPos pos, boolean preserve) {
            this(pos);
            this.preserve = preserve;
        }
    }

    static class Selection {
        public BlockPos pos1 = null;
        public BlockPos pos2 = null;
    }

    public static Object containerScanTweakUpdate() {
        // CONTAINERCACHE.clear();
        // CONTAINERS_WAITING.clear();
        if (FeatureToggle.TWEAK_CONTAINER_SCAN.getBooleanValue()) {
            scanContainers();
        }

        return null;
    }

    public static Object scanContainers() {
        MinecraftClient mc = MinecraftClient.getInstance();

        int vd = mc.options.viewDistance;
        ChunkPos cp = mc.player.getChunkPos();
        for (int j = -vd; j < vd; ++j) {
            for (int l = -vd; l < vd; ++l) {
                WorldChunk chunk = (WorldChunk) mc.world.getChunk(cp.x + j, cp.z + l, ChunkStatus.FULL, false);
                if (chunk == null)
                    continue;
                Map<BlockPos, BlockEntity> blockEntities = chunk.getBlockEntities();
                Iterator<BlockEntity> iterator = blockEntities.values().iterator();
                while (iterator.hasNext()) {
                    BlockEntity blockEntity = iterator.next();
                    BlockPos pos = blockEntity.getPos();

                    if (FeatureToggle.TWEAK_AREA_SELECTOR.getBooleanValue() && AREA_SELECTION.pos1 != null
                            && AREA_SELECTION.pos2 != null) {
                        if (!isInSelection(pos)) {
                            continue;
                        }
                    }

                    if (blockEntity instanceof LockableContainerBlockEntity) {
                        if (!CONTAINERCACHE.containsKey(pos.asLong()))
                            CONTAINERCACHE.put(pos.asLong(), new ContainerEntry(pos));
                    }
                }

            }
        }

        return null;
    }

    public static class ContainerEntry {
        public BlockPos pos;
        public int itemCount = 0;
        public int typeCount = 0;
        public ItemStack[] contentList = null;
        public boolean areSlotsCovered = false;
        public boolean isFull = false;
        public int status = 0;
        public boolean skipCount = false;

        ContainerEntry(BlockPos p) {
            pos = p;
        }

    }

    public static void onDesync() {
        CURRENT_CONTAINER = -1;

        for (ContainerEntry entry : CONTAINERS_WAITING) {
            entry.status = 3;
        }
    }

    public static boolean onOpenScreen(Text name, ScreenHandlerType<?> screenHandlerType, int syncId) {
        LAST_CHECK = System.currentTimeMillis();
        if (!FeatureToggle.TWEAK_CONTAINER_SCAN.getBooleanValue())
            return true;
        if (CONTAINERS_WAITING.isEmpty() || CONTAINERS_WAITING.size() <= CURRENT_CONTAINER) {
            // System.out.println("Desync, no containers are being scanned (open screen)");
            onDesync();
            return true;
        }

        CURRENT_SCREEN_TYPE = screenHandlerType;
        CURRENT_SYNC_ID = syncId;
        return false;
    }

    public static boolean onInventory(int syncId, List<ItemStack> contents) {
        LAST_CHECK = System.currentTimeMillis();
        if (!FeatureToggle.TWEAK_CONTAINER_SCAN.getBooleanValue())
            return true;

        if (CONTAINERS_WAITING.isEmpty() || CONTAINERS_WAITING.size() <= CURRENT_CONTAINER) {
            // System.out.println("Desync, no containers are being scanned");
            onDesync();
            return true;
        }

        if (syncId != CURRENT_SYNC_ID) {
            System.out.println(CURRENT_CONTAINER + " Desync, expected " + CURRENT_SYNC_ID + " but got " + syncId);
            onDesync();
            return false;
        }

        if (CURRENT_CONTAINER == -1) {
            System.out.println("Skipping due to desync");
            return false;
        }

        ContainerEntry entry = CONTAINERS_WAITING.get(CURRENT_CONTAINER);

        MinecraftClient mc = MinecraftClient.getInstance();
        BlockState state = mc.world.getBlockState(entry.pos);
        Block block = state.getBlock();

        int start = 0;
        int end = 0;
        boolean valid = false;
        if (CURRENT_SCREEN_TYPE == ScreenHandlerType.GENERIC_9X3) {
            end = 27;
            valid = block instanceof AbstractChestBlock || block instanceof BarrelBlock;
        } else if (CURRENT_SCREEN_TYPE == ScreenHandlerType.GENERIC_9X6) {
            end = 54;
            valid = block instanceof AbstractChestBlock;
        } else if (CURRENT_SCREEN_TYPE == ScreenHandlerType.GENERIC_3X3) {
            end = 9;
            valid = block instanceof DropperBlock || block instanceof DispenserBlock;
        } else if (CURRENT_SCREEN_TYPE == ScreenHandlerType.HOPPER) {
            end = 5;
            valid = block instanceof HopperBlock;
        } else if (CURRENT_SCREEN_TYPE == ScreenHandlerType.FURNACE
                || CURRENT_SCREEN_TYPE == ScreenHandlerType.BLAST_FURNACE
                || CURRENT_SCREEN_TYPE == ScreenHandlerType.SMOKER) {
            end = 3;
            valid = block instanceof AbstractFurnaceBlock;
        } else if (CURRENT_SCREEN_TYPE == ScreenHandlerType.BREWING_STAND) {
            end = 5;
            valid = block instanceof BrewingStandBlock;
        } else if (CURRENT_SCREEN_TYPE == ScreenHandlerType.SHULKER_BOX) {
            end = 27;
            valid = block instanceof ShulkerBoxBlock;
        } else if (CURRENT_SCREEN_TYPE == ScreenHandlerType.CRAFTING) {
            end = 10;
            valid = block instanceof CraftingTableBlock;
        }

        if (!valid) {
            System.out.println(CURRENT_CONTAINER + " Desync, type mismatch. Expected block with "
                    + Registry.SCREEN_HANDLER.getId(CURRENT_SCREEN_TYPE).getPath() + " screen but found a "
                    + Registry.BLOCK.getId(block).getPath());
            onDesync();
            return false;
        }

        HashSet<Item> hasSeenItem = new HashSet<Item>();
        int max = Math.min(contents.size(), end);
        ItemStack[] contentList = new ItemStack[max - start];
        entry.areSlotsCovered = false;
        entry.isFull = false;
        entry.typeCount = 0;
        entry.itemCount = 0;
        entry.contentList = null;
        if (max - start > 0) {
            entry.areSlotsCovered = true;
            entry.isFull = true;
        }
        for (int i = start; i < max; i++) {
            ItemStack stack = contents.get(i);
            if (stack.isEmpty()) {
                entry.areSlotsCovered = false;
                entry.isFull = false;
            } else if (!hasSeenItem.contains(stack.getItem())) {
                hasSeenItem.add(stack.getItem());
                entry.typeCount++;
            }
            entry.itemCount += stack.getCount();
            if (stack.getMaxCount() != stack.getCount())
                entry.isFull = false;

            contentList[i - start] = stack;
        }
        entry.contentList = contentList;
        entry.status = 2;
        CURRENT_CONTAINER++;
        if (CURRENT_CONTAINER >= CONTAINERS_WAITING.size())
            CONTAINERS_WAITING.clear();
        return false;
    }

    public static void setContainerOverlayData(HashMap<Long, ArrayList<Item>> data) {
        CACHED_OVERLAY_DATA = data;
    }

    public static void clearContainerScanCache() {
        CACHED_OVERLAY_DATA.clear();
        ItemList.INSTANCE.clearSelected();
        CONTAINERCACHE.clear();
        scanContainers();
    }

    public static void loadFakeChunk(int x, int z) {
        fakeWorld.getChunkManager().loadChunk(x, z);
    }

    public static void setFakeBlockState(World realWorld, BlockPos pos, BlockState state, BlockEntity be) {
        fakeWorld.setBlockState(pos, state, 0);
        if (be != null) {
            fakeWorld.addBlockEntity(be);
            be.setWorld(realWorld);
        }
    }

    public static void unloadFakeChunk(int x, int z) {
        fakeWorld.getChunkManager().unloadChunk(x, z);
    }

    /*
        This piece of code makes sure that the unused top portion of the screen stays black.
        Normally this is not an issue, but when tweak_standard_aspect_ratio is used with voxelmap,
        it will lead to periodic light flashes in that area, which is super annoying.

        This piece of code is cannabalized from the code that draws framebuffers.
        It took a days work for me to figure this out.
    */
    public static void renderCoverEnd() {
        MinecraftClient mc = MinecraftClient.getInstance();
        Window window = mc.getWindow();
        int yOffset = ((IMixinWindow) (Object) window).getYOffset();
        if (yOffset == 0)
            return;

            
        int x = 0;
        int y = window.getFramebufferHeight();
        int width = window.getFramebufferWidth();
        int height = yOffset;

        endframebuffer.setClearColor(0, 0, 0, 1);
        endframebuffer.clear(false);
        RenderSystem.assertThread(RenderSystem::isOnGameThreadOrInit);
        GlStateManager._viewport(x, y, width, height);

        Matrix4f matrix4f = Matrix4f.projectionMatrix((float) width, (float) (-height), 1000.0F, 3000.0F);
        RenderSystem.setProjectionMatrix(matrix4f);
        float f = (float) width;
        float g = (float) height;
        float h = (float) endframebuffer.viewportWidth / (float) endframebuffer.textureWidth;
        float i = (float) endframebuffer.viewportHeight / (float) endframebuffer.textureHeight;
        Tessellator tessellator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        bufferBuilder.vertex(0.0D, (double) g, 0.0D).texture(0.0F, 0.0F).color(255, 255, 255, 255).next();
        bufferBuilder.vertex((double) f, (double) g, 0.0D).texture(h, 0.0F).color(255, 255, 255, 255).next();
        bufferBuilder.vertex((double) f, 0.0D, 0.0D).texture(h, i).color(255, 255, 255, 255).next();
        bufferBuilder.vertex(0.0D, 0.0D, 0.0D).texture(0.0F, i).color(255, 255, 255, 255).next();
        bufferBuilder.end();
        BufferRenderer.postDraw(bufferBuilder);
    }
}
