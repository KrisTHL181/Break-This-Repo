package net.minecraft.world.level.block.entity;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.GeyserParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PotentSulfurBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.PotentSulfurState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jspecify.annotations.Nullable;

public class PotentSulfurBlockEntity extends BlockEntity {
    private static final int EFFECT_APPLICATION_FREQUENCY_TICKS = 10;
    private static final float EFFECT_DURATION_IN_SECONDS = 4.0F;
    private static final int EFFECT_DURATION_IN_TICKS = 80;
    public static final float EFFECT_RANGE = 3.0F;
    private static final Predicate<Entity> EFFECT_PREDICATE = EntitySelector.NO_SPECTATORS.and(EntitySelector.ENTITY_STILL_ALIVE);
    public static final int PARTICLE_FREQUENCY_TICKS = 20;
    public static final int SOUND_FREQUENCY_TICKS = 40;
    private static final float GEYSER_BASE_LAUNCH_SPEED = 0.3F;
    private static final float GEYSER_LAUNCH_FORCE = 0.2F;
    public int waitingCountdown = -1;
    public long eruptionTick = -1L;
    public static BlockEntityTicker<PotentSulfurBlockEntity> SERVER_NAUSEA_EFFECT_TICKER = (level, pos, state, potentSulfur) -> {
        if (level.getGameTime() % 10L == 0L) {
            BlockPos sourceBlock = findNoxiousGasSourceBlock(level, pos);
            if (sourceBlock != null) {
                for (LivingEntity entity : getNearbyLivingEntities(level, sourceBlock)) {
                    if (canBeReachedByNoxiousGas(level, sourceBlock, entity.getEyePosition())) {
                        applyNauseaEffect(entity);
                    }
                }
            }
        }
    };
    public static BlockEntityTicker<PotentSulfurBlockEntity> CLIENT_NOXIOUS_GAS_TICKER = (level, pos, state, entity) -> {
        if (level.getGameTime() % 20L == 0L) {
            BlockPos sourceBlock = findNoxiousGasSourceBlock(level, pos);
            if (sourceBlock != null) {
                spawnNoxiousGasCloudParticle(level, Vec3.atCenterOf(sourceBlock));
            }
        }
    };
    public static Function<SoundEvent, BlockEntityTicker<PotentSulfurBlockEntity>> CLIENT_GEYSER_PLUME_TICKER = sound -> (level, pos, state, entity) -> {
        BlockPos sourceBlock = findNoxiousGasSourceBlock(level, pos);
        if (sourceBlock != null) {
            long eruptionTime = level.getGameTime() - entity.eruptionTick;
            if (eruptionTime % 20L == 0L) {
                spawnGeyserParticle(level, pos, sourceBlock);
            }

            if (eruptionTime % 40L == 0L) {
                level.playLocalSound(sourceBlock.getX() + 0.5, sourceBlock.getY() + 0.5, sourceBlock.getZ() + 0.5, sound, SoundSource.BLOCKS, 1.0F, 1.0F, false);
            }
        }
    };
    public static BlockEntityTicker<PotentSulfurBlockEntity> SERVER_WAITING_COUNTDOWN_TICKER = (level, pos, state, entity) -> {
        if (level.getGameTime() % 20L == 0L) {
            BlockPos sourceBlock = findNoxiousGasSourceBlock(level, pos);
            if (sourceBlock != null) {
                if (entity.waitingCountdown <= 0) {
                    int waterBlocks = sourceBlock.getY() - pos.getY() - 1;
                    RandomSource geyserPositional = geyserPositional((ServerLevel)level, pos);
                    if (state.getValue(PotentSulfurBlock.STATE) == PotentSulfurState.DORMANT) {
                        entity.waitingCountdown = 10 * (waterBlocks - 1) + geyserPositional.nextIntBetweenInclusive(15, 30);
                    } else {
                        geyserPositional.nextInt();
                        entity.waitingCountdown = waterBlocks - 1 + geyserPositional.nextIntBetweenInclusive(1, 2);
                    }
                }

                if (entity.waitingCountdown > 0) {
                    entity.waitingCountdown--;
                }

                if (entity.waitingCountdown == 0) {
                    PotentSulfurState stateToSet = state.getValue(PotentSulfurBlock.STATE) == PotentSulfurState.DORMANT
                        ? PotentSulfurState.ERUPTING
                        : PotentSulfurState.DORMANT;
                    level.setBlock(pos, state.setValue(PotentSulfurBlock.STATE, stateToSet), 3);
                    if (stateToSet == PotentSulfurState.DORMANT) {
                        level.gameEvent(GameEvent.BLOCK_DEACTIVATE, pos, GameEvent.Context.of(state));
                    }
                }
            }
        }
    };
    public static final long GEYSER_SALT = -904011478L;
    public static BlockEntityTicker<PotentSulfurBlockEntity> LAUNCH_ENTITY_TICKER = (level, pos, state, entity) -> {
        BlockPos sourceBlock = findNoxiousGasSourceBlock(level, pos);
        if (sourceBlock != null) {
            int waterBlocks = sourceBlock.getY() - pos.getY() - 1;
            int geyserForceHeight = getUnobstructedBlockCount(level, pos.above(), waterBlocks);
            AABB aabb = new AABB(pos.above()).expandTowards(0.0, geyserForceHeight - 1, 0.0);

            for (Entity entityToBeLaunched : level.getEntitiesOfClass(Entity.class, aabb, EFFECT_PREDICATE)) {
                Vec3 entityVelocity = entityToBeLaunched.getDeltaMovement();
                entityToBeLaunched.checkFallDistanceAccumulation();
                if (entityToBeLaunched.canSimulateMovement()
                    && !(entityToBeLaunched instanceof Player player && player.getAbilities().flying)
                    && !entityToBeLaunched.isPassenger()
                    && !entityToBeLaunched.is(EntityTypeTags.NOT_AFFECTED_BY_GEYSERS)
                    && entityVelocity.y < 0.3F + waterBlocks * 0.1) {
                    entityToBeLaunched.addDeltaMovement(new Vec3(0.0, 0.2F, 0.0));
                    entityToBeLaunched.needsSync = true;
                }
            }
        }
    };

