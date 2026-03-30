package com.bartekkansy.prism.api.client.render.world;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.ChunkSkyLightSources;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.BlackholeTickAccess;
import net.minecraft.world.ticks.LevelTickAccess;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * A standalone, lightweight virtual world container used to simulate block geometry
 * and connections (like Walls, Fences, Stairs, and Waterlogged blocks) outside a standard level.
 * <p>
 * This class implements {@link LevelAccessor} and {@link LightChunkGetter} to provide a robust,
 * crash-proof context for the Minecraft block rendering engine in GUI screens. By acting as a
 * full level facade, it ensures that complex block logic (like block state updates and level queries)
 * can execute without throwing {@link NullPointerException}s during rendering.
 * </p>
 */
public class PrismVirtualSpace implements LevelAccessor, LightChunkGetter, LightChunk {
    private final Map<BlockPos, BlockState> blocks = new HashMap<>();
    private final LevelLightEngine dummyLightEngine;

    /**
     * Initializes a new Virtual Space with a full-bright ambient lighting engine.
     * Prevents blocks from rendering completely black in UI contexts.
     */
    public PrismVirtualSpace() {
        this.dummyLightEngine = new LevelLightEngine(this, false, false) {
            @Override
            public int getRawBrightness(BlockPos pos, int amount) {
                return 15; // Force max ambient brightness
            }
        };
    }

    /**
     * Retrieves all coordinates currently occupied by a block in this virtual space.
     *
     * @return An unmodifiable view or direct set of all populated block positions.
     */
    public Set<BlockPos> getAllPositions() {
        return blocks.keySet();
    }

    /**
     * Places a block into the virtual space and recursively updates its neighbors.
     * This is crucial for models that rely on neighbor states (e.g., fence posts connecting,
     * or stairs forming corner shapes).
     *
     * @param pos   The world coordinates of the block.
     * @param state The {@link BlockState} to place.
     */
    public void putBlock(BlockPos pos, BlockState state) {
        blocks.put(pos.immutable(), state);
        updateNeighbors(pos);
    }

    /**
     * Removes a block from the virtual space and notifies neighbors so they can retract
     * any extended model parts (like un-connecting a wall block).
     *
     * @param pos The coordinates to clear.
     */
    public void removeBlock(BlockPos pos) {
        blocks.remove(pos);
        updateNeighbors(pos);
    }

    /**
     * Clears all blocks from the virtual space, leaving it entirely empty.
     */
    public void clear() {
        blocks.clear();
    }

