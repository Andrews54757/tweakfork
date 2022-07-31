package fi.dy.masa.tweakeroo.world;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import fi.dy.masa.tweakeroo.Reference;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.map.MapState;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.DimensionTypes;
import net.minecraft.world.entity.EntityLookup;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.GameEvent.Emitter;
import net.minecraft.world.tick.QueryableTickScheduler;

public class FakeWorld extends World
{
    private static final RegistryKey<World> REGISTRY_KEY = RegistryKey.of(Registry.WORLD_KEY, new Identifier(Reference.MOD_ID, "selective_world"));

    private final MinecraftClient mc;
    private final FakeChunkManager chunkManager;

    private DynamicRegistryManager registryManager;
    public static final RegistryEntry<DimensionType> DIMENSIONTYPE = BuiltinRegistries.DIMENSION_TYPE.entryOf(DimensionTypes.OVERWORLD);
    
    public FakeWorld(DynamicRegistryManager registry, MutableWorldProperties mutableWorldProperties, RegistryEntry<DimensionType> dimensionType, Supplier<Profiler> supplier, int loadDistance)
    {
        //MutableWorldProperties properties, RegistryKey<World> registryRef, RegistryEntry<DimensionType> dimension, Supplier<Profiler> profiler, boolean isClient, boolean debugWorld, long seed, int maxChainedNeighborUpdates
        super(mutableWorldProperties, REGISTRY_KEY, dimensionType, supplier, true, true, 0L, 1000000);
        registryManager = registry;
        this.mc = MinecraftClient.getInstance();
        this.chunkManager = new FakeChunkManager(this, loadDistance);

    }

    public FakeWorld(DynamicRegistryManager registry, int loadDistance) {
        this(registry, new ClientWorld.Properties(Difficulty.PEACEFUL, false, true), DIMENSIONTYPE, MinecraftClient.getInstance()::getProfiler, loadDistance);
    }

    public FakeChunkManager getChunkProvider()
    {
        return this.chunkManager;
    }

    @Override
    public FakeChunkManager getChunkManager()
    {
        return this.chunkManager;
    }

    @Override
    public WorldChunk getWorldChunk(BlockPos pos)
    {
        return this.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }

    @Override
    public FakeChunk getChunk(int chunkX, int chunkZ)
    {
        return this.chunkManager.getChunk(chunkX, chunkZ);
    }

    @Override
    public Chunk getChunk(int chunkX, int chunkZ, ChunkStatus status, boolean required)
    {
        return this.getChunk(chunkX, chunkZ);
    }

    @Override
    public boolean setBlockState(BlockPos pos, BlockState newState, int flags)
    {
        if (pos.getY() < this.getBottomY() || pos.getY() >= this.getTopY())
        {
            return false;
        }
        else
        {
            return this.getChunk(pos.getX() >> 4, pos.getZ() >> 4).setBlockState(pos, newState, false) != null;
        }
    }


    public List<FakeChunk> getChunksWithinBox(Box box)
    {
        final int minX = MathHelper.floor(box.minX / 16.0);
        final int minZ = MathHelper.floor(box.minZ / 16.0);
        final int maxX = MathHelper.floor(box.maxX / 16.0);
        final int maxZ = MathHelper.floor(box.maxZ / 16.0);

        List<FakeChunk> chunks = new ArrayList<>();

        for (int cx = minX; cx <= maxX; ++cx)
        {
            for (int cz = minZ; cz <= maxZ; ++cz)
            {
                FakeChunk chunk = this.chunkManager.getChunkIfExists(cx, cz);

                if (chunk != null)
                {
                    chunks.add(chunk);
                }
            }
        }

        return chunks;
    }

    @Override
    public int getBottomY()
    {
        return this.mc.world != null ? this.mc.world.getBottomY() : -64;
    }

    @Override
    public int getHeight()
    {
        return this.mc.world != null ? this.mc.world.getHeight() : 384;
    }

    // The following HeightLimitView overrides are to work around an incompatibility with Lithium 0.7.4+

    @Override
    public int getTopY()
    {
        return this.getBottomY() + this.getHeight();
    }

    @Override
    public int getBottomSectionCoord()
    {
        return this.getBottomY() >> 4;
    }

    @Override
    public int getTopSectionCoord()
    {
        return this.getTopY() >> 4;
    }

    @Override
    public int countVerticalSections()
    {
        return this.getTopSectionCoord() - this.getBottomSectionCoord();
    }

    @Override
    public boolean isOutOfHeightLimit(BlockPos pos)
    {
        return this.isOutOfHeightLimit(pos.getY());
    }

    @Override
    public boolean isOutOfHeightLimit(int y)
    {
        return (y < this.getBottomY()) || (y >= this.getTopY());
    }

    @Override
    public int getSectionIndex(int y)
    {
        return (y >> 4) - (this.getBottomY() >> 4);
    }

    @Override
    public int sectionCoordToIndex(int coord)
    {
        return coord - (this.getBottomY() >> 4);
    }

    @Override
    public int sectionIndexToCoord(int index)
    {
        return index + (this.getBottomY() >> 4);
    }



    @Override
    public String asString()
    {
        return "Chunks[FAKE] W: " + this.getChunkManager().getDebugString();
    }

    @Override
    public QueryableTickScheduler<Block> getBlockTickScheduler()
    {
        return null;
    }

    @Override
    public QueryableTickScheduler<Fluid> getFluidTickScheduler()
    {
        return null;
    }

    @Override
    public void syncWorldEvent(PlayerEntity player, int eventId, BlockPos pos, int data) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void emitGameEvent(Entity entity, GameEvent event, BlockPos pos) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public DynamicRegistryManager getRegistryManager()
    {
        return registryManager;
    }

    @Override
    public List<? extends PlayerEntity> getPlayers() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public float getBrightness(Direction direction, boolean shaded) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void updateListeners(BlockPos pos, BlockState oldState, BlockState newState, int flags) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void playSound(PlayerEntity except, double x, double y, double z, SoundEvent sound, SoundCategory category,
            float volume, float pitch) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void playSoundFromEntity(PlayerEntity except, Entity entity, SoundEvent sound, SoundCategory category,
            float volume, float pitch) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Entity getEntityById(int id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MapState getMapState(String id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void putMapState(String id, MapState state) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int getNextMapId() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setBlockBreakingInfo(int entityId, BlockPos pos, int progress) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Scoreboard getScoreboard() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RecipeManager getRecipeManager() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected EntityLookup<Entity> getEntityLookup() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RegistryEntry<Biome> getGeneratorStoredBiome(int var1, int var2, int var3) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void emitGameEvent(GameEvent var1, Vec3d var2, Emitter var3) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void playSound(PlayerEntity var1, double var2, double var4, double var6, SoundEvent var8, SoundCategory var9,
            float var10, float var11, long var12) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void playSoundFromEntity(PlayerEntity var1, Entity var2, SoundEvent var3, SoundCategory var4, float var5,
            float var6, long var7) {
        // TODO Auto-generated method stub
        
    }
}