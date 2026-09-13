package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.EndFeatures;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.EndGatewayConfiguration;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class TheEndGatewayBlockEntity extends TheEndPortalBlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SPAWN_TIME = 200;
    private static final int COOLDOWN_TIME = 40;
    private static final int ATTENTION_INTERVAL = 2400;
    private static final int EVENT_COOLDOWN = 1;
    private static final int GATEWAY_HEIGHT_ABOVE_SURFACE = 10;
    private static final long DEFAULT_AGE = 0L;
    private static final boolean DEFAULT_EXACT_TELEPORT = false;
    private long age = 0L;
    private int teleportCooldown;
    private @Nullable BlockPos exitPortal;
    private boolean exactTeleport = false;

    public TheEndGatewayBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
        super(BlockEntityTypes.END_GATEWAY, worldPosition, blockState);
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("Age", this.age);
        output.storeNullable("exit_portal", BlockPos.CODEC, this.exitPortal);
        if (this.exactTeleport) {
            output.putBoolean("ExactTeleport", true);
        }
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        this.age = input.getLongOr("Age", 0L);
        this.exitPortal = input.read("exit_portal", BlockPos.CODEC).filter(Level::isInSpawnableBounds).orElse(null);
        this.exactTeleport = input.getBooleanOr("ExactTeleport", false);
    }

    public static void beamAnimationTick(final Level level, final BlockPos pos, final BlockState state, final TheEndGatewayBlockEntity entity) {
        entity.age++;
        if (entity.isCoolingDown()) {
            entity.teleportCooldown--;
        }
    }

    public static void portalTick(final Level level, final BlockPos pos, final BlockState state, final TheEndGatewayBlockEntity entity) {
        boolean spawning = entity.isSpawning();
        boolean coolingDown = entity.isCoolingDown();
        entity.age++;
        if (coolingDown) {
            entity.teleportCooldown--;
        } else if (entity.age % 2400L == 0L) {
            triggerCooldown(level, pos, state, entity);
        }

        if (spawning != entity.isSpawning() || coolingDown != entity.isCoolingDown()) {
            setChanged(level, pos, state);
        }
    }

    public boolean isSpawning() {
        return this.age < 200L;
    }

    public boolean isCoolingDown() {
        return this.teleportCooldown > 0;
    }

    public float getSpawnPercent(final float a) {
        return Mth.clamp(((float)this.age + a) / 200.0F, 0.0F, 1.0F);
    }

    public float getCooldownPercent(final float a) {
        return 1.0F - Mth.clamp((this.teleportCooldown - a) / 40.0F, 0.0F, 1.0F);
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    public static void triggerCooldown(final Level level, final BlockPos pos, final BlockState blockState, final TheEndGatewayBlockEntity entity) {
        if (!level.isClientSide()) {
            entity.teleportCooldown = 40;
            level.blockEvent(pos, blockState.getBlock(), 1, 0);
            setChanged(level, pos, blockState);
        }
    }

    @Override
    public boolean triggerEvent(final int b0, final int b1) {
        if (b0 == 1) {
            this.teleportCooldown = 40;
            return true;
        } else {
            return super.triggerEvent(b0, b1);
        }
    }

    public @Nullable Vec3 getPortalPosition(final ServerLevel currentLevel, final BlockPos portalEntryPos) {
        if (this.exitPortal == null && currentLevel.dimension() == Level.END) {
            BlockPos exitPortalPos = findOrCreateValidTeleportPos(currentLevel, portalEntryPos);
            exitPortalPos = exitPortalPos.above(10);
            LOGGER.debug("Creating portal at {}", exitPortalPos);
            spawnGatewayPortal(currentLevel, exitPortalPos, EndGatewayConfiguration.knownExit(portalEntryPos, false));
            this.setExitPosition(exitPortalPos, this.exactTeleport);
        }

        if (this.exitPortal != null) {
            BlockPos pos = this.exactTeleport ? this.exitPortal : findExitPosition(currentLevel, this.exitPortal);
            return Vec3.atBottomCenterOf(pos);
        } else {
            return null;
        }
    }

    private static BlockPos findExitPosition(final Level level, final BlockPos exitPortal) {
        BlockPos pos = findTallestBlock(level, exitPortal.offset(0, 2, 0), 5, false);
        LOGGER.debug("Best exit position for portal at {} is {}", exitPortal, pos);
        return pos.above();
    }

    private static BlockPos findOrCreateValidTeleportPos(final ServerLevel level, final BlockPos endGatewayPos) {
        Vec3 exitPortalXZPosTentative = findExitPortalXZPosTentative(level, endGatewayPos);
        LevelChunk exitPortalChunk = getChunk(level, exitPortalXZPosTentative);
        BlockPos exitPortalPos = findValidSpawnInChunk(exitPortalChunk);
        if (exitPortalPos == null) {
            BlockPos newExitPortalPos = BlockPos.containing(exitPortalXZPosTentative.x + 0.5, 75.0, exitPortalXZPosTentative.z + 0.5);
            LOGGER.debug("Failed to find a suitable block to teleport to, spawning an island on {}", newExitPortalPos);
            level.registryAccess()
                .lookup(Registries.CONFIGURED_FEATURE)
                .flatMap(registry -> registry.get(EndFeatures.END_ISLAND))
                .ifPresent(
                    endIsland -> endIsland.value()
                        .place(level, level.getChunkSource().getGenerator(), RandomSource.create((long)newExitPortalPos.hashCode()), newExitPortalPos)
                );
            exitPortalPos = newExitPortalPos;
        } else {
            LOGGER.debug("Found suitable block to teleport to: {}", exitPortalPos);
        }

        return findTallestBlock(level, exitPortalPos, 16, true);
    }

    private static Vec3 findExitPortalXZPosTentative(final ServerLevel level, final BlockPos endGatewayPos) {
        Vec3 teleportXZDirectionVector = new Vec3(endGatewayPos.getX(), 0.0, endGatewayPos.getZ()).normalize();
        int teleportDistance = 1024;
        Vec3 exitPortalXZPosTentative = teleportXZDirectionVector.scale(1024.0);

        for (int chunkLimit = 16;
            !isChunkEmpty(level, exitPortalXZPosTentative) && chunkLimit-- > 0;
            exitPortalXZPosTentative = exitPortalXZPosTentative.add(teleportXZDirectionVector.scale(-16.0))
        ) {
            LOGGER.debug("Skipping backwards past nonempty chunk at {}", exitPortalXZPosTentative);
        }

        for (int var6 = 16;
            isChunkEmpty(level, exitPortalXZPosTentative) && var6-- > 0;
            exitPortalXZPosTentative = exitPortalXZPosTentative.add(teleportXZDirectionVector.scale(16.0))
        ) {
            LOGGER.debug("Skipping forward past empty chunk at {}", exitPortalXZPosTentative);
        }

        LOGGER.debug("Found chunk at {}", exitPortalXZPosTentative);
        return exitPortalXZPosTentative;
    }

    private static boolean isChunkEmpty(final ServerLevel level, final Vec3 xzPos) {
        return getChunk(level, xzPos).getHighestFilledSectionIndex() == -1;
    }

    private static BlockPos findTallestBlock(final BlockGetter level, final BlockPos around, final int dist, final boolean allowBedrock) {
        BlockPos tallest = null;

        for (int xd = -dist; xd <= dist; xd++) {
            for (int zd = -dist; zd <= dist; zd++) {
                if (xd != 0 || zd != 0 || allowBedrock) {
                    for (int y = level.getMaxY(); y > (tallest == null ? level.getMinY() : tallest.getY()); y--) {
                        BlockPos pos = new BlockPos(around.getX() + xd, y, around.getZ() + zd);
                        BlockState state = level.getBlockState(pos);
                        if (state.isCollisionShapeFullBlock(level, pos) && (allowBedrock || !state.is(Blocks.BEDROCK))) {
                            tallest = pos;
                            break;
                        }
                    }
                }
            }
        }

        return tallest == null ? around : tallest;
    }

    private static LevelChunk getChunk(final Level level, final Vec3 pos) {
        return level.getChunk(Mth.floor(pos.x / 16.0), Mth.floor(pos.z / 16.0));
    }

    private static @Nullable BlockPos findValidSpawnInChunk(final LevelChunk chunk) {
        ChunkPos chunkPos = chunk.getPos();
        BlockPos start = new BlockPos((int)chunkPos.getMinBlockX(), 30, (int)chunkPos.getMinBlockZ());
        int maxY = chunk.getHighestSectionPosition() + 16 - 1;
        BlockPos end = new BlockPos((int)chunkPos.getMaxBlockX(), maxY, (int)chunkPos.getMaxBlockZ());
        BlockPos closest = null;
        double closestDist = 0.0;

        for (BlockPos pos : BlockPos.betweenClosed(start, end)) {
            BlockState state = chunk.getBlockState(pos);
            BlockPos above = pos.above();
            BlockPos above2 = pos.above(2);
            if (state.is(Blocks.END_STONE)
                && !chunk.getBlockState(above).isCollisionShapeFullBlock(chunk, above)
                && !chunk.getBlockState(above2).isCollisionShapeFullBlock(chunk, above2)) {
                double dist = pos.distToCenterSqr(0.0, 0.0, 0.0);
                if (closest == null || dist < closestDist) {
                    closest = pos;
                    closestDist = dist;
                }
            }
        }

        return closest;
    }

    private static void spawnGatewayPortal(final ServerLevel level, final BlockPos portalPos, final EndGatewayConfiguration config) {
        Feature.END_GATEWAY.place(config, level, level.getChunkSource().getGenerator(), RandomSource.create(), portalPos);
    }

    @Override
    public boolean shouldRenderFace(final Direction direction) {
        return Block.shouldRenderFace(this.getBlockState(), this.level.getBlockState(this.getBlockPos().relative(direction)), direction);
    }

    public int getParticleAmount() {
        int count = 0;

        for (Direction direction : Direction.values()) {
            count += this.shouldRenderFace(direction) ? 1 : 0;
        }

        return count;
    }

    public void setExitPosition(final BlockPos exactPosition, final boolean exact) {
        this.exactTeleport = exact;
        this.exitPortal = exactPosition;
        this.setChanged();
    }
}