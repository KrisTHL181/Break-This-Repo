package net.minecraft.world.entity.animal.golem;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CopperGolemStatueBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.CopperGolemStatueBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class CopperGolem extends AbstractGolem implements ContainerUser, Shearable {
    private static final long IGNORE_WEATHERING_TICK = -2L;
    private static final long UNSET_WEATHERING_TICK = -1L;
    private static final int WEATHERING_TICK_FROM = 504000;
    private static final int WEATHERING_TICK_TO = 552000;
    private static final int SPIN_ANIMATION_MIN_COOLDOWN = 200;
    private static final int SPIN_ANIMATION_MAX_COOLDOWN = 240;
    private static final float SPIN_SOUND_TIME_INTERVAL_OFFSET = 10.0F;
    private static final float TURN_TO_STATUE_CHANCE = 0.0058F;
    private static final int SPAWN_COOLDOWN_MIN = 60;
    private static final int SPAWN_COOLDOWN_MAX = 100;
    private static final Brain.Provider<CopperGolem> BRAIN_PROVIDER = Brain.provider(
        List.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.HURT_BY), var0 -> CopperGolemAi.getActivities()
    );
    private static final EntityDataAccessor<WeatheringCopper.WeatherState> DATA_WEATHER_STATE = SynchedEntityData.defineId(
        CopperGolem.class, EntityDataSerializers.WEATHERING_COPPER_STATE
    );
    private static final EntityDataAccessor<CopperGolemState> COPPER_GOLEM_STATE = SynchedEntityData.defineId(
        CopperGolem.class, EntityDataSerializers.COPPER_GOLEM_STATE
    );
    private @Nullable BlockPos openedChestPos;
    private @Nullable UUID lastLightningBoltUUID;
    private long nextWeatheringTick = -1L;
    private int idleAnimationStartTick = 0;
    private final AnimationState idleAnimationState = new AnimationState();
    private final AnimationState interactionGetItemAnimationState = new AnimationState();
    private final AnimationState interactionGetNoItemAnimationState = new AnimationState();
    private final AnimationState interactionDropItemAnimationState = new AnimationState();
    private final AnimationState interactionDropNoItemAnimationState = new AnimationState();
    public static final EquipmentSlot EQUIPMENT_SLOT_ANTENNA = EquipmentSlot.SADDLE;

    public CopperGolem(final EntityType<? extends AbstractGolem> type, final Level level) {
        super(type, level);
        this.getNavigation().setRequiredPathLength(48.0F);
        this.getNavigation().setCanOpenDoors(true);
        this.setPersistenceRequired();
        this.setState(CopperGolemState.IDLE);
        this.setPathfindingMalus(PathType.FIRE_IN_NEIGHBOR, 16.0F);
        this.setPathfindingMalus(PathType.DAMAGING_IN_NEIGHBOR, 16.0F);
        this.setPathfindingMalus(PathType.FIRE, -1.0F);
        this.getBrain().setMemory(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS, this.getRandom().nextInt(60, 100));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.2F).add(Attributes.STEP_HEIGHT, 1.0).add(Attributes.MAX_HEALTH, 12.0);
    }

    public CopperGolemState getState() {
        return this.entityData.get(COPPER_GOLEM_STATE);
    }

    public void setState(final CopperGolemState state) {
        this.entityData.set(COPPER_GOLEM_STATE, state);
    }

    public WeatheringCopper.WeatherState getWeatherState() {
        return this.entityData.get(DATA_WEATHER_STATE);
    }

    public void setWeatherState(final WeatheringCopper.WeatherState state) {
        this.entityData.set(DATA_WEATHER_STATE, state);
    }

    public void setOpenedChestPos(final BlockPos openedChestPos) {
        this.openedChestPos = openedChestPos;
    }

    public void clearOpenedChestPos() {
        this.openedChestPos = null;
    }

    public AnimationState getIdleAnimationState() {
        return this.idleAnimationState;
    }

    public AnimationState getInteractionGetItemAnimationState() {
        return this.interactionGetItemAnimationState;
    }

    public AnimationState getInteractionGetNoItemAnimationState() {
        return this.interactionGetNoItemAnimationState;
    }

    public AnimationState getInteractionDropItemAnimationState() {
        return this.interactionDropItemAnimationState;
    }

    public AnimationState getInteractionDropNoItemAnimationState() {
        return this.interactionDropNoItemAnimationState;
    }

    @Override
    protected Brain<CopperGolem> makeBrain(final Brain.Packed packedBrain) {
        return BRAIN_PROVIDER.makeBrain(this, packedBrain);
    }

    @Override
    public Brain<CopperGolem> getBrain() {
        return (Brain<CopperGolem>)super.getBrain();
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_WEATHER_STATE, WeatheringCopper.WeatherState.UNAFFECTED);
        entityData.define(COPPER_GOLEM_STATE, CopperGolemState.IDLE);
    }

    @Override
    public void addAdditionalSaveData(final ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putLong("next_weather_age", this.nextWeatheringTick);
        output.store("weather_state", WeatheringCopper.WeatherState.CODEC, this.getWeatherState());
    }

    @Override
    public void readAdditionalSaveData(final ValueInput input) {
        super.readAdditionalSaveData(input);
        this.nextWeatheringTick = input.getLongOr("next_weather_age", -1L);
        this.setWeatherState(input.read("weather_state", WeatheringCopper.WeatherState.CODEC).orElse(WeatheringCopper.WeatherState.UNAFFECTED));
    }

    @Override
    protected void customServerAiStep(final ServerLevel level) {
        ProfilerFiller profiler = Profiler.get();
        profiler.push("copperGolemBrain");
        this.getBrain().tick(level, this);
        profiler.pop();
        profiler.push("copperGolemActivityUpdate");
        CopperGolemAi.updateActivity(this);
        profiler.pop();
        super.customServerAiStep(level);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            if (!this.isNoAi()) {
                this.setupAnimationStates();
            }
        } else {
            this.updateWeathering((ServerLevel)this.level(), this.level().getRandom(), this.level().getGameTime());
        }
    }

    @Override
    public InteractionResult mobInteract(final Player player, final InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (itemStack.isEmpty()) {
            ItemStack equippedItem = this.getMainHandItem();
            if (!equippedItem.isEmpty()) {
                BehaviorUtils.throwItem(this, equippedItem, player.position());
                this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                return InteractionResult.SUCCESS;
            }
        }

        Level level = this.level();
        if (itemStack.is(Items.SHEARS) && this.readyForShearing()) {
            if (level instanceof ServerLevel serverLevel) {
                this.shear(serverLevel, SoundSource.PLAYERS, itemStack);
                this.gameEvent(GameEvent.SHEAR, player);
                itemStack.hurtAndBreak(1, player, hand);
            }

            return InteractionResult.SUCCESS;
        } else {
            if (level.isClientSide()) {
                return InteractionResult.PASS;
            }

            if (itemStack.is(Items.HONEYCOMB) && this.nextWeatheringTick != -2L) {
                level.levelEvent(this, 3003, this.blockPosition(), 0);
                this.nextWeatheringTick = -2L;
                this.usePlayerItem(player, hand, itemStack);
                return InteractionResult.SUCCESS_SERVER;
            }

            if (itemStack.is(ItemTags.AXES) && this.nextWeatheringTick == -2L) {
                level.playSound(null, this, SoundEvents.AXE_SCRAPE, this.getSoundSource(), 1.0F, 1.0F);
                level.levelEvent(this, 3004, this.blockPosition(), 0);
                this.nextWeatheringTick = -1L;
                itemStack.hurtAndBreak(1, player, hand.asEquipmentSlot());
                return InteractionResult.SUCCESS_SERVER;
            }

            if (itemStack.is(ItemTags.AXES)) {
                WeatheringCopper.WeatherState weatherState = this.getWeatherState();
                if (weatherState != WeatheringCopper.WeatherState.UNAFFECTED) {
                    level.playSound(null, this, SoundEvents.AXE_SCRAPE, this.getSoundSource(), 1.0F, 1.0F);
                    level.levelEvent(this, 3005, this.blockPosition(), 0);
                    this.nextWeatheringTick = -1L;
                    this.entityData.set(DATA_WEATHER_STATE, weatherState.previous(), true);
                    itemStack.hurtAndBreak(1, player, hand.asEquipmentSlot());
                    return InteractionResult.SUCCESS_SERVER;
                }
            }

            return super.mobInteract(player, hand);
        }
    }

    private void updateWeathering(final ServerLevel level, final RandomSource random, final long gameTime) {
        if (this.nextWeatheringTick != -2L) {
            if (this.nextWeatheringTick == -1L) {
                this.nextWeatheringTick = gameTime + random.nextIntBetweenInclusive(504000, 552000);
            } else {
                WeatheringCopper.WeatherState weatherState = this.entityData.get(DATA_WEATHER_STATE);
                boolean isFullyOxidized = weatherState.equals(WeatheringCopper.WeatherState.OXIDIZED);
                if (gameTime >= this.nextWeatheringTick && !isFullyOxidized) {
                    WeatheringCopper.WeatherState newState = weatherState.next();
                    boolean isNewStateFullyOxidized = newState.equals(WeatheringCopper.WeatherState.OXIDIZED);
                    this.setWeatherState(newState);
                    this.nextWeatheringTick = isNewStateFullyOxidized ? 0L : this.nextWeatheringTick + random.nextIntBetweenInclusive(504000, 552000);
                }

                if (isFullyOxidized && this.canTurnToStatue(level)) {
                    this.turnToStatue(level);
                }
            }
        }
    }

    private boolean canTurnToStatue(final Level level) {
        return level.getBlockState(this.blockPosition()).isAir() && level.getRandom().nextFloat() <= 0.0058F;
    }

    private void turnToStatue(final ServerLevel level) {
        BlockPos pos = this.blockPosition();
        level.setBlock(
            pos,
            Blocks.COPPER_GOLEM_STATUE
                .weathering()
                .oxidized()
                .defaultBlockState()
                .setValue(
                    CopperGolemStatueBlock.POSE, CopperGolemStatueBlock.Pose.values()[this.random.nextInt(0, CopperGolemStatueBlock.Pose.values().length)]
                )
                .setValue(CopperGolemStatueBlock.FACING, Direction.fromYRot(this.getYRot())),
            3
        );
        if (level.getBlockEntity(pos) instanceof CopperGolemStatueBlockEntity copperGolemStatueBlockEntity) {
            copperGolemStatueBlockEntity.createStatue(this);
            this.dropPreservedEquipment(level);
            this.discard();
            this.playSound(SoundEvents.COPPER_GOLEM_BECOME_STATUE);
            if (this.isLeashed()) {
                if (level.getGameRules().get(GameRules.ENTITY_DROPS)) {
                    this.dropLeash();
                } else {
                    this.removeLeash();
                }
            }
        }
    }

    private void setupAnimationStates() {
        switch (this.getState()) {
            case IDLE:
                this.interactionGetNoItemAnimationState.stop();
                this.interactionGetItemAnimationState.stop();
                this.interactionDropItemAnimationState.stop();
                this.interactionDropNoItemAnimationState.stop();
                if (this.idleAnimationStartTick == this.tickCount) {
                    this.idleAnimationState.start(this.tickCount);
                } else if (this.idleAnimationStartTick == 0) {
                    this.idleAnimationStartTick = this.tickCount + this.random.nextInt(200, 240);
                }

                if (this.tickCount == this.idleAnimationStartTick + 10.0F) {
                    this.playHeadSpinSound();
                    this.idleAnimationStartTick = 0;
                }
                break;
            case GETTING_ITEM:
                this.idleAnimationState.stop();
                this.idleAnimationStartTick = 0;
                this.interactionGetNoItemAnimationState.stop();
                this.interactionDropItemAnimationState.stop();
                this.interactionDropNoItemAnimationState.stop();
                this.interactionGetItemAnimationState.startIfStopped(this.tickCount);
                break;
            case GETTING_NO_ITEM:
                this.idleAnimationState.stop();
                this.idleAnimationStartTick = 0;
                this.interactionGetItemAnimationState.stop();
                this.interactionDropNoItemAnimationState.stop();
                this.interactionDropItemAnimationState.stop();
                this.interactionGetNoItemAnimationState.startIfStopped(this.tickCount);
                break;
            case DROPPING_ITEM:
                this.idleAnimationState.stop();
                this.idleAnimationStartTick = 0;
                this.interactionGetItemAnimationState.stop();
                this.interactionGetNoItemAnimationState.stop();
                this.interactionDropNoItemAnimationState.stop();
                this.interactionDropItemAnimationState.startIfStopped(this.tickCount);
                break;
            case DROPPING_NO_ITEM:
                this.idleAnimationState.stop();
                this.idleAnimationStartTick = 0;
                this.interactionGetItemAnimationState.stop();
                this.interactionGetNoItemAnimationState.stop();
                this.interactionDropItemAnimationState.stop();
                this.interactionDropNoItemAnimationState.startIfStopped(this.tickCount);
        }
    }

    public void spawn(final WeatheringCopper.WeatherState weatherState) {
        this.setWeatherState(weatherState);
        this.playSpawnSound();
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
        final ServerLevelAccessor level, final DifficultyInstance difficulty, final EntitySpawnReason spawnReason, final @Nullable SpawnGroupData groupData
    ) {
        this.playSpawnSound();
        return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
    }

    public void playSpawnSound() {
        this.playSound(SoundEvents.COPPER_GOLEM_SPAWN);
    }

    private void playHeadSpinSound() {
        if (!this.isSilent()) {
            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), this.getSpinHeadSound(), this.getSoundSource(), 1.0F, 1.0F, false);
        }
    }

    @Override
    protected SoundEvent getHurtSound(final DamageSource source) {
        return CopperGolemOxidationLevels.getOxidationLevel(this.getWeatherState()).hurtSound();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return CopperGolemOxidationLevels.getOxidationLevel(this.getWeatherState()).deathSound();
    }

    @Override
    protected void playStepSound(final BlockPos pos, final BlockState blockState) {
        this.playSound(CopperGolemOxidationLevels.getOxidationLevel(this.getWeatherState()).stepSound(), 1.0F, 1.0F);
    }

    private SoundEvent getSpinHeadSound() {
        return CopperGolemOxidationLevels.getOxidationLevel(this.getWeatherState()).spinHeadSound();
    }

    @Override
    public Vec3 getLeashOffset() {
        return new Vec3(0.0, 0.75F * this.getEyeHeight(), 0.0);
    }

    @Override
    public boolean hasContainerOpen(final ContainerOpenersCounter container, final BlockPos blockPos) {
        if (this.openedChestPos == null) {
            return false;
        }

        BlockState blockState = this.level().getBlockState(this.openedChestPos);
        return this.openedChestPos.equals(blockPos)
            || blockState.getBlock() instanceof ChestBlock
                && blockState.getValue(ChestBlock.TYPE) != ChestType.SINGLE
                && ChestBlock.getConnectedBlockPos(this.openedChestPos, blockState).equals(blockPos);
    }

    @Override
    public double getContainerInteractionRange() {
        return 3.0;
    }

    @Override
    public void shear(final ServerLevel level, final SoundSource soundSource, final ItemStack tool) {
        level.playSound(null, this, SoundEvents.COPPER_GOLEM_SHEAR, soundSource, 1.0F, 1.0F);
        ItemStack itemStack = this.getItemBySlot(EQUIPMENT_SLOT_ANTENNA);
        this.setItemSlot(EQUIPMENT_SLOT_ANTENNA, ItemStack.EMPTY);
        this.spawnAtLocation(level, itemStack, 1.5F);
    }

    @Override
    public boolean readyForShearing() {
        return this.getItemBySlot(EQUIPMENT_SLOT_ANTENNA).is(ItemTags.SHEARABLE_FROM_COPPER_GOLEM);
    }

    @Override
    protected void dropEquipment(final ServerLevel level) {
        super.dropEquipment(level);
        this.dropPreservedEquipment(level);
    }

    @Override
    protected void actuallyHurt(final ServerLevel level, final DamageSource source, final float dmg) {
        super.actuallyHurt(level, source, dmg);
        this.setState(CopperGolemState.IDLE);
    }

    @Override
    public void thunderHit(final ServerLevel level, final LightningBolt lightningBolt) {
        super.thunderHit(level, lightningBolt);
        UUID lightningBoltUUID = lightningBolt.getUUID();
        if (!lightningBoltUUID.equals(this.lastLightningBoltUUID)) {
            this.lastLightningBoltUUID = lightningBoltUUID;
            WeatheringCopper.WeatherState weatherState = this.getWeatherState();
            if (weatherState != WeatheringCopper.WeatherState.UNAFFECTED) {
                this.nextWeatheringTick = -1L;
                this.entityData.set(DATA_WEATHER_STATE, weatherState.previous(), true);
            }
        }
    }
}
