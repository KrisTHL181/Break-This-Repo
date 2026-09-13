package net.minecraft.world.entity.animal.feline;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.CatLieOnBedGoal;
import net.minecraft.world.entity.ai.goal.CatSitOnBlockGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OcelotAttackGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NonTameRandomTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.variant.SpawnContext;
import net.minecraft.world.entity.variant.VariantUtils;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class Cat extends TamableAnimal {
    public static final double TEMPT_SPEED_MOD = 0.6;
    public static final double WALK_SPEED_MOD = 0.8;
    public static final double SPRINT_SPEED_MOD = 1.33;
    private static final EntityDataAccessor<Holder<CatVariant>> DATA_VARIANT_ID = SynchedEntityData.defineId(Cat.class, EntityDataSerializers.CAT_VARIANT);
    private static final EntityDataAccessor<Boolean> IS_LYING = SynchedEntityData.defineId(Cat.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> RELAX_STATE_ONE = SynchedEntityData.defineId(Cat.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_COLLAR_COLOR = SynchedEntityData.defineId(Cat.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Holder<CatSoundVariant>> DATA_SOUND_VARIANT_ID = SynchedEntityData.defineId(
        Cat.class, EntityDataSerializers.CAT_SOUND_VARIANT
    );
    private static final ResourceKey<CatVariant> DEFAULT_VARIANT = CatVariants.BLACK;
    private static final DyeColor DEFAULT_COLLAR_COLOR = DyeColor.RED;
    private static final EntityDimensions BABY_DIMENSIONS = EntityDimensions.scalable(0.3F, 0.35F)
        .withEyeHeight(0.34375F)
        .withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0.0F, 0.3125F, 0.0F));
    private Cat.@Nullable CatAvoidEntityGoal<Player> avoidPlayersGoal;
    private @Nullable TemptGoal temptGoal;
    private float lieDownAmount;
    private float lieDownAmountO;
    private float lieDownAmountTail;
    private float lieDownAmountOTail;
    private boolean isLyingOnTopOfSleepingPlayer;
    private float relaxStateOneAmount;
    private float relaxStateOneAmountO;

    public Cat(final EntityType<? extends Cat> type, final Level level) {
        super(type, level);
        this.reassessTameGoals();
    }

    @Override
    protected void registerGoals() {
        this.temptGoal = new Cat.CatTemptGoal(this, 0.6, i -> i.is(ItemTags.CAT_FOOD), true);
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(1, new TamableAnimal.TamableAnimalPanicGoal(1.5));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new Cat.CatRelaxOnOwnerGoal(this));
        this.goalSelector.addGoal(4, this.temptGoal);
        this.goalSelector.addGoal(5, new CatLieOnBedGoal(this, 1.1, 8));
        this.goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.0, 10.0F, 5.0F));
        this.goalSelector.addGoal(7, new CatSitOnBlockGoal(this, 0.8));
        this.goalSelector.addGoal(8, new LeapAtTargetGoal(this, 0.3F));
        this.goalSelector.addGoal(9, new OcelotAttackGoal(this));
        this.goalSelector.addGoal(10, new BreedGoal(this, 0.8));
        this.goalSelector.addGoal(11, new WaterAvoidingRandomStrollGoal(this, 0.8, 1.0000001E-5F));
        this.goalSelector.addGoal(12, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.targetSelector.addGoal(1, new NonTameRandomTargetGoal<>(this, Rabbit.class, false, null));
        this.targetSelector.addGoal(1, new NonTameRandomTargetGoal<>(this, Turtle.class, false, Turtle.BABY_ON_LAND_SELECTOR));
    }

    public Holder<CatVariant> getVariant() {
        return this.entityData.get(DATA_VARIANT_ID);
    }

    private void setVariant(final Holder<CatVariant> variant) {
        this.entityData.set(DATA_VARIANT_ID, variant);
    }

    private Holder<CatSoundVariant> getSoundVariant() {
        return this.entityData.get(DATA_SOUND_VARIANT_ID);
    }

    private void setSoundVariant(final Holder<CatSoundVariant> soundVariant) {
        this.entityData.set(DATA_SOUND_VARIANT_ID, soundVariant);
    }

    private CatSoundVariant.CatSoundSet getSoundSet() {
        return this.isBaby() ? this.getSoundVariant().value().babySounds() : this.getSoundVariant().value().adultSounds();
    }

    @Override
    public <T> @Nullable T get(final DataComponentType<? extends T> type) {
        if (type == DataComponents.CAT_VARIANT) {
            return castComponentValue((DataComponentType<T>)type, this.getVariant());
        } else if (type == DataComponents.CAT_SOUND_VARIANT) {
            return castComponentValue((DataComponentType<T>)type, this.getSoundVariant());
        } else {
            return type == DataComponents.CAT_COLLAR ? castComponentValue((DataComponentType<T>)type, this.getCollarColor()) : super.get(type);
        }
    }

    @Override
    protected void applyImplicitComponents(final DataComponentGetter components) {
        this.applyImplicitComponentIfPresent(components, DataComponents.CAT_VARIANT);
        this.applyImplicitComponentIfPresent(components, DataComponents.CAT_SOUND_VARIANT);
        this.applyImplicitComponentIfPresent(components, DataComponents.CAT_COLLAR);
        super.applyImplicitComponents(components);
    }

    @Override
    protected <T> boolean applyImplicitComponent(final DataComponentType<T> type, final T value) {
        if (type == DataComponents.CAT_VARIANT) {
            this.setVariant(castComponentValue(DataComponents.CAT_VARIANT, value));
            return true;
        } else if (type == DataComponents.CAT_SOUND_VARIANT) {
            this.setSoundVariant(castComponentValue(DataComponents.CAT_SOUND_VARIANT, value));
            return true;
        } else if (type == DataComponents.CAT_COLLAR) {
            this.setCollarColor(castComponentValue(DataComponents.CAT_COLLAR, value));
            return true;
        } else {
            return super.applyImplicitComponent(type, value);
        }
    }

    public void setLying(final boolean value) {
        this.entityData.set(IS_LYING, value);
    }

    public boolean isLying() {
        return this.entityData.get(IS_LYING);
    }

    private void setRelaxStateOne(final boolean value) {
        this.entityData.set(RELAX_STATE_ONE, value);
    }

    private boolean isRelaxStateOne() {
        return this.entityData.get(RELAX_STATE_ONE);
    }

    public DyeColor getCollarColor() {
        return DyeColor.byId(this.entityData.get(DATA_COLLAR_COLOR));
    }

    private void setCollarColor(final DyeColor color) {
        this.entityData.set(DATA_COLLAR_COLOR, color.getId());
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        Registry<CatSoundVariant> catSoundVariants = this.registryAccess().lookupOrThrow(Registries.CAT_SOUND_VARIANT);
        entityData.define(DATA_VARIANT_ID, VariantUtils.getDefaultOrAny(this.registryAccess(), DEFAULT_VARIANT));
        entityData.define(DATA_SOUND_VARIANT_ID, catSoundVariants.get(CatSoundVariants.CLASSIC).or(catSoundVariants::getAny).orElseThrow());
        entityData.define(IS_LYING, false);
        entityData.define(RELAX_STATE_ONE, false);
        entityData.define(DATA_COLLAR_COLOR, DEFAULT_COLLAR_COLOR.getId());
    }

    @Override
    protected void addAdditionalSaveData(final ValueOutput output) {
        super.addAdditionalSaveData(output);
        VariantUtils.writeVariant(output, this.getVariant());
        this.getSoundVariant()
            .unwrapKey()
            .ifPresent(
                soundVariant -> output.store("sound_variant", ResourceKey.codec(Registries.CAT_SOUND_VARIANT), (ResourceKey<CatSoundVariant>)soundVariant)
            );
        output.store("CollarColor", DyeColor.LEGACY_ID_CODEC, this.getCollarColor());
    }

    @Override
    protected void readAdditionalSaveData(final ValueInput input) {
        super.readAdditionalSaveData(input);
        VariantUtils.readVariant(input, Registries.CAT_VARIANT).ifPresent(this::setVariant);
        input.read("sound_variant", ResourceKey.codec(Registries.CAT_SOUND_VARIANT))
            .flatMap(soundVariant -> this.registryAccess().lookupOrThrow(Registries.CAT_SOUND_VARIANT).get((ResourceKey<CatSoundVariant>)soundVariant))
            .ifPresent(this::setSoundVariant);
        this.setCollarColor(input.read("CollarColor", DyeColor.LEGACY_ID_CODEC).orElse(DEFAULT_COLLAR_COLOR));
    }

    @Override
    public void customServerAiStep(final ServerLevel level) {
        if (this.getMoveControl().hasWanted()) {
            double speed = this.getMoveControl().getSpeedModifier();
            if (speed == 0.6) {
                this.setPose(Pose.CROUCHING);
                this.setSprinting(false);
            } else if (speed == 1.33) {
                this.setPose(Pose.STANDING);
                this.setSprinting(true);
            } else {
                this.setPose(Pose.STANDING);
                this.setSprinting(false);
            }
        } else {
            this.setPose(Pose.STANDING);
            this.setSprinting(false);
        }
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        if (this.isTame()) {
            if (this.isInLove()) {
                return this.getSoundSet().purrSound().value();
            } else {
                return this.random.nextInt(4) == 0 ? this.getSoundSet().purreowSound().value() : this.getSoundSet().ambientSound().value();
            }
        } else {
            return this.getSoundSet().strayAmbientSound().value();
        }
    }

    @Override
    public int getAmbientSoundInterval() {
        return 120;
    }

    public void hiss() {
        this.makeSound(this.getSoundSet().hissSound().value());
    }

    @Override
    protected SoundEvent getHurtSound(final DamageSource source) {
        return this.getSoundSet().hurtSound().value();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return this.getSoundSet().deathSound().value();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.3F).add(Attributes.ATTACK_DAMAGE, 3.0);
    }

    @Override
    public EntityDimensions getDefaultDimensions(final Pose pose) {
        return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(pose);
    }

    @Override
    protected void playEatingSound() {
        this.playSound(this.getSoundSet().eatSound().value(), 1.0F, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.temptGoal != null && this.temptGoal.isRunning() && !this.isTame() && this.tickCount % 100 == 0) {
            this.playSound(this.getSoundSet().begForFoodSound().value(), 1.0F, 1.0F);
        }

        this.handleLieDown();
    }

    private void handleLieDown() {
        if ((this.isLying() || this.isRelaxStateOne()) && this.tickCount % 5 == 0) {
            this.playSound(this.getSoundSet().purrSound().value(), 0.6F + 0.4F * (this.random.nextFloat() - this.random.nextFloat()), 1.0F);
        }

        this.updateLieDownAmount();
        this.updateRelaxStateOneAmount();
        this.isLyingOnTopOfSleepingPlayer = false;
        if (this.isLying()) {
            BlockPos catPos = this.blockPosition();

            for (Player player : this.level().getEntitiesOfClass(Player.class, new AABB(catPos).inflate(2.0, 2.0, 2.0))) {
                if (player.isSleeping()) {
                    this.isLyingOnTopOfSleepingPlayer = true;
                    break;
                }
            }
        }
    }

    public boolean isLyingOnTopOfSleepingPlayer() {
        return this.isLyingOnTopOfSleepingPlayer;
    }

    private void updateLieDownAmount() {
        this.lieDownAmountO = this.lieDownAmount;
        this.lieDownAmountOTail = this.lieDownAmountTail;
        if (this.isLying()) {
            this.lieDownAmount = Math.min(1.0F, this.lieDownAmount + 0.15F);
            this.lieDownAmountTail = Math.min(1.0F, this.lieDownAmountTail + 0.08F);
        } else {
            this.lieDownAmount = Math.max(0.0F, this.lieDownAmount - 0.22F);
            this.lieDownAmountTail = Math.max(0.0F, this.lieDownAmountTail - 0.13F);
        }
    }

    private void updateRelaxStateOneAmount() {
        this.relaxStateOneAmountO = this.relaxStateOneAmount;
        if (this.isRelaxStateOne()) {
            this.relaxStateOneAmount = Math.min(1.0F, this.relaxStateOneAmount + 0.1F);
        } else {
            this.relaxStateOneAmount = Math.max(0.0F, this.relaxStateOneAmount - 0.13F);
        }
    }

    public float getLieDownAmount(final float a) {
        return Mth.lerp(a, this.lieDownAmountO, this.lieDownAmount);
    }

    public float getLieDownAmountTail(final float a) {
        return Mth.lerp(a, this.lieDownAmountOTail, this.lieDownAmountTail);
    }

    public float getRelaxStateOneAmount(final float a) {
        return Mth.lerp(a, this.relaxStateOneAmountO, this.relaxStateOneAmount);
    }

    public @Nullable Cat getBreedOffspring(final ServerLevel level, final AgeableMob partner) {
        Cat baby = EntityTypes.CAT.create(level, EntitySpawnReason.BREEDING);
        if (baby != null && partner instanceof Cat partnerCat) {
            if (this.random.nextBoolean()) {
                baby.setVariant(this.getVariant());
            } else {
                baby.setVariant(partnerCat.getVariant());
            }

            if (this.isTame()) {
                baby.setOwnerReference(this.getOwnerReference());
                baby.setTame(true, true);
                DyeColor parent1CollarColor = this.getCollarColor();
                DyeColor parent2CollarColor = partnerCat.getCollarColor();
                baby.setCollarColor(DyeColor.getMixedColor(level, parent1CollarColor, parent2CollarColor));
            }
        }

        return baby;
    }

    @Override
    public boolean canMate(final Animal partner) {
        if (!this.isTame()) {
            return false;
        } else {
            return !(partner instanceof Cat cat) ? false : cat.isTame() && super.canMate(partner);
        }
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
        final ServerLevelAccessor level, final DifficultyInstance difficulty, final EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData
    ) {
        groupData = super.finalizeSpawn(level, difficulty, spawnReason, groupData);
        VariantUtils.selectVariantToSpawn(SpawnContext.create(level, this.blockPosition()), Registries.CAT_VARIANT).ifPresent(this::setVariant);
        this.setSoundVariant(CatSoundVariants.pickRandomSoundVariant(this.registryAccess(), level.getRandom()));
        return groupData;
    }

    @Override
    public InteractionResult mobInteract(final Player player, final InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (this.isTame()) {
            if (this.isOwnedBy(player)) {
                if (itemStack.is(ItemTags.CAT_COLLAR_DYES)) {
                    DyeColor color = itemStack.get(DataComponents.DYE);
                    if (color != null && color != this.getCollarColor()) {
                        if (!this.level().isClientSide()) {
                            this.setCollarColor(color);
                            itemStack.consume(1, player);
                            this.setPersistenceRequired();
                        }

                        return InteractionResult.SUCCESS;
                    }
                } else if (this.isFood(itemStack) && this.getHealth() < this.getMaxHealth()) {
                    if (!this.level().isClientSide()) {
                        this.feed(player, hand, itemStack, 1.0F, 1.0F);
                    }

                    return InteractionResult.SUCCESS;
                }

                InteractionResult parentInteraction = super.mobInteract(player, hand);
                if (!parentInteraction.consumesAction()) {
                    this.setOrderedToSit(!this.isOrderedToSit());
                    return InteractionResult.SUCCESS;
                }

                return parentInteraction;
            }
        } else if (this.isFood(itemStack)) {
            if (!this.level().isClientSide()) {
                this.usePlayerItem(player, hand, itemStack);
                this.tryToTame(player);
                this.setPersistenceRequired();
                this.playEatingSound();
            }

            return InteractionResult.SUCCESS;
        }

        InteractionResult interact = super.mobInteract(player, hand);
        if (interact.consumesAction()) {
            this.setPersistenceRequired();
        }

        return interact;
    }

    @Override
    public boolean isFood(final ItemStack itemStack) {
        return itemStack.is(ItemTags.CAT_FOOD);
    }

    @Override
    public boolean removeWhenFarAway(final double distSqr) {
        return !this.isTame() && this.tickCount > 2400;
    }

    @Override
    public void setTame(final boolean isTame, final boolean includeSideEffects) {
        super.setTame(isTame, includeSideEffects);
        this.reassessTameGoals();
    }

    protected void reassessTameGoals() {
        if (this.avoidPlayersGoal == null) {
            this.avoidPlayersGoal = new Cat.CatAvoidEntityGoal<>(this, Player.class, 16.0F, 0.8, 1.33);
        }

        this.goalSelector.removeGoal(this.avoidPlayersGoal);
        if (!this.isTame()) {
            this.goalSelector.addGoal(4, this.avoidPlayersGoal);
        }
    }

    private void tryToTame(final Player player) {
        if (this.random.nextInt(3) == 0) {
            this.tame(player);
            this.setOrderedToSit(true);
            this.level().broadcastEntityEvent(this, (byte)7);
        } else {
            this.level().broadcastEntityEvent(this, (byte)6);
        }
    }

    @Override
    public boolean isSteppingCarefully() {
        return this.isCrouching() || super.isSteppingCarefully();
    }

    private static class CatAvoidEntityGoal<T extends LivingEntity> extends AvoidEntityGoal<T> {
        private final Cat cat;

        public CatAvoidEntityGoal(
            final Cat cat, final Class<T> avoidClass, final float maxDist, final double walkSpeedModifier, final double sprintSpeedModifier
        ) {
            super(cat, avoidClass, maxDist, walkSpeedModifier, sprintSpeedModifier, EntitySelector.NO_CREATIVE_OR_SPECTATOR);
            this.cat = cat;
        }

        @Override
        public boolean canUse() {
            return !this.cat.isTame() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !this.cat.isTame() && super.canContinueToUse();
        }
    }

    private static class CatRelaxOnOwnerGoal extends Goal {
        private final Cat cat;
        private @Nullable Player ownerPlayer;
        private @Nullable BlockPos goalPos;
        private int onBedTicks;

        public CatRelaxOnOwnerGoal(final Cat cat) {
            this.cat = cat;
        }

        @Override
        public boolean canUse() {
            if (!this.cat.isTame()) {
                return false;
            }

            if (this.cat.isOrderedToSit()) {
                return false;
            }

            LivingEntity owner = this.cat.getOwner();
            if (owner instanceof Player playerOwner) {
                this.ownerPlayer = playerOwner;
                if (!owner.isSleeping()) {
                    return false;
                }

                if (this.cat.distanceToSqr(this.ownerPlayer) > 100.0) {
                    return false;
                }

                BlockPos ownerPos = this.ownerPlayer.blockPosition();
                BlockState ownerPosState = this.cat.level().getBlockState(ownerPos);
                if (ownerPosState.is(BlockTags.BEDS)) {
                    this.goalPos = ownerPosState.getOptionalValue(BedBlock.FACING)
                        .map(bedDir -> ownerPos.relative(bedDir.getOpposite()))
                        .orElseGet(() -> new BlockPos(ownerPos));
                    return !this.spaceIsOccupied();
                }
            }

            return false;
        }

        private boolean spaceIsOccupied() {
            for (Cat otherCat : this.cat.level().getEntitiesOfClass(Cat.class, new AABB(this.goalPos).inflate(2.0))) {
                if (otherCat != this.cat && (otherCat.isLying() || otherCat.isRelaxStateOne())) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return this.cat.isTame()
                && !this.cat.isOrderedToSit()
                && this.ownerPlayer != null
                && this.ownerPlayer.isSleeping()
                && this.goalPos != null
                && !this.spaceIsOccupied();
        }

        @Override
        public void start() {
            if (this.goalPos != null) {
                this.cat.setInSittingPose(false);
                this.cat.getNavigation().moveTo(this.goalPos.getX(), this.goalPos.getY(), this.goalPos.getZ(), 1.1F);
            }
        }

        @Override
        public void stop() {
            this.cat.setLying(false);
            if (this.ownerPlayer.getSleepTimer() >= 100
                && this.cat.level().getRandom().nextFloat()
                    < this.cat.level().environmentAttributes().getValue(EnvironmentAttributes.CAT_WAKING_UP_GIFT_CHANCE, this.cat.position())) {
                this.giveMorningGift();
            }

            this.onBedTicks = 0;
            this.cat.setRelaxStateOne(false);
            this.cat.getNavigation().stop();
        }

        private void giveMorningGift() {
            RandomSource random = this.cat.getRandom();
            BlockPos.MutableBlockPos catPos = new BlockPos.MutableBlockPos();
            catPos.set(this.cat.isLeashed() ? this.cat.getLeashHolder().blockPosition() : this.cat.blockPosition());
            this.cat
                .randomTeleport(catPos.getX() + random.nextInt(11) - 5, catPos.getY() + random.nextInt(5) - 2, catPos.getZ() + random.nextInt(11) - 5, false);
            catPos.set(this.cat.blockPosition());
            this.cat
                .dropFromGiftLootTable(
                    getServerLevel(this.cat),
                    BuiltInLootTables.CAT_MORNING_GIFT,
                    (level, itemStack) -> level.addFreshEntity(
                        new ItemEntity(
                            level,
                            (double)catPos.getX() - Mth.sin(this.cat.yBodyRot * (float) (Math.PI / 180.0)),
                            catPos.getY(),
                            (double)catPos.getZ() + Mth.cos(this.cat.yBodyRot * (float) (Math.PI / 180.0)),
                            itemStack
                        )
                    )
                );
        }

        @Override
        public void tick() {
            if (this.ownerPlayer != null && this.goalPos != null) {
                this.cat.setInSittingPose(false);
                this.cat.getNavigation().moveTo(this.goalPos.getX(), this.goalPos.getY(), this.goalPos.getZ(), 1.1F);
                if (this.cat.distanceToSqr(this.ownerPlayer) < 2.5) {
                    this.onBedTicks++;
                    if (this.onBedTicks > this.adjustedTickDelay(16)) {
                        this.cat.setLying(true);
                        this.cat.setRelaxStateOne(false);
                    } else {
                        this.cat.lookAt(this.ownerPlayer, 45.0F, 45.0F);
                        this.cat.setRelaxStateOne(true);
                    }
                } else {
                    this.cat.setLying(false);
                }
            }
        }
    }

    private static class CatTemptGoal extends TemptGoal {
        private @Nullable Player selectedPlayer;
        private final Cat cat;

        public CatTemptGoal(final Cat mob, final double speedModifier, final Predicate<ItemStack> items, final boolean canScare) {
            super(mob, speedModifier, items, canScare);
            this.cat = mob;
        }

        @Override
        public void tick() {
            super.tick();
            if (this.selectedPlayer == null && this.mob.getRandom().nextInt(this.adjustedTickDelay(600)) == 0) {
                this.selectedPlayer = this.player;
            } else if (this.mob.getRandom().nextInt(this.adjustedTickDelay(500)) == 0) {
                this.selectedPlayer = null;
            }
        }

        @Override
        protected boolean canScare() {
            return this.selectedPlayer != null && this.selectedPlayer.equals(this.player) ? false : super.canScare();
        }

        @Override
        public boolean canUse() {
            return super.canUse() && !this.cat.isTame();
        }
    }
}