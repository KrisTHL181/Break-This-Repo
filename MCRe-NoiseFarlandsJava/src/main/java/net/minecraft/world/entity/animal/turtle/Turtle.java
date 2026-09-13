package net.minecraft.world.entity.animal.turtle;

import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Turtle extends Animal {
    private static final EntityDataAccessor<Boolean> HAS_EGG = SynchedEntityData.defineId(Turtle.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> LAYING_EGG = SynchedEntityData.defineId(Turtle.class, EntityDataSerializers.BOOLEAN);
    private static final float BABY_SCALE = 0.3F;
    private static final EntityDimensions BABY_DIMENSIONS = EntityTypes.TURTLE
        .getDimensions()
        .withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0.0F, EntityTypes.TURTLE.getHeight(), -0.25F))
        .scale(0.3F);
    private static final boolean DEFAULT_HAS_EGG = false;
    private int layEggCounter;
    public static final TargetingConditions.Selector BABY_ON_LAND_SELECTOR = (target, level) -> target.isBaby() && !target.isInWater();
    private BlockPos homePos = BlockPos.ZERO;
    private @Nullable BlockPos travelPos;
    private boolean goingHome;

    public Turtle(final EntityType<? extends Turtle> type, final Level level) {
        super(type, level);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
        this.setPathfindingMalus(PathType.DOOR_IRON_CLOSED, -1.0F);
        this.setPathfindingMalus(PathType.DOOR_WOOD_CLOSED, -1.0F);
        this.setPathfindingMalus(PathType.DOOR_OPEN, -1.0F);
        this.moveControl = new Turtle.TurtleMoveControl<>(this);
    }

    public void setHomePos(final BlockPos pos) {
        this.homePos = pos;
    }

    public BlockPos getHomePos() {
        return this.homePos;
    }

    public boolean hasEgg() {
        return this.entityData.get(HAS_EGG);
    }

    private void setHasEgg(final boolean onOff) {
        this.entityData.set(HAS_EGG, onOff);
    }

    public boolean isLayingEgg() {
        return this.entityData.get(LAYING_EGG);
    }

    private void setLayingEgg(final boolean on) {
        this.layEggCounter = on ? 1 : 0;
        this.entityData.set(LAYING_EGG, on);
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(HAS_EGG, false);
        entityData.define(LAYING_EGG, false);
    }

    @Override
    protected void addAdditionalSaveData(final ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("home_pos", BlockPos.CODEC, this.homePos);
        output.putBoolean("has_egg", this.hasEgg());
    }

    @Override
    protected void readAdditionalSaveData(final ValueInput input) {
        this.setHomePos(input.read("home_pos", BlockPos.CODEC).orElse(this.blockPosition()));
        super.readAdditionalSaveData(input);
        this.setHasEgg(input.getBooleanOr("has_egg", false));
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
        final ServerLevelAccessor level, final DifficultyInstance difficulty, final EntitySpawnReason spawnReason, final @Nullable SpawnGroupData groupData
    ) {
        this.setHomePos(this.blockPosition());
        return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
    }

    public static boolean checkTurtleSpawnRules(
        final EntityType<Turtle> type, final LevelAccessor level, final EntitySpawnReason spawnReason, final BlockPos pos, final RandomSource random
    ) {
        return pos.getY() < level.getSeaLevel() + 4 && TurtleEggBlock.onSand(level, pos) && isBrightEnoughToSpawn(level, pos);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new Turtle.TurtlePanicGoal(this, 1.2));
        this.goalSelector.addGoal(1, new Turtle.TurtleBreedGoal(this, 1.0));
        this.goalSelector.addGoal(1, new Turtle.TurtleLayEggGoal(this, 1.0));
        this.goalSelector.addGoal(2, new TemptGoal(this, 1.1, i -> i.is(ItemTags.TURTLE_FOOD), false));
        this.goalSelector.addGoal(3, new Turtle.TurtleGoToWaterGoal(this, 1.0));
        this.goalSelector.addGoal(4, new Turtle.TurtleGoHomeGoal(this, 1.0));
        this.goalSelector.addGoal(7, new Turtle.TurtleTravelGoal(this, 1.0));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new Turtle.TurtleRandomStrollGoal(this, 1.0, 100));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return !this.isInWater() && this.onGround() && !this.isBaby() ? SoundEvents.TURTLE_AMBIENT_LAND : super.getAmbientSound();
    }

    @Override
    protected void playSwimSound(final float volume) {
        super.playSwimSound(volume * 1.5F);
    }

    @Override
    protected SoundEvent getSwimSound() {
        return SoundEvents.TURTLE_SWIM;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(final DamageSource source) {
        return this.isBaby() ? SoundEvents.TURTLE_HURT_BABY : SoundEvents.TURTLE_HURT;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return this.isBaby() ? SoundEvents.TURTLE_DEATH_BABY : SoundEvents.TURTLE_DEATH;
    }

    @Override
    protected void playStepSound(final BlockPos pos, final BlockState blockState) {
        SoundEvent sound = this.isBaby() ? SoundEvents.TURTLE_SHAMBLE_BABY : SoundEvents.TURTLE_SHAMBLE;
        this.playSound(sound, 0.15F, 1.0F);
    }

    @Override
    public boolean canFallInLove() {
        return super.canFallInLove() && !this.hasEgg();
    }

    @Override
    protected float nextStep() {
        return this.moveDist + 0.15F;
    }

    @Override
    public float getAgeScale() {
        return this.isBaby() ? 0.3F : 1.0F;
    }

    @Override
    protected PathNavigation createNavigation(final Level level) {
        return new Turtle.TurtlePathNavigation(this, level);
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(final ServerLevel level, final AgeableMob partner) {
        return EntityTypes.TURTLE.create(level, EntitySpawnReason.BREEDING);
    }

    @Override
    public boolean isFood(final ItemStack itemStack) {
        return itemStack.is(ItemTags.TURTLE_FOOD);
    }

    @Override
    public float getWalkTargetValue(final BlockPos pos, final LevelReader level) {
        if (!this.goingHome && level.getFluidState(pos).is(FluidTags.WATER)) {
            return 10.0F;
        } else {
            return TurtleEggBlock.onSand(level, pos) ? 10.0F : level.getPathfindingCostFromLightLevels(pos);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.isAlive() && this.isLayingEgg() && this.layEggCounter >= 1 && this.layEggCounter % 5 == 0) {
            BlockPos pos = this.blockPosition();
            if (TurtleEggBlock.onSand(this.level(), pos)) {
                this.level().levelEvent(2001, pos, Block.getId(this.level().getBlockState(pos.below())));
                this.gameEvent(GameEvent.ENTITY_ACTION);
            }
        }
    }

    @Override
    protected void ageBoundaryReached() {
        super.ageBoundaryReached();
        if (!this.isBaby() && this.level() instanceof ServerLevel level && level.getGameRules().get(GameRules.MOB_DROPS)) {
            this.dropFromGiftLootTable(level, BuiltInLootTables.TURTLE_GROW, this::spawnAtLocation);
        }
    }

    @Override
    protected void travelInWater(final Vec3 input, final double baseGravity, final boolean isFalling, final double oldY) {
        this.moveRelative(0.1F, input);
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
        if (this.getTarget() == null && (!this.goingHome || !this.homePos.closerToCenterThan(this.position(), 20.0))) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.005, 0.0));
        }
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public void thunderHit(final ServerLevel level, final LightningBolt lightningBolt) {
        this.hurtServer(level, this.damageSources().lightningBolt(), Float.MAX_VALUE);
    }

    @Override
    public EntityDimensions getDefaultDimensions(final Pose pose) {
        return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(pose);
    }

    private static class TurtleBreedGoal extends BreedGoal {
        private final Turtle turtle;

        public TurtleBreedGoal(final Turtle turtle, final double speedModifier) {
            super(turtle, speedModifier);
            this.turtle = turtle;
        }

        @Override
        public boolean canUse() {
            return super.canUse() && !this.turtle.hasEgg();
        }

        @Override
        protected void breed() {
            ServerPlayer loveCause = this.animal.getLoveCause();
            if (loveCause == null && this.partner.getLoveCause() != null) {
                loveCause = this.partner.getLoveCause();
            }

            if (loveCause != null) {
                loveCause.awardStat(Stats.ANIMALS_BRED);
                CriteriaTriggers.BRED_ANIMALS.trigger(loveCause, this.animal, this.partner, null);
            }

            this.turtle.setHasEgg(true);
            this.animal.setAge(6000);
            this.partner.setAge(6000);
            this.animal.resetLove();
            this.partner.resetLove();
            RandomSource random = this.animal.getRandom();
            if (getServerLevel(this.level).getGameRules().get(GameRules.MOB_DROPS)) {
                this.level.addFreshEntity(new ExperienceOrb(this.level, this.animal.getX(), this.animal.getY(), this.animal.getZ(), random.nextInt(7) + 1));
            }
        }
    }

    private static class TurtleGoHomeGoal extends Goal {
        private final Turtle turtle;
        private final double speedModifier;
        private boolean stuck;
        private int closeToHomeTryTicks;
        private static final int GIVE_UP_TICKS = 600;

        public TurtleGoHomeGoal(final Turtle turtle, final double speedModifier) {
            this.turtle = turtle;
            this.speedModifier = speedModifier;
        }

        @Override
        public boolean canUse() {
            if (this.turtle.isBaby()) {
                return false;
            } else if (this.turtle.hasEgg()) {
                return true;
            } else {
                return this.turtle.getRandom().nextInt(reducedTickDelay(700)) != 0
                    ? false
                    : !this.turtle.homePos.closerToCenterThan(this.turtle.position(), 64.0);
            }
        }

        @Override
        public void start() {
            this.turtle.goingHome = true;
            this.stuck = false;
            this.closeToHomeTryTicks = 0;
        }

        @Override
        public void stop() {
            this.turtle.goingHome = false;
        }

        @Override
        public boolean canContinueToUse() {
            return !this.turtle.homePos.closerToCenterThan(this.turtle.position(), 7.0)
                && !this.stuck
                && this.closeToHomeTryTicks <= this.adjustedTickDelay(600);
        }

        @Override
        public void tick() {
            BlockPos homePos = this.turtle.homePos;
            boolean closeToHome = homePos.closerToCenterThan(this.turtle.position(), 16.0);
            if (closeToHome) {
                this.closeToHomeTryTicks++;
            }

            if (this.turtle.getNavigation().isDone()) {
                Vec3 homePosVec = Vec3.atBottomCenterOf(homePos);
                Vec3 nextPos = DefaultRandomPos.getPosTowards(this.turtle, 16, 3, homePosVec, (float) (Math.PI / 10));
                if (nextPos == null) {
                    nextPos = DefaultRandomPos.getPosTowards(this.turtle, 8, 7, homePosVec, (float) (Math.PI / 2));
                }

                if (nextPos != null && !closeToHome && !this.turtle.level().getBlockState(BlockPos.containing(nextPos)).is(Blocks.WATER)) {
                    nextPos = DefaultRandomPos.getPosTowards(this.turtle, 16, 5, homePosVec, (float) (Math.PI / 2));
                }

                if (nextPos == null) {
                    this.stuck = true;
                    return;
                }

                this.turtle.getNavigation().moveTo(nextPos.x, nextPos.y, nextPos.z, this.speedModifier);
            }
        }
    }

    private static class TurtleGoToWaterGoal extends MoveToBlockGoal {
        private static final int GIVE_UP_TICKS = 1200;
        private final Turtle turtle;

        private TurtleGoToWaterGoal(final Turtle turtle, final double speedModifier) {
            super(turtle, turtle.isBaby() ? 2.0 : speedModifier, 24);
            this.turtle = turtle;
            this.verticalSearchStart = -1;
        }

        @Override
        public boolean canContinueToUse() {
            return !this.turtle.isInWater() && this.tryTicks <= 1200 && this.isValidTarget(this.turtle.level(), this.blockPos);
        }

        @Override
        public boolean canUse() {
            if (this.turtle.isBaby() && !this.turtle.isInWater()) {
                return super.canUse();
            } else {
                return !this.turtle.goingHome && !this.turtle.isInWater() && !this.turtle.hasEgg() ? super.canUse() : false;
            }
        }

        @Override
        public boolean shouldRecalculatePath() {
            return this.tryTicks % 160 == 0;
        }

        @Override
        protected boolean isValidTarget(final LevelReader level, final BlockPos pos) {
            return level.getBlockState(pos).is(Blocks.WATER);
        }
    }

    private static class TurtleLayEggGoal extends MoveToBlockGoal {
        private final Turtle turtle;

        public TurtleLayEggGoal(final Turtle turtle, final double speedModifier) {
            super(turtle, speedModifier, 16);
            this.turtle = turtle;
        }

        @Override
        public boolean canUse() {
            return this.turtle.hasEgg() && this.turtle.homePos.closerToCenterThan(this.turtle.position(), 9.0) ? super.canUse() : false;
        }

        @Override
        public boolean canContinueToUse() {
            return super.canContinueToUse() && this.turtle.hasEgg() && this.turtle.homePos.closerToCenterThan(this.turtle.position(), 9.0);
        }

        @Override
        public void tick() {
            super.tick();
            BlockPos turtlePos = this.turtle.blockPosition();
            if (!this.turtle.isInWater() && this.isReachedTarget()) {
                if (this.turtle.layEggCounter < 1) {
                    this.turtle.setLayingEgg(true);
                } else if (this.turtle.layEggCounter > this.adjustedTickDelay(200)) {
                    Level level = this.turtle.level();
                    level.playSound(null, turtlePos, SoundEvents.TURTLE_LAY_EGG, SoundSource.BLOCKS, 0.3F, 0.9F + level.getRandom().nextFloat() * 0.2F);
                    BlockPos eggPos = this.blockPos.above();
                    BlockState eggState = Blocks.TURTLE_EGG.defaultBlockState().setValue(TurtleEggBlock.EGGS, this.turtle.random.nextInt(4) + 1);
                    level.setBlock(eggPos, eggState, 3);
                    level.gameEvent(GameEvent.BLOCK_PLACE, eggPos, GameEvent.Context.of(this.turtle, eggState));
                    this.turtle.setHasEgg(false);
                    this.turtle.setLayingEgg(false);
                    this.turtle.setInLoveTime(600);
                }

                if (this.turtle.isLayingEgg()) {
                    this.turtle.layEggCounter++;
                }
            }
        }

        @Override
        protected boolean isValidTarget(final LevelReader level, final BlockPos pos) {
            return !level.isEmptyBlock(pos.above()) ? false : TurtleEggBlock.isSand(level, pos);
        }
    }

    private static class TurtleMoveControl<T extends Turtle> extends MoveControl<T> {
        public TurtleMoveControl(final T turtle) {
            super(turtle);
        }

        private void updateSpeed() {
            if (this.mob.isInWater()) {
                this.mob.setDeltaMovement(this.mob.getDeltaMovement().add(0.0, 0.005, 0.0));
                if (!this.mob.getHomePos().closerToCenterThan(this.mob.position(), 16.0)) {
                    this.mob.setSpeed(Math.max(this.mob.getSpeed() / 2.0F, 0.08F));
                }

                if (this.mob.isBaby()) {
                    this.mob.setSpeed(Math.max(this.mob.getSpeed() / 3.0F, 0.06F));
                }
            } else if (this.mob.onGround()) {
                this.mob.setSpeed(Math.max(this.mob.getSpeed() / 2.0F, 0.06F));
            }
        }

        @Override
        public void tick() {
            this.updateSpeed();
            if (this.operation == MoveControl.Operation.MOVE_TO && !this.mob.getNavigation().isDone()) {
                double xd = this.wantedX - this.mob.getX();
                double yd = this.wantedY - this.mob.getY();
                double zd = this.wantedZ - this.mob.getZ();
                double dd = Math.sqrt(xd * xd + yd * yd + zd * zd);
                if (dd < 1.0E-5F) {
                    this.mob.setSpeed(0.0F);
                } else {
                    yd /= dd;
                    float yRotD = (float)(Mth.atan2(zd, xd) * 180.0F / (float)Math.PI) - 90.0F;
                    this.mob.setYRot(this.rotlerp(this.mob.getYRot(), yRotD, 90.0F));
                    this.mob.yBodyRot = this.mob.getYRot();
                    float targetSpeed = (float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
                    this.mob.setSpeed(Mth.lerp(0.125F, this.mob.getSpeed(), targetSpeed));
                    this.mob.setDeltaMovement(this.mob.getDeltaMovement().add(0.0, this.mob.getSpeed() * yd * 0.1, 0.0));
                }
            } else {
                this.mob.setSpeed(0.0F);
            }
        }
    }

    private static class TurtlePanicGoal extends PanicGoal {
        public TurtlePanicGoal(final Turtle turtle, final double speedModifier) {
            super(turtle, speedModifier);
        }

        @Override
        public boolean canUse() {
            if (!this.shouldPanic()) {
                return false;
            } else {
                BlockPos blockPos = this.lookForWater(this.mob.level(), this.mob, 7);
                if (blockPos != null) {
                    this.posX = blockPos.getX();
                    this.posY = blockPos.getY();
                    this.posZ = blockPos.getZ();
                    return true;
                } else {
                    return this.findRandomPosition();
                }
            }
        }
    }

    private static class TurtlePathNavigation extends AmphibiousPathNavigation {
        public TurtlePathNavigation(final Turtle mob, final Level level) {
            super(mob, level);
        }

        @Override
        public boolean isStableDestination(final BlockPos pos) {
            return this.mob instanceof Turtle turtle && turtle.travelPos != null
                ? this.level.getBlockState(pos).is(Blocks.WATER)
                : !this.level.getBlockState(pos.below()).isAir();
        }
    }

    private static class TurtleRandomStrollGoal extends RandomStrollGoal {
        private final Turtle turtle;

        private TurtleRandomStrollGoal(final Turtle turtle, final double speedModifier, final int interval) {
            super(turtle, speedModifier, interval);
            this.turtle = turtle;
        }

        @Override
        public boolean canUse() {
            return !this.mob.isInWater() && !this.turtle.goingHome && !this.turtle.hasEgg() ? super.canUse() : false;
        }
    }

    private static class TurtleTravelGoal extends Goal {
        private final Turtle turtle;
        private final double speedModifier;
        private boolean stuck;

        public TurtleTravelGoal(final Turtle turtle, final double speedModifier) {
            this.turtle = turtle;
            this.speedModifier = speedModifier;
        }

        @Override
        public boolean canUse() {
            return !this.turtle.goingHome && !this.turtle.hasEgg() && this.turtle.isInWater();
        }

        @Override
        public void start() {
            int xzDist = 512;
            int yDist = 4;
            RandomSource random = this.turtle.random;
            int xt = random.nextInt(1025) - 512;
            int yt = random.nextInt(9) - 4;
            int zt = random.nextInt(1025) - 512;
            if (yt + this.turtle.getY() > this.turtle.level().getSeaLevel() - 1) {
                yt = 0;
            }

            this.turtle.travelPos = BlockPos.containing(xt + this.turtle.getX(), yt + this.turtle.getY(), zt + this.turtle.getZ());
            this.stuck = false;
        }

        @Override
        public void tick() {
            if (this.turtle.travelPos == null) {
                this.stuck = true;
            } else {
                if (this.turtle.getNavigation().isDone()) {
                    Vec3 targetPos = Vec3.atBottomCenterOf(this.turtle.travelPos);
                    Vec3 nextPos = DefaultRandomPos.getPosTowards(this.turtle, 16, 3, targetPos, (float) (Math.PI / 10));
                    if (nextPos == null) {
                        nextPos = DefaultRandomPos.getPosTowards(this.turtle, 8, 7, targetPos, (float) (Math.PI / 2));
                    }

                    if (nextPos != null) {
                        int xc = Mth.floor(nextPos.x);
                        int zc = Mth.floor(nextPos.z);
                        int r = 34;
                        if (!this.turtle.level().hasChunksAt(xc - 34, zc - 34, xc + 34, zc + 34)) {
                            nextPos = null;
                        }
                    }

                    if (nextPos == null) {
                        this.stuck = true;
                        return;
                    }

                    this.turtle.getNavigation().moveTo(nextPos.x, nextPos.y, nextPos.z, this.speedModifier);
                }
            }
        }

        @Override
        public boolean canContinueToUse() {
            return !this.turtle.getNavigation().isDone() && !this.stuck && !this.turtle.goingHome && !this.turtle.isInLove() && !this.turtle.hasEgg();
        }

        @Override
        public void stop() {
            this.turtle.travelPos = null;
            super.stop();
        }
    }
}