    /**
     * Fills a rectangular 3D volume with a specific block state.
     *
     * @param from  The starting corner coordinates.
     * @param to    The ending corner coordinates.
     * @param state The state to fill the volume with.
     */
    public void fill(BlockPos from, BlockPos to, BlockState state) {
        int minX = Math.min(from.getX(), to.getX());
        int minY = Math.min(from.getY(), to.getY());
        int minZ = Math.min(from.getZ(), to.getZ());
        int maxX = Math.max(from.getX(), to.getX());
        int maxY = Math.max(from.getY(), to.getY());
        int maxZ = Math.max(from.getZ(), to.getZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    putBlock(new BlockPos(x, y, z), state);
                }
            }
        }
    }

    /**
     * Calculates the geometric center of all blocks in this space.
     * Useful for automatically centering a camera on a cluster of built blocks in a GUI.
     *
     * @return A {@link Vec3} representing the average positional center.
     */
    public Vec3 getCenter() {
        if (blocks.isEmpty()) return Vec3.ZERO;
        double x = 0, y = 0, z = 0;
        for (BlockPos pos : blocks.keySet()) {
            x += pos.getX();
            y += pos.getY();
            z += pos.getZ();
        }
        return new Vec3(x / blocks.size(), y / blocks.size(), z / blocks.size());
    }

    /**
     * Triggers shape updates for the given position and all adjacent directions.
     */
    private void updateNeighbors(BlockPos pos) {
        refresh(pos);
        for (Direction d : Direction.values()) refresh(pos.relative(d));
    }

    /**
     * Forces a specific block to evaluate its surroundings and update its shape state.
     */
    private void refresh(BlockPos pos) {
        BlockState state = blocks.get(pos);
        if (state == null || state.isAir()) return;

        BlockState updated = state;
        for (Direction d : Direction.values()) {
            BlockPos neighborPos = pos.relative(d);
            BlockState neighborState = getBlockState(neighborPos);
            // Minecraft 1.21.1 standard shape update call
            updated = updated.updateShape(d, neighborState, this, pos, neighborPos);
        }
        blocks.put(pos.immutable(), updated);
    }

    // ===================================================================================
    // LEVEL WRITER & LEVEL ACCESSOR IMPLEMENTATIONS (1.21.1)
    // ===================================================================================

    @Override
    public boolean setBlock(BlockPos pos, BlockState state, int flags, int recursionLeft) {
        putBlock(pos, state);
        return true;
    }

    @Override
    public boolean removeBlock(BlockPos pos, boolean isMoving) {
        removeBlock(pos);
        return true;
    }

    @Override
    public boolean destroyBlock(BlockPos pos, boolean dropBlock, @Nullable Entity entity, int recursionLeft) {
        removeBlock(pos);
        // Delegate block break sound/particles to client if applicable
        if (Minecraft.getInstance().level != null) {
            Minecraft.getInstance().level.levelEvent(2001, pos, Block.getId(getBlockState(pos)));
        }
        return true;
    }

    @Override
    public boolean addFreshEntity(Entity entity) {
        return false; // Virtual spaces do not process ticking entities
    }

    // ===================================================================================
    // BLOCK AND TINT GETTER IMPLEMENTATIONS
    // ===================================================================================

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState());
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        // Allows waterlogged blocks (like stairs/slabs) to query their internal water state properly
        return getBlockState(pos).getFluidState();
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        // Returning null for Block Entities avoids heavy ticking calculations.
        // If BE rendering is required, a dedicated virtual BE tick manager is needed.
        return null;
    }

    @Override
    public float getShade(Direction direction, boolean shaded) {
        if (!shaded) return 1.0f;
        // Replicates vanilla Minecraft 3D directional shading values.
        return switch (direction) {
            case DOWN -> 0.5f;
            case UP -> 1.0f;
            case NORTH, SOUTH -> 0.8f;
            case EAST, WEST -> 0.6f;
        };
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver resolver) {
        // Safe delegation to the active client level to fetch exact Biome grass/water colors.
        if (Minecraft.getInstance().level != null) {
            return resolver.getColor(Minecraft.getInstance().level.getBiome(pos).value(), pos.getX(), pos.getZ());
        }
        return -1; // Default fallback tint
    }

    // ===================================================================================
    // LIGHTING SYSTEM IMPLEMENTATIONS
    // ===================================================================================

    @Override
    public @Nullable LightChunk getChunkForLighting(int x, int z) {
        return this;
    }

    @Override
    public BlockGetter getLevel() {
        return null;
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return dummyLightEngine;
    }

    @Override
    public int getBrightness(LightLayer type, BlockPos pos) {
        return 15;
    }

    @Override
    public int getRawBrightness(BlockPos pos, int amount) {
        return 15;
    }

    @Override
    public void findBlockLightSources(BiConsumer<BlockPos, BlockState> biConsumer) {}

    @Override
    public ChunkSkyLightSources getSkyLightSources() {
        return null;
    }

    // ===================================================================================
    // SOUNDS, PARTICLES, AND EVENTS (1.21.1 Updated)
    // ===================================================================================

    @Override
    public void playSound(@Nullable Player player, BlockPos pos, SoundEvent sound, SoundSource source, float volume, float pitch) {
        // Plays the sound locally in the player's client environment (perfect for UI feedback)
        if (Minecraft.getInstance().level != null) {
            Minecraft.getInstance().level.playSound(player, pos, sound, source, volume, pitch);
        }
    }

    @Override
    public void addParticle(ParticleOptions particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        // Spawns particles directly into the client world if blocks in the UI trigger them
        if (Minecraft.getInstance().level != null) {
            Minecraft.getInstance().level.addParticle(particleData, x, y, z, xSpeed, ySpeed, zSpeed);
        }
    }

    @Override
    public void levelEvent(@Nullable Player player, int type, BlockPos pos, int data) {
        if (Minecraft.getInstance().level != null) {
            Minecraft.getInstance().level.levelEvent(player, type, pos, data);
        }
    }

    @Override
    public void gameEvent(Holder<GameEvent> event, Vec3 position, GameEvent.Context context) {
        // Silently swallow GameEvents (Vibrations, Sculk Sensors) as they aren't needed in UI
    }

    // ===================================================================================
    // LEVEL DATA & REGISTRY DELEGATION
    // ===================================================================================

    @Override
    public RegistryAccess registryAccess() {
        return Minecraft.getInstance().level != null ? Minecraft.getInstance().level.registryAccess() : RegistryAccess.EMPTY;
    }

    @Override
    public FeatureFlagSet enabledFeatures() {
        return Minecraft.getInstance().level != null ? Minecraft.getInstance().level.enabledFeatures() : FeatureFlagSet.of();
    }

    @Override
    public LevelData getLevelData() {
        return Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getLevelData() : null;
    }

    @Override
    public DimensionType dimensionType() {
        return Minecraft.getInstance().level != null ? Minecraft.getInstance().level.dimensionType() : null;
    }

    @Override
    public BiomeManager getBiomeManager() {
        return Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getBiomeManager() : null;
    }

    @Override
    public Holder<Biome> getUncachedNoiseBiome(int x, int y, int z) {
        return Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getUncachedNoiseBiome(x, y, z) : null;
    }

    // ===================================================================================
    // TICKS, ENTITIES, AND STUBS
    // ===================================================================================

    @Override
    public LevelTickAccess<Block> getBlockTicks() {
        return BlackholeTickAccess.emptyLevelList(); // Prevents UI crashes from block tick scheduling
    }

    @Override
    public LevelTickAccess<Fluid> getFluidTicks() {
        return BlackholeTickAccess.emptyLevelList();
    }

    @Override
    public List<Entity> getEntities(@Nullable Entity entity, AABB boundingBox, Predicate<? super Entity> predicate) {
        return Collections.emptyList();
    }

    @Override
    public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> entityTypeTest, AABB boundingBox, Predicate<? super T> predicate) {
        return Collections.emptyList();
    }

    @Override
    public List<Player> players() {
        return Collections.emptyList();
    }

    @Override
    public ChunkSource getChunkSource() {
        return Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getChunkSource() : null;
    }

    @Nullable
    @Override
    public ChunkAccess getChunk(int x, int z, ChunkStatus status, boolean requireChunk) {
        return null;
    }

    @Override
    public int getHeight(Heightmap.Types heightmapType, int x, int z) {
        return 0;
    }

    @Override
    public int getSkyDarken() {
        return 0;
    }

    @Override
    public DifficultyInstance getCurrentDifficultyAt(BlockPos pos) {
        return new DifficultyInstance(Difficulty.NORMAL, 0, 0, 0f);
    }

    @Nullable
    @Override
    public MinecraftServer getServer() {
        return null;
    }

    @Override
    public RandomSource getRandom() {
        return RandomSource.create();
    }

    @Override
    public long dayTime() {
        return 0L;
    }

    @Override
    public long nextSubTickCount() {
        return 0;
    }

    @Override
    public boolean isClientSide() {
        return true;
    }

    @Override
    public int getSeaLevel() {
        return 63;
    }

    @Override
    public int getMinBuildHeight() {
        return -64;
    }

    @Override
    public int getHeight() {
        return 384;
    }

    @Override
    public WorldBorder getWorldBorder() {
        return null;
    }

    @Override
    public boolean isStateAtPosition(BlockPos blockPos, Predicate<BlockState> predicate) {
        return false;
    }

    @Override
    public boolean isFluidAtPosition(BlockPos blockPos, Predicate<FluidState> predicate) {
        return false;
    }
}