    public PotentSulfurBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
        super(BlockEntityTypes.POTENT_SULFUR, worldPosition, blockState);
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("countdown", this.waitingCountdown);
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        input.getInt("countdown").ifPresent(value -> this.waitingCountdown = value);
    }

    @Override
    public void setLevel(final Level level) {
        super.setLevel(level);
        if (this.eruptionTick == -1L) {
            this.eruptionTick = level.getGameTime();
        }
    }

    public void resetCountdown() {
        this.waitingCountdown = -1;
    }

    private static void applyNauseaEffect(final LivingEntity entity) {
        entity.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 80, 0, true, true));
    }

    private static List<LivingEntity> getNearbyLivingEntities(final Level level, final BlockPos pos) {
        AABB aabb = new AABB(pos).inflate(2.5, 0.0, 2.5);
        return level.getEntitiesOfClass(LivingEntity.class, aabb, EFFECT_PREDICATE);
    }

    public static RandomSource geyserPositional(final ServerLevel level, final BlockPos pos) {
        return new XoroshiroRandomSource(level.getSeed() ^ -904011478L).forkPositional().at(pos);
    }

    private static void spawnGeyserParticle(final Level level, final BlockPos sulfurPos, final BlockPos sourcePos) {
        int waterBlocks = sourcePos.getY() - sulfurPos.getY() - 1;
        level.addParticle(
            new GeyserParticleOptions(ParticleTypes.GEYSER, waterBlocks), sourcePos.getX() + 0.5, sourcePos.getY(), sourcePos.getZ() + 0.5, 0.0, 0.0, 0.0
        );
    }

    private static void spawnNoxiousGasCloudParticle(final Level level, final Vec3 pos) {
        level.addParticle(ParticleTypes.NOXIOUS_GAS_CLOUD, pos.x, pos.y, pos.z, 0.0, 0.0, 0.0);
    }

    private static int getUnobstructedBlockCount(final Level level, final BlockPos pos, final int waterBlocks) {
        int geyserForceHeight = 6 * waterBlocks;
        CollisionContext geyserPositionContext = CollisionContext.positionContext(pos.below().getY());

        for (int i = 0; i < geyserForceHeight; i++) {
            BlockPos currentPos = pos.above(i);
            BlockState state = level.getBlockState(currentPos);
            if (!isGeyserPassableBlock(state, level, currentPos, geyserPositionContext)) {
                return i;
            }
        }

        return geyserForceHeight;
    }

    private static boolean isGeyserPassableBlock(final BlockState state, final Level level, final BlockPos pos, final CollisionContext context) {
        return !state.isAir() && !state.is(Blocks.WATER) ? state.getCollisionShape(level, pos, context).isEmpty() : true;
    }

    private static @Nullable BlockPos findNoxiousGasSourceBlock(final Level level, final BlockPos origin) {
        int maxY = origin.getY() + 4 + 1;
        CollisionContext geyserPositionContext = CollisionContext.positionContext(origin.getY());
        BlockPos.MutableBlockPos pos = origin.above(1).mutable();

        while (pos.getY() <= maxY) {
            BlockState state = level.getBlockState(pos);
            boolean isWaterLogged = level.getFluidState(pos).isSourceOfType(Fluids.WATER);
            if (!isWaterLogged || !state.is(Blocks.WATER) && !isGeyserPassableBlock(state, level, pos, geyserPositionContext)) {
                if (state.isAir() || isGeyserPassableBlock(state, level, pos, geyserPositionContext)) {
                    return pos.immutable();
                }
                break;
            }

            pos.move(Direction.UP);
        }

        return null;
    }

    public static boolean canBeReachedByNoxiousGas(final Level level, final BlockPos sourceBlock, final Vec3 pos) {
        BlockPos blockPos = BlockPos.containing(pos);
        CollisionContext geyserPositionContext = CollisionContext.positionContext(blockPos.below().getY());
        if (!isGeyserPassableBlock(level.getBlockState(blockPos), level, blockPos, geyserPositionContext)) {
            return false;
        }

        if (pos.distanceToSqr(Vec3.atCenterOf(sourceBlock)) > 9.0) {
            return false;
        }

        Vec3 belowSource = Vec3.atCenterOf(sourceBlock.below());
        Vec3 belowPos = pos.with(Direction.Axis.Y, pos.y - 1.0);
        return isWater(level, belowPos) && haveLineOfSight(level, belowSource, belowPos);
    }

    private static boolean isWater(final Level level, final Vec3 pos) {
        return level.getFluidState(BlockPos.containing(pos)).isSourceOfType(Fluids.WATER);
    }

    private static boolean haveLineOfSight(final Level level, final Vec3 a, final Vec3 b) {
        HitResult hitResult = level.clip(new ClipContext(a, b, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        return hitResult.getType() != HitResult.Type.BLOCK;
    }
}