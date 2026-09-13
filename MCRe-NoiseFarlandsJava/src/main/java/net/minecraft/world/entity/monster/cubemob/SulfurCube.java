package net.minecraft.world.entity.monster.cubemob;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Bucketable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.SulfurCubeArchetype;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.SulfurCubeContent;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class SulfurCube extends AbstractCubeMob implements Bucketable, Shearable {
    public static final int SPLIT_COUNT = 2;
    public static final int MAX_SIZE = 2;
    public static final int MIN_SIZE = 1;
    public static final int PICKUP_TIMER_DURATION = 100;
    public static final double PUSH_DISTANCE_THRESHOLD = 1.3F;
    private int pickupTimer = 0;
    private int pushSoundCooldown = 0;
    private boolean floatsInLiquids = false;
    private static final double MAX_PLAYER_PUSH_SPEED = 0.5;
    private static final float PLAYER_PUSH_SPEED_SCALE_MULTIPLIER = 0.3F;
    private static final float VEHICLE_PUSH_SPEED_SCALE_MULTIPLIER = 0.16F;
    private static final float VERTICAL_PUSH_MULTIPLIER = 0.3F;
    private Optional<SulfurCubeArchetype.ExplosionData> explosionData = Optional.empty();
    private SulfurCubeArchetype.KnockbackModifiers knockbackModifier = SulfurCubeArchetype.DEFAULT_KNOCKBACK_MODIFIERS;
    private SulfurCubeArchetype.SoundSettings soundSettings = SulfurCubeArchetype.DEFAULT_SOUND_SETTINGS;
    private int fuse = -1;
    private List<SulfurCubeArchetype.ContactDamage> contactDamages = new ArrayList<>();
    private static final EntityDataAccessor<Integer> MAX_FUSE = SynchedEntityData.defineId(SulfurCube.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FROM_BUCKET = SynchedEntityData.defineId(SulfurCube.class, EntityDataSerializers.BOOLEAN);
    private static final boolean DEFAULT_FROM_BUCKET = false;
    private static final float HORIZONTAL_HIT_ANGLE_SCALE = 1.6F;
    private static final float VERTICAL_HIT_ANGLE_SCALE = 0.5F;
    private static final float VERTICAL_POSITION_ANGLE_SCALE = 0.8F;
    private static final float EXTRA_KNOCKBACK_DAMPENING = 0.25F;
    private static final Predicate<ItemEntity> ALLOWED_ITEMS = e -> !e.hasPickUpDelay() && e.isAlive() && isSwallowableItem(e.getItem());

    public SulfurCube(final EntityType<? extends SulfurCube> type, final Level level) {
        super(type, level);
        this.lookControl = new SulfurCube.SulfurCubeLookControl();
        this.moveControl = new SulfurCube.SulfurCubeMobMoveControl<>(this);
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(FROM_BUCKET, false);
        entityData.define(MAX_FUSE, -1);
    }

    @Override
    protected void addBehaviourGoals() {
        this.goalSelector
            .addGoal(
                2,
                new SulfurCube.SulfurCubeTemptGoal(
                    this, 1.0, itemStack -> this.isBaby() ? itemStack.is(ItemTags.SULFUR_CUBE_FOOD) : isSwallowableItem(itemStack), false, 1.0
                )
            );
        this.goalSelector.addGoal(3, new SulfurCube.SulfurCubeSearchForItemsGoal(this));
    }

    @Override
    public boolean fromBucket() {
        return this.entityData.get(FROM_BUCKET);
    }

    public int getFuse() {
        return this.fuse;
    }

    public boolean isPrimed() {
        return this.getFuse() >= 0;
    }

    private void setFuse(final int fuse) {
        this.fuse = fuse;
    }

    @Override
    public void onSyncedDataUpdated(final EntityDataAccessor<?> accessor) {
        if (MAX_FUSE.equals(accessor)) {
            this.setFuse(this.entityData.get(MAX_FUSE));
        }

        super.onSyncedDataUpdated(accessor);
    }

    @Override
    public void setFromBucket(final boolean fromBucket) {
        this.entityData.set(FROM_BUCKET, fromBucket);
    }

    @Override
    public SoundEvent getPickupSound() {
        return SoundEvents.BUCKET_FILL_SULFUR_CUBE;
    }

    @Override
    public void saveToBucketTag(final ItemStack bucket) {
        Bucketable.saveDefaultDataToBucketTag(this, bucket);
        bucket.copyFrom(DataComponents.SULFUR_CUBE_CONTENT, this);
        CustomData.update(DataComponents.BUCKET_ENTITY_DATA, bucket, tag -> {
            tag.putInt("age", this.getAge());
            tag.putBoolean("age_locked", this.isAgeLocked());
        });
    }

    @Override
    public boolean canBreatheUnderwater() {
        return this.hasBodyItem() || super.canBreatheUnderwater();
    }

    @Override
    protected void travelInFluid(final Vec3 input) {
        super.travelInFluid(input);
        if (this.hasBodyItem() && this.floatsInLiquids) {
            float vibeAmount = 0.2F * Mth.sin(this.tickCount * 0.4F);
            double immersion = this.getFluidHeight(this.isInWater() ? FluidTags.WATER : FluidTags.LAVA) - this.getFluidJumpThreshold() + vibeAmount;
            if (immersion > 0.0) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0, Math.min(1.0, immersion) * 0.04F, 0.0));
            }
        }
    }

    @Override
    public double getFluidJumpThreshold() {
        return this.getBbHeight() * 0.2;
    }

    @Override
    public void loadFromBucketTag(final CompoundTag tag) {
        Bucketable.loadDefaultDataFromBucketTag(this, tag);
        this.setAge(tag.getIntOr("age", 0));
        this.setAgeLocked(tag.getBooleanOr("age_locked", false));
    }

    @Override
    public ItemStack getBucketItemStack() {
        return new ItemStack(Items.SULFUR_CUBE_BUCKET);
    }

    @Override
    protected void addTargetingGoals() {
    }

    @Override
    public float getLightLevelDependentMagicValue() {
        return 1.0F;
    }

    @Override
    protected boolean isDealsDamage() {
        return false;
    }

    public static boolean checkSulfurCubeSpawnRules(
        final EntityType<SulfurCube> type, final LevelAccessor level, final EntitySpawnReason spawnReason, final BlockPos pos, final RandomSource random
    ) {
        return true;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return super.requiresCustomPersistence() || this.hasBodyItem() || this.fromBucket();
    }

    @Override
    public boolean canBeLeashed() {
        return this.hasBodyItem();
    }

    @Override
    public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
        if (this.hasBodyItem()) {
            if (this.canExplode() && !this.isPrimed()) {
                Entity sourceEntity = source.getDirectEntity();
                if (source.is(DamageTypeTags.IS_FIRE) || sourceEntity instanceof AbstractArrow projectile && projectile.isOnFire()) {
                    this.primeTime(false);
                } else if (source.is(DamageTypeTags.IS_EXPLOSION)) {
                    this.primeTime(true);
                }
            }

            if (source.is(DamageTypeTags.SULFUR_CUBE_WITH_BLOCK_IMMUNE_TO)) {
                if (!source.is(DamageTypeTags.NO_KNOCKBACK)) {
                    this.dealDefaultKnockback(source, damage, true);
                }

                return true;
            }
        }

        return super.hurtServer(level, source, damage);
    }

    public boolean hasBodyItem() {
        return !this.getItemBySlot(EquipmentSlot.BODY).isEmpty();
    }

    public boolean canExplode() {
        return this.explosionData.isPresent() && this.isAlive() && !this.isPrimed();
    }

    @VisibleForTesting
    public List<SulfurCubeArchetype> matchingArchetypes(final ItemStack stack) {
        return this.level()
            .registryAccess()
            .lookupOrThrow(Registries.SULFUR_CUBE_ARCHETYPE)
            .stream()
            .filter(arch -> stack.is(arch.items()))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public void tick() {
        this.tickFuse();
        this.primeWhenOnPoweredPosition();
        super.tick();
    }

    private void tickFuse() {
        if (this.fuse > 0) {
            this.fuse--;
        }

        if (!this.explosionData.isEmpty()) {
            if (this.fuse == 0) {
                this.dropLeash();
                this.dead = true;
                if (this.level() instanceof ServerLevel level) {
                    if (level.getGameRules().get(GameRules.TNT_EXPLODES)) {
                        Level.ExplosionInteraction explosionInteraction = level.getGameRules().get(GameRules.MOB_GRIEFING)
                            ? Level.ExplosionInteraction.TNT
                            : Level.ExplosionInteraction.NONE;
                        level.explode(
                            this,
                            Explosion.getDefaultDamageSource(this.level(), this),
                            this.getPortalCooldown() > 0 ? PrimedTnt.USED_PORTAL_DAMAGE_CALCULATOR : null,
                            this.getX(),
                            this.getY(0.0625),
                            this.getZ(),
                            this.explosionData.get().power(),
                            this.explosionData.get().causesFire(),
                            explosionInteraction
                        );
                    }

                    this.triggerOnDeathMobEffects(level, Entity.RemovalReason.KILLED);
                }

                this.discard();
            }
        }
    }

    private void primeWhenOnPoweredPosition() {
        if (this.level() instanceof ServerLevel level && this.canExplode()) {
            BlockPos here = BlockPos.containing(this.position());
            if (level.getBestOwnOrNeighbourSignal(here) != 0) {
                this.primeTime(false);
            }
        }
    }

    public boolean primeTime(final boolean imminent) {
        if (!this.explosionData.isEmpty()
            && this.isAlive()
            && this.level() instanceof ServerLevel serverLevel
            && serverLevel.getGameRules().get(GameRules.TNT_EXPLODES)
            && !this.isPrimed()) {
            int fuse = this.explosionData.get().fuse();
            int fuseTime = imminent ? PrimedTnt.getRandomShortFuse(fuse, this.getRandom()) : fuse;
            this.setInvulnerable(true);
            this.setFuse(fuseTime);
            this.entityData.set(MAX_FUSE, fuseTime);
            this.makeSound(SoundEvents.TNT_PRIMED);
            this.gameEvent(GameEvent.PRIME_FUSE);
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void customServerAiStep(final ServerLevel level) {
        super.customServerAiStep(level);
        if (this.pickupTimer > 0) {
            this.pickupTimer--;
        }

        if (this.pushSoundCooldown > 0) {
            this.pushSoundCooldown--;
        }
    }

    @Override
    protected @Nullable Map<EquipmentSlot, ItemStack> collectEquipmentChanges(final Map<EquipmentSlot, ItemStack> lastEquipmentItems) {
        ItemStack previous = lastEquipmentItems.get(EquipmentSlot.BODY);
        ItemStack current = this.getItemBySlot(EquipmentSlot.BODY);
        if (this.equipmentHasChanged(previous, current)) {
            if (!current.isEmpty()) {
                this.removeAllGoals(g -> true);
                this.setSpeed(0.0F);
            } else {
                this.registerGoals();
            }

            for (SulfurCubeArchetype archetype : this.matchingArchetypes(previous)) {
                for (SulfurCubeArchetype.AttributeEntry mod : archetype.attributeModifiers()) {
                    AttributeInstance attr = this.getAttribute(mod.attribute());
                    if (attr != null) {
                        attr.removeModifier(mod.modifier());
                    }
                }
            }

            this.floatsInLiquids = false;
            this.explosionData = Optional.empty();
            this.contactDamages.clear();
            this.knockbackModifier = SulfurCubeArchetype.DEFAULT_KNOCKBACK_MODIFIERS;
            this.soundSettings = SulfurCubeArchetype.DEFAULT_SOUND_SETTINGS;

            for (SulfurCubeArchetype archetype : this.matchingArchetypes(current)) {
                if (archetype.buoyant()) {
                    this.floatsInLiquids = true;
                }

                if (archetype.explosion().isPresent()) {
                    this.explosionData = archetype.explosion();
                }

                if (archetype.contactDamage().isPresent()) {
                    this.contactDamages.add(archetype.contactDamage().get());
                }

                this.knockbackModifier = archetype.knockbackModifiers();
                this.soundSettings = archetype.soundSettings();

                for (SulfurCubeArchetype.AttributeEntry mod : archetype.attributeModifiers()) {
                    AttributeInstance attr = this.getAttribute(mod.attribute());
                    if (attr != null) {
                        attr.addOrUpdateTransientModifier(mod.modifier());
                    }
                }
            }
        }

        return super.collectEquipmentChanges(lastEquipmentItems);
    }

    @Override
    public float maxUpStep() {
        return this.hasBodyItem() ? 0.0F : super.maxUpStep();
    }

    @Override
    protected boolean omnidirectionalAirMover() {
        return this.hasBodyItem();
    }

    @Override
    public boolean canFreeze() {
        return this.hasBodyItem() ? false : super.canFreeze();
    }

    @Override
    protected InteractionResult mobInteract(final Player player, final InteractionHand hand) {
        ItemStack heldItem = player.getItemInHand(hand);
        if (this.isBaby()) {
            if (this.isFood(heldItem) && this.canAgeUp()) {
                int age = this.getAge();
                this.usePlayerItem(player, hand, heldItem);
                this.ageUp(getSpeedUpSecondsWhenFeeding(-age), true);
                this.playEatingSound();
                return InteractionResult.SUCCESS;
            } else {
                return super.mobInteract(player, hand);
            }
        } else {
            if (this.isPrimed()) {
                return InteractionResult.PASS;
            }

            if (!this.canExplode() || !heldItem.is(Items.FLINT_AND_STEEL) && !heldItem.is(Items.FIRE_CHARGE)) {
                if (heldItem.is(Items.SHEARS) && this.readyForShearing()) {
                    if (this.level() instanceof ServerLevel level) {
                        ItemStack itemStackToShear = this.getItemBySlot(EquipmentSlot.BODY);
                        this.shear(level, SoundSource.PLAYERS, heldItem);
                        this.gameEvent(GameEvent.SHEAR, player);
                        heldItem.hurtAndBreak(1, player, hand.asEquipmentSlot());
                        CriteriaTriggers.PLAYER_SHEARED_EQUIPMENT.trigger((ServerPlayer)player, itemStackToShear, this);
                    }

                    return InteractionResult.SUCCESS;
                } else if (isSwallowableItem(heldItem)) {
                    boolean itWorked = this.equipItem(heldItem);
                    if (itWorked) {
                        heldItem.consume(1, player);
                        this.gameEvent(GameEvent.ENTITY_INTERACT);
                    }

                    return itWorked ? InteractionResult.SUCCESS_SERVER : InteractionResult.PASS;
                } else {
                    return Bucketable.bucketMobPickup(player, hand, this).orElse(super.mobInteract(player, hand));
                }
            } else if (this.level() instanceof ServerLevel serverLevel && !serverLevel.getGameRules().get(GameRules.TNT_EXPLODES)) {
                player.sendOverlayMessage(Component.translatable("block.minecraft.tnt.disabled"));
                return InteractionResult.PASS;
            } else {
                this.primeTime(false);
                if (heldItem.is(Items.FLINT_AND_STEEL)) {
                    heldItem.hurtAndBreak(1, player, hand.asEquipmentSlot());
                } else {
                    heldItem.consume(1, player);
                }

                player.awardStat(Stats.ITEM_USED.get(heldItem.getItem()));
                return InteractionResult.SUCCESS_SERVER;
            }
        }
    }

    public boolean equipItem(final ItemStack heldItem) {
        if (this.isBaby()) {
            return false;
        }

        if (this.hasBodyItem()) {
            Item swallowedItem = this.getItemBySlot(EquipmentSlot.BODY).getItem();
            if (heldItem.is(swallowedItem)) {
                return false;
            }

            Vec3 equipmentSpawnOffset = this.getAttachments().getAverage(EntityAttachment.PASSENGER);
            if (this.level() instanceof ServerLevel serverLevel) {
                this.spawnAtLocation(serverLevel, this.getItemBySlot(EquipmentSlot.BODY), equipmentSpawnOffset);
            }
        }

        if (!this.level().isClientSide()) {
            ItemStack swallowedItem = this.getItemBySlot(EquipmentSlot.BODY);
            this.setItemSlotAndDropWhenKilled(EquipmentSlot.BODY, heldItem.copyWithCount(1));
            if (!swallowedItem.isEmpty()) {
                Map<EquipmentSlot, ItemStack> lastEquipmentItems = Util.makeEnumMap(EquipmentSlot.class, slot -> ItemStack.EMPTY);
                lastEquipmentItems.put(EquipmentSlot.BODY, swallowedItem);
                this.collectEquipmentChanges(lastEquipmentItems);
            }
        }

        this.playSound(this.getAbsorbSound());
        return true;
    }

    private void applyContactDamage(final Entity entity) {
        if (this.level() instanceof ServerLevel serverLevel) {
            for (SulfurCubeArchetype.ContactDamage damage : this.contactDamages) {
                entity.hurtServer(
                    serverLevel, new DamageSource(damage.damageType(), damage.attributeToSource() ? this : null), damage.amount().sample(this.getRandom())
                );
            }
        }
    }

    protected void playEatingSound() {
        this.makeSound(SoundEvents.SULFUR_CUBE_SMALL_EAT);
    }

    @Override
    public boolean canBePickedUpWithBucket(final ItemStack itemStack) {
        return itemStack.getItem() == Items.BUCKET;
    }

    @Override
    public EquipmentSlot getEquipmentSlotForItem(final ItemStack itemStack) {
        return isSwallowableItem(itemStack) ? EquipmentSlot.BODY : super.getEquipmentSlotForItem(itemStack);
    }

    @Override
    public boolean isEquippableInSlot(final ItemStack itemStack, final EquipmentSlot slot) {
        return slot == EquipmentSlot.BODY ? isSwallowableItem(itemStack) : super.isEquippableInSlot(itemStack, slot);
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.NEUTRAL;
    }

    @Override
    protected SoundEvent getHurtSound(final DamageSource source) {
        return this.isTiny() ? SoundEvents.SULFUR_CUBE_SMALL_HURT : SoundEvents.SULFUR_CUBE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return this.isTiny() ? SoundEvents.SULFUR_CUBE_SMALL_DEATH : SoundEvents.SULFUR_CUBE_DEATH;
    }

    @Override
    protected SoundEvent getSquishSound() {
        if (this.isTiny()) {
            return SoundEvents.SULFUR_CUBE_SMALL_SQUISH;
        } else {
            return this.hasBodyItem() ? SoundEvents.SULFUR_CUBE_BOUNCE : SoundEvents.SULFUR_CUBE_SQUISH;
        }
    }

    @Override
    protected SoundEvent getJumpSound() {
        return this.isTiny() ? SoundEvents.SULFUR_CUBE_SMALL_JUMP : SoundEvents.SULFUR_CUBE_JUMP;
    }

    private SoundEvent getAbsorbSound() {
        return SoundEvents.SULFUR_CUBE_ABSORB;
    }

    private SoundEvent getEjectSound() {
        return SoundEvents.SULFUR_CUBE_EJECT;
    }

    @Override
    protected void playStepSound(final BlockPos pos, final BlockState blockState) {
        if (!this.hasBodyItem()) {
            super.playStepSound(pos, blockState);
        }
    }

    @Override
    protected @Nullable ParticleOptions getParticleType() {
        return ParticleTypes.SULFUR_CUBE_GOO;
    }

    public static AttributeSupplier.Builder createSulfurCubeAttributes() {
        return Mob.createMobAttributes().add(Attributes.TEMPT_RANGE, 8.0);
    }

    @Override
    public void shear(final ServerLevel level, final SoundSource soundSource, final ItemStack tool) {
        Vec3 equipmentSpawnOffset = this.getAttachments().getAverage(EntityAttachment.PASSENGER);
        ItemStack itemStackToShear = this.getItemBySlot(EquipmentSlot.BODY);
        this.setItemSlot(EquipmentSlot.BODY, ItemStack.EMPTY);
        this.spawnAtLocation(level, itemStackToShear, equipmentSpawnOffset);
        this.playSound(this.getEjectSound());
        this.pickupTimer = 100;
    }

    @Override
    public boolean readyForShearing() {
        return this.hasBodyItem();
    }

    @Override
    public boolean canPickUpLoot() {
        return !this.hasBodyItem();
    }

    private static boolean isSwallowableItem(final ItemStack itemStack) {
        return itemStack.is(ItemTags.SULFUR_CUBE_SWALLOWABLE);
    }

    @Override
    public boolean canUseSlot(final EquipmentSlot slot) {
        return slot != EquipmentSlot.BODY ? super.canUseSlot(slot) : this.isAlive() && !this.isBaby();
    }

    @Override
    protected boolean canDispenserEquipIntoSlot(final EquipmentSlot slot) {
        return slot == EquipmentSlot.BODY;
    }

    @Override
    public boolean canHoldItem(final ItemStack itemStack) {
        ItemStack heldItemStack = this.getItemBySlot(EquipmentSlot.BODY);
        return heldItemStack.isEmpty() && isSwallowableItem(itemStack) && !this.isBaby();
    }

    @Override
    protected void pickUpItem(final ServerLevel level, final ItemEntity entity) {
        ItemStack itemStack = entity.getItem();
        if (this.canHoldItem(itemStack) && this.pickupTimer <= 0) {
            this.onItemPickup(entity);
            this.setItemSlot(EquipmentSlot.BODY, itemStack.split(1));
            this.playSound(this.getAbsorbSound());
            this.setGuaranteedDrop(EquipmentSlot.BODY);
            this.take(entity, 1);
        }
    }

    @Override
    protected int getBaseExperienceReward(final ServerLevel level) {
        return this.isBaby() ? 0 : 1 + this.random.nextInt(2);
    }

    @Override
    protected int getSplitCount() {
        return this.isPrimed() ? 0 : 2;
    }

    @Override
    protected void setSpawnSize(final ServerLevelAccessor level, final DifficultyInstance difficulty) {
        if (this.isBaby()) {
            this.setSize(1, true);
        } else {
            this.setSize(2, true);
        }
    }

    @Override
    public void setSize(final int size, final boolean updateHealth) {
        super.setSize(size, updateHealth);
        if (updateHealth && size == 1 && !this.isBaby()) {
            this.setBaby(true);
        }
    }

    @Override
    protected void setUpSplitCube(final AbstractCubeMob cubeMob, final int halfSize, final float xd, final float zd) {
        super.setUpSplitCube(cubeMob, halfSize, xd, zd);
        cubeMob.setBaby(true);
    }

    @Override
    public @Nullable AbstractCubeMob getBreedOffspring(final ServerLevel level, final AgeableMob partner) {
        SulfurCube sulfurCube = EntityTypes.SULFUR_CUBE.create(level, EntitySpawnReason.BREEDING);
        if (sulfurCube != null) {
            sulfurCube.setSize(1, true);
        }

        return sulfurCube;
    }

    private boolean isFood(final ItemStack itemStack) {
        return itemStack.is(ItemTags.SULFUR_CUBE_FOOD);
    }

    @Override
    protected void ageBoundaryReached() {
        super.ageBoundaryReached();
        if (!this.isBaby()) {
            this.setSize(2, true);
        }
    }

    @Override
    protected void addAdditionalSaveData(final ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("pickup_timer", this.pickupTimer);
        output.putBoolean("from_bucket", this.fromBucket());
        output.putInt("fuse", this.getFuse());
    }

    @Override
    protected void readAdditionalSaveData(final ValueInput input) {
        this.pickupTimer = input.getIntOr("pickup_timer", 0);
        this.setFromBucket(input.getBooleanOr("from_bucket", false));
        this.setFuse(input.getIntOr("fuse", -1));
        this.entityData.set(MAX_FUSE, this.getFuse());
        super.readAdditionalSaveData(input);
    }

    @Override
    protected void doPush(final Entity entity) {
        super.doPush(entity);
        this.applyContactDamage(entity);
    }

    @Override
    public void playerTouch(final Player player) {
        super.playerTouch(player);
        this.playerPush(player);
    }

    private void playerPush(final Player player) {
        if (this.hasBodyItem()) {
            Entity pusher = player.isPassenger() ? player.getRootVehicle() : player;
            Vec3 cubeToPusher = this.position().subtract(pusher.position());
            double pusherFeetPosition = pusher.getY();
            double sulfurCubeBottomPosition = this.getY();
            double sulfurCubeTopPosition = sulfurCubeBottomPosition + this.getBbHeight();
            double pusherTopPosition = pusherFeetPosition + pusher.getBbHeight();
            if (cubeToPusher.horizontalDistance() < 1.3F && pusherFeetPosition <= sulfurCubeTopPosition && pusherTopPosition > sulfurCubeBottomPosition) {
                double knockback = Math.max(0.0, 1.0 - this.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
                Vec3 pushDirection = cubeToPusher.horizontal().normalize().scale(knockback);
                float pushSpeedScale = player.isPassenger() ? 0.16F : 0.3F;
                double playerSpeed = player.getKnownSpeed().length() * 2.0 * pushSpeedScale;
                playerSpeed = Mth.clamp(playerSpeed, 0.0, 0.5);
                Vec3 pushVelocity = new Vec3(pushDirection.x, this.onGround() ? knockback * 0.3F : 0.0, pushDirection.z).scale(playerSpeed);
                this.needsSync = true;
                float push_sound_threshold = this.soundSettings.pushSoundImpulseThreshold();
                if (pushVelocity.lengthSqr() > push_sound_threshold * push_sound_threshold && this.pushSoundCooldown <= 0) {
                    this.pushSoundCooldown = (int)(this.soundSettings.pushSoundCooldown() * 20.0F);
                    this.playSound(this.soundSettings.pushSound().value());
                }

                this.addDeltaMovement(pushVelocity);
                this.applyContactDamage(player);
            }
        }
    }

    private Vec2 applyHorizontalHitAngleScale(
        final float horizontalAngleScale, final Vec2 originalAngle, final Vec3 attackerPosition, final Vec3 attackerAimDirection, final Vec3 targetCenter
    ) {
        Vec3 attackerToTarget = targetCenter.subtract(attackerPosition).normalize();
        float angleDiff = (float)Math.atan2(
            attackerAimDirection.x * attackerToTarget.z - attackerAimDirection.z * attackerToTarget.x,
            attackerAimDirection.x * attackerToTarget.x + attackerAimDirection.z * attackerToTarget.z
        );
        return originalAngle.rotate(angleDiff * horizontalAngleScale);
    }

    private Vec2 applyVerticalHitAnglePowerTransfer(
        final float verticalHitAngleScale,
        final float horizontalPower,
        final float verticalPower,
        final Vec3 attackerPosition,
        final Vec3 attackerAimDirection,
        final Vec3 targetCenteredPosition,
        final float targetHeight
    ) {
        float targetHalfHeight = 0.5F * targetHeight;
        Vec3 targetTopPos = targetCenteredPosition.add(0.0, targetHalfHeight, 0.0);
        Vec3 tagetBottomPos = targetCenteredPosition.add(0.0, -targetHalfHeight, 0.0);
        Vec3 attackerToTargetTop = targetTopPos.subtract(attackerPosition).normalize();
        Vec3 attackerToTargetBottom = tagetBottomPos.subtract(attackerPosition).normalize();
        float verticalHitAngleFactor = (float)Mth.clampedMap(attackerAimDirection.y, attackerToTargetTop.y, attackerToTargetBottom.y, -1.0, 1.0);
        float transferredPowerRatio = Math.abs(verticalHitAngleFactor * verticalHitAngleScale);
        if (verticalHitAngleFactor < 0.0F) {
            transferredPowerRatio = -transferredPowerRatio;
        }

        float px = horizontalPower * (1.0F - transferredPowerRatio);
        float py = verticalPower * (1.0F + transferredPowerRatio);
        return new Vec2(px, py);
    }

    private Vec2 applyVerticalPositionAnglePowerRotation(
        final float verticalPositionAngleScale,
        final float horizontalPower,
        final float verticalPower,
        final float originalHorizontalPower,
        final float originalVerticalPower,
        final Vec3 attackerFeetPosition,
        final Vec3 targetFeetPosition
    ) {
        Vec3 attackerFeetToTargetFeet = targetFeetPosition.subtract(attackerFeetPosition);
        float verticalPositionAngle = (float)Math.atan2(-attackerFeetToTargetFeet.y, attackerFeetToTargetFeet.horizontalDistance());
        Vec2 powerBeforeRotation = new Vec2(horizontalPower, verticalPower);
        Vec2 rotatedPower = powerBeforeRotation.rotate(-verticalPositionAngle * verticalPositionAngleScale);
        float horizontalRatio = originalHorizontalPower > 0.0F ? Mth.abs(rotatedPower.x) / originalHorizontalPower : 0.0F;
        float verticalRatio = originalVerticalPower > 0.0F ? Mth.abs(rotatedPower.y) / originalVerticalPower : 0.0F;
        float maxRatio = Math.max(horizontalRatio, verticalRatio);
        if (maxRatio > 1.0F) {
            rotatedPower = rotatedPower.scale(1.0F / maxRatio);
        }

        return rotatedPower;
    }

    @Override
    public void knockback(final double power, double xd, double zd, final DamageSource source, final float damage, final boolean comesFromEffect) {
        if (source.getEntity() != null && this.hasBodyItem()) {
            float horizontalHitAngleScale = 1.6F;
            float verticalHitAngleScale = 0.5F;
            float verticalPositionAngleScale = 0.8F;
            float horizontalPower = this.knockbackModifier.horizontalPower();
            float verticalPower = this.knockbackModifier.verticalPower();
            float originalHorizontalPower = horizontalPower;
            float originalVerticalPower = verticalPower;
            Holder<SoundEvent> hitSound = this.soundSettings.hitSound();
            Vec2 originalAngle = new Vec2((float)xd, (float)zd);
            Vec2 newAngle = this.applyHorizontalHitAngleScale(
                1.6F, originalAngle, source.getEntity().getEyePosition(), source.getEntity().getLookAngle().normalize(), this.getBoundingBox().getCenter()
            );
            Vec2 newPower = this.applyVerticalHitAnglePowerTransfer(
                0.5F,
                horizontalPower,
                verticalPower,
                source.getEntity().getEyePosition(),
                source.getEntity().getLookAngle().normalize(),
                this.getBoundingBox().getCenter(),
                this.getBbHeight()
            );
            horizontalPower = newPower.x;
            verticalPower = newPower.y;
            newPower = this.applyVerticalPositionAnglePowerRotation(
                0.8F, horizontalPower, verticalPower, originalHorizontalPower, originalVerticalPower, source.getEntity().position(), this.position()
            );
            horizontalPower = newPower.x;
            verticalPower = newPower.y;
            xd = newAngle.x;
            zd = newAngle.y;
            float powerMultiplier = Mth.sqrt(damage) * (comesFromEffect ? (float)power * 0.25F : 1.0F);
            horizontalPower *= powerMultiplier;
            verticalPower *= powerMultiplier;
            double knockBackResistance = this.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
            horizontalPower *= (float)(1.0 - knockBackResistance);
            verticalPower *= (float)(1.0 - knockBackResistance);
            this.needsSync = true;
            Vec3 deltaMovement = this.getDeltaMovement();
            horizontalPower *= 0.4F;
            horizontalPower = Mth.clamp(horizontalPower, -128.0F, 128.0F);
            verticalPower = Mth.clamp(verticalPower, -128.0F, 128.0F);
            Vec3 horizontalKnockback = new Vec3(xd, 0.0, zd).normalize().scale(horizontalPower);
            this.setDeltaMovement(deltaMovement.x - horizontalKnockback.x, deltaMovement.y + verticalPower * 1.2, deltaMovement.z - horizontalKnockback.z);
            this.playSound(hitSound.value());
        } else {
            super.knockback(power, xd, zd, source, damage, comesFromEffect);
        }
    }

    @Override
    public <T> @Nullable T get(final DataComponentType<? extends T> type) {
        return type == DataComponents.SULFUR_CUBE_CONTENT
            ? castComponentValue((DataComponentType<T>)type, getSulfurCubeContent(this.getBodyArmorItem()))
            : super.get(type);
    }

    @Override
    protected void applyImplicitComponents(final DataComponentGetter components) {
        this.applyImplicitComponentIfPresent(components, DataComponents.SULFUR_CUBE_CONTENT);
        super.applyImplicitComponents(components);
    }

    @Override
    protected <T> boolean applyImplicitComponent(final DataComponentType<T> type, final T value) {
        if (type == DataComponents.SULFUR_CUBE_CONTENT) {
            this.setSulfurCubeContent(castComponentValue(DataComponents.SULFUR_CUBE_CONTENT, value));
            return true;
        } else {
            return super.applyImplicitComponent(type, value);
        }
    }

    private static @Nullable SulfurCubeContent getSulfurCubeContent(final ItemStack itemStack) {
        return itemStack.isEmpty() ? null : SulfurCubeContent.ofNonEmpty(itemStack);
    }

    private void setSulfurCubeContent(final SulfurCubeContent sulfurCubeContent) {
        this.setItemSlotAndDropWhenKilled(EquipmentSlot.BODY, sulfurCubeContent.absorbedBlockItemStack().create());
    }

    @Override
    public Vec3 getLeashOffset() {
        return new Vec3(0.0, this.getBbHeight() / 2.0F, 0.0);
    }

    @Override
    protected void setcubeMobHealth(final int actualSize) {
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(4 * actualSize);
    }

    @Override
    public boolean isInvulnerableToPiercingWeapon() {
        return this.isInvulnerable() && !this.isPrimed();
    }

    @Override
    public boolean canBePickedFromInside() {
        return !this.hasBodyItem();
    }

    private class SulfurCubeLookControl extends LookControl {
        private SulfurCubeLookControl() {
            super(SulfurCube.this);
        }

        @Override
        public void tick() {
            if (!SulfurCube.this.hasBodyItem()) {
                super.tick();
            } else {
                float closeAngle = Mth.wrapDegrees90(SulfurCube.this.getYRot());
                SulfurCube.this.setYRot(SulfurCube.this.getYRot() - closeAngle);
                SulfurCube.this.setYHeadRot(SulfurCube.this.getYRot());
            }
        }
    }

    protected static class SulfurCubeMobMoveControl<T extends SulfurCube> extends AbstractCubeMob.CubeMobMoveControl<T> {
        public SulfurCubeMobMoveControl(final T cubeMob) {
            super(cubeMob);
        }

        @Override
        public void tick() {
            if (!this.mob.hasBodyItem()) {
                super.tick();
            }
        }
    }

    private class SulfurCubeSearchForItemsGoal extends Goal {
        private final SulfurCube sulfurCube;
        private @Nullable ItemEntity targetItem;

        public SulfurCubeSearchForItemsGoal(final SulfurCube sulfurCube) {
            this.setFlags(EnumSet.of(Goal.Flag.LOOK));
            this.sulfurCube = sulfurCube;
        }

        @Override
        public boolean canUse() {
            if (!this.sulfurCube.isBaby() && this.sulfurCube.pickupTimer <= 0) {
                this.targetItem = getServerLevel(this.sulfurCube)
                    .getNearestEntity(
                        this.sulfurCube
                            .level()
                            .getEntitiesOfClass(ItemEntity.class, this.sulfurCube.getBoundingBox().inflate(8.0, 8.0, 8.0), SulfurCube.ALLOWED_ITEMS),
                        this.sulfurCube.getX(),
                        this.sulfurCube.getY(),
                        this.sulfurCube.getZ()
                    );
                return this.targetItem != null;
            } else {
                return false;
            }
        }

        @Override
        public void tick() {
            SulfurCube.this.lookAt(this.targetItem, 10.0F, 10.0F);
            if (SulfurCube.this.getMoveControl() instanceof AbstractCubeMob.CubeMobMoveControl cubeMobMoveControl) {
                cubeMobMoveControl.setDirection(SulfurCube.this.getYRot(), true);
            }
        }
    }

    private static class SulfurCubeTemptGoal extends TemptGoal.ForNonPathfinders {
        public SulfurCubeTemptGoal(
            final Mob mob, final double speedModifier, final Predicate<ItemStack> items, final boolean canScare, final double stopDistance
        ) {
            super(mob, speedModifier, items, canScare, stopDistance);
            this.setFlags(EnumSet.of(Goal.Flag.LOOK));
        }

        @Override
        protected void stopNavigation() {
            if (this.mob.getMoveControl() instanceof AbstractCubeMob.CubeMobMoveControl cubeMobMoveControl) {
                cubeMobMoveControl.setWantedMovement(0.0);
            }
        }

        @Override
        protected void navigateTowards(final Player player) {
            this.mob.lookAt(player, 10.0F, 10.0F);
            if (this.mob.getMoveControl() instanceof AbstractCubeMob.CubeMobMoveControl cubeMobMoveControl) {
                cubeMobMoveControl.setDirection(this.mob.getYRot(), true);
            }
        }
    }
}