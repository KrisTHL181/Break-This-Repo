package net.minecraft.world.entity;

import com.google.common.collect.Sets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class LightningBolt extends Entity {
    private static final int START_LIFE = 2;
    private static final double DAMAGE_RADIUS = 3.0;
    private static final double DETECTION_RADIUS = 15.0;
    private int life;
    public long seed;
    private int flashes;
    private boolean visualOnly;
    private @Nullable ServerPlayer cause;
    private final Set<Entity> hitEntities = Sets.newHashSet();
    private int blocksSetOnFire;

    public LightningBolt(final EntityType<? extends LightningBolt> type, final Level level) {
        super(type, level);
        this.life = 2;
        this.seed = this.random.nextLong();
        this.flashes = this.random.nextInt(3) + 1;
    }

    public void setVisualOnly(final boolean visualOnly) {
        this.visualOnly = visualOnly;
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.WEATHER;
    }

    public @Nullable ServerPlayer getCause() {
        return this.cause;
    }

    public void setCause(final @Nullable ServerPlayer cause) {
        this.cause = cause;
    }

    private void powerLightningRod() {
        BlockPos strikePosition = this.getStrikePosition();
        BlockState stateBelow = this.level().getBlockState(strikePosition);
        if (stateBelow.getBlock() instanceof LightningRodBlock lightningRodBlock) {
            lightningRodBlock.onLightningStrike(stateBelow, this.level(), strikePosition);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.life == 2) {
            if (this.level().isClientSide()) {
                this.level()
                    .playLocalSound(
                        this.getX(),
                        this.getY(),
                        this.getZ(),
                        SoundEvents.LIGHTNING_BOLT_THUNDER,
                        SoundSource.WEATHER,
                        10000.0F,
                        0.8F + this.random.nextFloat() * 0.2F,
                        false
                    );
                this.level()
                    .playLocalSound(
                        this.getX(),
                        this.getY(),
                        this.getZ(),
                        SoundEvents.LIGHTNING_BOLT_IMPACT,
                        SoundSource.WEATHER,
                        2.0F,
                        0.5F + this.random.nextFloat() * 0.2F,
                        false
                    );
            } else {
                Difficulty difficulty = this.level().getDifficulty();
                if (difficulty == Difficulty.NORMAL || difficulty == Difficulty.HARD) {
                    this.spawnFire(4);
                }

                this.powerLightningRod();
                clearCopperOnLightningStrike(this.level(), this.getStrikePosition());
                this.gameEvent(GameEvent.LIGHTNING_STRIKE);
            }
        }

        this.life--;
        if (this.life < 0) {
            if (this.flashes == 0) {
                if (this.level() instanceof ServerLevel) {
                    List<Entity> viewers = this.level()
                        .getEntities(
                            this,
                            new AABB(
                                this.getX() - 15.0, this.getY() - 15.0, this.getZ() - 15.0, this.getX() + 15.0, this.getY() + 6.0 + 15.0, this.getZ() + 15.0
                            ),
                            entityx -> entityx.isAlive() && !this.hitEntities.contains(entityx)
                        );

                    for (ServerPlayer player : ((ServerLevel)this.level()).getPlayers(playerx -> playerx.distanceTo(this) < 256.0F)) {
                        CriteriaTriggers.LIGHTNING_STRIKE.trigger(player, this, viewers);
                    }
                }

                this.discard();
            } else if (this.life < -this.random.nextInt(10)) {
                this.flashes--;
                this.life = 1;
                this.seed = this.random.nextLong();
                this.spawnFire(0);
            }
        }

        if (this.life >= 0) {
            if (!(this.level() instanceof ServerLevel)) {
                this.level().setSkyFlashTime(2);
            } else if (!this.visualOnly) {
                List<Entity> entities = this.level()
                    .getEntities(
                        this,
                        new AABB(this.getX() - 3.0, this.getY() - 3.0, this.getZ() - 3.0, this.getX() + 3.0, this.getY() + 6.0 + 3.0, this.getZ() + 3.0),
                        Entity::isAlive
                    );

                for (Entity entity : entities) {
                    entity.thunderHit((ServerLevel)this.level(), this);
                }

                this.hitEntities.addAll(entities);
                if (this.cause != null) {
                    CriteriaTriggers.CHANNELED_LIGHTNING.trigger(this.cause, entities);
                }
            }
        }
    }

    private BlockPos getStrikePosition() {
        Vec3 position = this.position();
        return BlockPos.containing(position.x, position.y - 1.0E-6, position.z);
    }

    private void spawnFire(final int additionalSources) {
        if (!this.visualOnly && this.level() instanceof ServerLevel level) {
            BlockPos var7 = this.blockPosition();
            if (level.canSpreadFireAround(var7)) {
                BlockState fire = BaseFireBlock.getState(level, var7);
                if (level.getBlockState(var7).isAir() && fire.canSurvive(level, var7)) {
                    level.setBlockAndUpdate(var7, fire);
                    this.blocksSetOnFire++;
                }

                for (int i = 0; i < additionalSources; i++) {
                    BlockPos nearbyPos = var7.offset(this.random.nextInt(3) - 1, this.random.nextInt(3) - 1, this.random.nextInt(3) - 1);
                    fire = BaseFireBlock.getState(level, nearbyPos);
                    if (level.getBlockState(nearbyPos).isAir() && fire.canSurvive(level, nearbyPos)) {
                        level.setBlockAndUpdate(nearbyPos, fire);
                        this.blocksSetOnFire++;
                    }
                }
            }
        }
    }

    private static void clearCopperOnLightningStrike(final Level level, final BlockPos struckPos) {
        BlockState struckState = level.getBlockState(struckPos);
        boolean isWaxed = HoneycombItem.WAX_OFF_BY_BLOCK.get().get(struckState.getBlock()) != null;
        boolean isWeatheringCopper = struckState.getBlock() instanceof WeatheringCopper;
        if (isWeatheringCopper || isWaxed) {
            if (isWeatheringCopper) {
                level.setBlockAndUpdate(struckPos, WeatheringCopper.getFirst(level.getBlockState(struckPos)));
            }

            BlockPos.MutableBlockPos workPos = struckPos.mutable();
            RandomSource random = level.getRandom();
            int strikesCount = random.nextInt(3) + 3;

            for (int strike = 0; strike < strikesCount; strike++) {
                int stepCount = random.nextInt(8) + 1;
                randomWalkCleaningCopper(level, struckPos, workPos, stepCount);
            }
        }
    }

    private static void randomWalkCleaningCopper(
        final Level level, final BlockPos originalStrikePos, final BlockPos.MutableBlockPos workPos, final int stepCount
    ) {
        workPos.set(originalStrikePos);

        for (int step = 0; step < stepCount; step++) {
            Optional<BlockPos> stepPos = randomStepCleaningCopper(level, workPos);
            if (stepPos.isEmpty()) {
                break;
            }

            workPos.set(stepPos.get());
        }
    }

    private static Optional<BlockPos> randomStepCleaningCopper(final Level level, final BlockPos pos) {
        for (BlockPos candidate : BlockPos.randomInCube(level.getRandom(), 10, pos, 1)) {
            BlockState state = level.getBlockState(candidate);
            if (state.getBlock() instanceof WeatheringCopper) {
                WeatheringCopper.getPrevious(state).ifPresent(s -> level.setBlockAndUpdate(candidate, s));
                level.levelEvent(3002, candidate, -1);
                return Optional.of(candidate);
            }
        }

        return Optional.empty();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(final double distance) {
        double size = 64.0 * getViewScale();
        return distance < size * size;
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
    }

    @Override
    protected void readAdditionalSaveData(final ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(final ValueOutput output) {
    }

    public int getBlocksSetOnFire() {
        return this.blocksSetOnFire;
    }

    public Stream<Entity> getHitEntities() {
        return this.hitEntities.stream().filter(Entity::isAlive);
    }

    @Override
    public final boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
        return false;
    }
}