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
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.featuretoggle.FeatureSet;
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
    private static final RegistryKey<World> REGISTRY_KEY = RegistryKey.of(RegistryKeys.WORLD, new Identifier(Reference.MOD_ID, "selective_world"));
    private static final ClientWorld.Properties LEVEL_INFO = new ClientWorld.Properties(Difficulty.PEACEFUL, false, true);
    private static final RegistryEntry<DimensionType> DIMENSION_TYPE = BuiltinRegistries.createWrapperLookup().createRegistryLookup().getOrThrow(RegistryKeys.DIMENSION_TYPE).getOrThrow(DimensionTypes.OVERWORLD);
    
    private final MinecraftClient mc;
    private final FakeChunkManager chunkManager;
    private final DynamicRegistryManager registryManager;

    public FakeWorld(
        DynamicRegistryManager registryManager,
        MutableWorldProperties properties,
        RegistryEntry<DimensionType> dimension,
        Supplier<Profiler> supplier,
        int loadDistance
    )
    {
        //MutableWorldProperties properties, RegistryKey<World> registryRef, RegistryEntry<DimensionType> dimension, Supplier<Profiler> profiler, boolean isClient, boolean debugWorld, long seed, int maxChainedNeighborUpdates
        super(properties, REGISTRY_KEY, registryManager, dimension, supplier, true, false, 0L, 0);
        this.mc = MinecraftClient.getInstance();
        this.registryManager = registryManager;
        this.chunkManager = new FakeChunkManager(this, loadDistance);
    }

    public FakeWorld(DynamicRegistryManager registryManager, int loadDistance) {
        this(registryManager, LEVEL_INFO, DIMENSION_TYPE, MinecraftClient.getInstance()::getProfiler, loadDistance);
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
    public DynamicRegistryManager getRegistryManager() {
        return this.registryManager;
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
    public void syncWorldEvent(PlayerEntity var1, int var2, BlockPos var3, int var4) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void emitGameEvent(GameEvent var1, Vec3d var2, Emitter var3) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public List<? extends PlayerEntity> getPlayers() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RegistryEntry<Biome> getGeneratorStoredBiome(int var1, int var2, int var3) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FeatureSet getEnabledFeatures() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public float getBrightness(Direction var1, boolean var2) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void updateListeners(BlockPos var1, BlockState var2, BlockState var3, int var4) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void playSound(PlayerEntity var1, double var2, double var4, double var6, RegistryEntry<SoundEvent> var8,
            SoundCategory var9, float var10, float var11, long var12) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void playSoundFromEntity(PlayerEntity var1, Entity var2, RegistryEntry<SoundEvent> var3, SoundCategory var4,
            float var5, float var6, long var7) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Entity getEntityById(int var1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MapState getMapState(String var1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void putMapState(String var1, MapState var2) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int getNextMapId() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setBlockBreakingInfo(int var1, BlockPos var2, int var3) {
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
}