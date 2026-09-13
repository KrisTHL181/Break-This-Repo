package net.minecraft.world.entity.monster.piglin;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class Piglin extends AbstractPiglin implements CrossbowAttackMob, InventoryCarrier {
    private static final EntityDataAccessor<Boolean> DATA_BABY_ID = SynchedEntityData.defineId(Piglin.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_CHARGING_CROSSBOW = SynchedEntityData.defineId(Piglin.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_DANCING = SynchedEntityData.defineId(Piglin.class, EntityDataSerializers.BOOLEAN);
    private static final Identifier SPEED_MODIFIER_BABY_ID = Identifier.withDefaultNamespace("baby");
    private static final AttributeModifier SPEED_MODIFIER_BABY = new AttributeModifier(
        SPEED_MODIFIER_BABY_ID, 0.2F, AttributeModifier.Operation.ADD_MULTIPLIED_BASE
    );
    private static final int MAX_HEALTH = 16;
    private static final float MOVEMENT_SPEED_WHEN_FIGHTING = 0.35F;
    private static final int ATTACK_DAMAGE = 5;
    private static final float CHANCE_OF_WEARING_EACH_ARMOUR_ITEM = 0.1F;
    private static final int MAX_PASSENGERS_ON_ONE_HOGLIN = 3;
    private static final float PROBABILITY_OF_SPAWNING_AS_BABY = 0.2F;
    private static final EntityDimensions BABY_DIMENSIONS = EntityDimensions.scalable(0.49F, 0.98F)
        .withEyeHeight(0.78F)
        .withAttachments(EntityAttachments.builder().attach(EntityAttachment.VEHICLE, 0.0F, 0.1875F, 0.0F));
    private static final double PROBABILITY_OF_SPAWNING_WITH_CROSSBOW_INSTEAD_OF_SWORD = 0.5;
    private static final boolean DEFAULT_IS_BABY = false;
    private static final boolean DEFAULT_CANNOT_HUNT = false;
    private static final int INVENTORY_SLOT_OFFSET = 300;
    private static final int INVENTORY_SIZE = 8;
    private final SimpleContainer inventory = new SimpleContainer(8);
    private boolean cannotHunt = false;
    private static final Brain.Provider<Piglin> BRAIN_PROVIDER = Brain.<Piglin>provider(
        List.of(
            MemoryModuleType.UNIVERSAL_ANGER,
            MemoryModuleType.ATE_RECENTLY,
            MemoryModuleType.SPEAR_FLEEING_TIME,
            MemoryModuleType.SPEAR_FLEEING_POSITION,
            MemoryModuleType.SPEAR_CHARGE_POSITION,
            MemoryModuleType.SPEAR_ENGAGE_TIME
        ),
        List.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS, SensorType.NEAREST_ITEMS, SensorType.HURT_BY, SensorType.PIGLIN_SPECIFIC_SENSOR),
        PiglinAi::getActivities
    );

    public Piglin(final EntityType<? extends AbstractPiglin> type, final Level level) {
        super(type, level);
        this.xpReward = 5;
    }

    @Override
    protected void addAdditionalSaveData(final ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("IsBaby", this.isBaby());
        output.putBoolean("CannotHunt", this.cannotHunt);
        this.writeInventoryToTag(output);
    }

    @Override
    protected void readAdditionalSaveData(final ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setBaby(input.getBooleanOr("IsBaby", false));
        this.setCannotHunt(input.getBooleanOr("CannotHunt", false));
        this.readInventoryFromTag(input);
    }

    @VisibleForDebug
    @Override
    public SimpleContainer getInventory() {
        return this.inventory;
    }

    @Override
    protected void dropCustomDeathLoot(final ServerLevel level, final DamageSource source, final boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        this.inventory.removeAllItems().forEach(itemStack -> this.spawnAtLocation(level, itemStack));
    }

    protected ItemStack addToInventory(final ItemStack itemStack) {
        return this.inventory.addItem(itemStack);
    }

    protected boolean canAddToInventory(final ItemStack itemStack) {
        return this.inventory.canAddItem(itemStack);
    }

    @Override
    public @Nullable SlotAccess getSlot(final int slot) {
        int inventorySlot = slot - 300;
        return inventorySlot >= 0 && inventorySlot < this.inventory.getContainerSize() ? this.inventory.getSlot(inventorySlot) : super.getSlot(slot);
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_BABY_ID, false);
        entityData.define(DATA_IS_CHARGING_CROSSBOW, false);
        entityData.define(DATA_IS_DANCING, false);
    }

    @Override
    public void onSyncedDataUpdated(final EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DATA_BABY_ID.equals(accessor)) {
            this.refreshDimensions();
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 16.0).add(Attributes.MOVEMENT_SPEED, 0.35F).add(Attributes.ATTACK_DAMAGE, 5.0);
    }

    public static boolean checkPiglinSpawnRules(
        final EntityType<Piglin> type, final LevelAccessor level, final EntitySpawnReason spawnReason, final BlockPos pos, final RandomSource random
    ) {
        return !level.getBlockState(pos.below()).is(Blocks.NETHER_WART_BLOCK);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
        final ServerLevelAccessor level, final DifficultyInstance difficulty, final EntitySpawnReason spawnReason, final @Nullable SpawnGroupData groupData
    ) {
        RandomSource random = level.getRandom();
        if (spawnReason != EntitySpawnReason.STRUCTURE) {
            if (random.nextFloat() < 0.2F) {
                this.setBaby(true);
            } else if (this.isAdult()) {
                this.setItemSlot(EquipmentSlot.MAINHAND, this.createSpawnWeapon());
            }
        }

        PiglinAi.initMemories(this, level.getRandom());
        this.populateDefaultEquipmentSlots(random, difficulty);
        this.populateDefaultEquipmentEnchantments(level, random, difficulty);
        return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
    }

    @Override
    public boolean removeWhenFarAway(final double distSqr) {
        return !this.isPersistenceRequired();
    }

    @Override
    protected void populateDefaultEquipmentSlots(final RandomSource random, final DifficultyInstance difficulty) {
        if (this.isAdult()) {
            this.maybeWearArmor(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET), random);
            this.maybeWearArmor(EquipmentSlot.CHEST, new ItemStack(Items.GOLDEN_CHESTPLATE), random);
            this.maybeWearArmor(EquipmentSlot.LEGS, new ItemStack(Items.GOLDEN_LEGGINGS), random);
            this.maybeWearArmor(EquipmentSlot.FEET, new ItemStack(Items.GOLDEN_BOOTS), random);
        }
    }

    private void maybeWearArmor(final EquipmentSlot slot, final ItemStack itemStack, final RandomSource random) {
        if (random.nextFloat() < 0.1F) {
            this.setItemSlot(slot, itemStack);
        }
    }

    @Override
    protected Brain<Piglin> makeBrain(final Brain.Packed packedBrain) {
        return BRAIN_PROVIDER.makeBrain(this, packedBrain);
    }

    @Override
    public Brain<Piglin> getBrain() {
        return (Brain<Piglin>)super.getBrain();
    }

    @Override
    public InteractionResult mobInteract(final Player player, final InteractionHand hand) {
        InteractionResult interactionResult = super.mobInteract(player, hand);
        if (interactionResult.consumesAction()) {
            return interactionResult;
        } else if (this.level() instanceof ServerLevel level) {
            return PiglinAi.mobInteract(level, this, player, hand);
        } else {
            boolean canAdmire = PiglinAi.canAdmire(this, player.getItemInHand(hand)) && this.getArmPose() != PiglinArmPose.ADMIRING_ITEM;
            return canAdmire ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
    }

    @Override
    public EntityDimensions getDefaultDimensions(final Pose pose) {
        return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(pose);
    }

    @Override
    public void setBaby(final boolean baby) {
        this.getEntityData().set(DATA_BABY_ID, baby);
        if (!this.level().isClientSide()) {
            AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
            speed.removeModifier(SPEED_MODIFIER_BABY.id());
            if (baby) {
                speed.addTransientModifier(SPEED_MODIFIER_BABY);
            }
        }
    }

    @Override
    public boolean isBaby() {
        return this.getEntityData().get(DATA_BABY_ID);
    }

    private void setCannotHunt(final boolean cannotHunt) {
        this.cannotHunt = cannotHunt;
    }

    @Override
    protected boolean canHunt() {
        return !this.cannotHunt;
    }

    @Override
    protected void customServerAiStep(final ServerLevel level) {
        ProfilerFiller profiler = Profiler.get();
        profiler.push("piglinBrain");
        this.getBrain().tick(level, this);
        profiler.pop();
        PiglinAi.updateActivity(this);
        super.customServerAiStep(level);
    }

    @Override
    protected int getBaseExperienceReward(final ServerLevel level) {
        return this.xpReward;
    }

    @Override
    protected void finishConversion(final ServerLevel level) {
        PiglinAi.cancelAdmiring(level, this);
        this.inventory.removeAllItems().forEach(itemStack -> this.spawnAtLocation(level, itemStack));
        super.finishConversion(level);
    }

    private ItemStack createSpawnWeapon() {
        return this.random.nextFloat() < 0.5
            ? new ItemStack(Items.CROSSBOW)
            : new ItemStack(this.random.nextInt(10) == 0 ? Items.GOLDEN_SPEAR : Items.GOLDEN_SWORD);
    }

    @Override
    public @Nullable TagKey<Item> getPreferredWeaponType() {
        return this.isBaby() ? null : ItemTags.PIGLIN_PREFERRED_WEAPONS;
    }

    private boolean isChargingCrossbow() {
        return this.entityData.get(DATA_IS_CHARGING_CROSSBOW);
    }

    @Override
    public void setChargingCrossbow(final boolean isCharging) {
        this.entityData.set(DATA_IS_CHARGING_CROSSBOW, isCharging);
    }

    @Override
    public void onCrossbowAttackPerformed() {
        this.noActionTime = 0;
    }

    @Override
    public PiglinArmPose getArmPose() {
        if (this.isDancing()) {
            return PiglinArmPose.DANCING;
        } else if (PiglinAi.isLovedItem(this.getOffhandItem())) {
            return PiglinArmPose.ADMIRING_ITEM;
        } else if (this.isAggressive() && this.isHoldingMeleeWeapon()) {
            return PiglinArmPose.ATTACKING_WITH_MELEE_WEAPON;
        } else if (this.isChargingCrossbow()) {
            return PiglinArmPose.CROSSBOW_CHARGE;
        } else {
            return this.isHolding(Items.CROSSBOW) && CrossbowItem.isCharged(this.getWeaponItem()) ? PiglinArmPose.CROSSBOW_HOLD : PiglinArmPose.DEFAULT;
        }
    }

    public boolean isDancing() {
        return this.entityData.get(DATA_IS_DANCING);
    }

    public void setDancing(final boolean dancing) {
        this.entityData.set(DATA_IS_DANCING, dancing);
    }

    @Override
    public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
        boolean wasHurt = super.hurtServer(level, source, damage);
        if (wasHurt && source.getEntity() instanceof LivingEntity sourceEntity) {
            PiglinAi.wasHurtBy(level, this, sourceEntity);
        }

        return wasHurt;
    }

    @Override
    public void performRangedAttack(final LivingEntity target, final float power) {
        this.performCrossbowAttack(this, 1.6F);
    }

    @Override
    public boolean canUseNonMeleeWeapon(final ItemStack item) {
        return item.getItem() == Items.CROSSBOW || item.has(DataComponents.KINETIC_WEAPON);
    }

    protected void holdInMainHand(final ItemStack itemStack) {
        this.setItemSlotAndDropWhenKilled(EquipmentSlot.MAINHAND, itemStack);
        this.setPersistenceRequired();
    }

    protected void holdInOffHand(final ItemStack itemStack) {
        this.setItemSlotAndDropWhenKilled(EquipmentSlot.OFFHAND, itemStack);
        if (!itemStack.is(PiglinAi.BARTERING_ITEM)) {
            this.setPersistenceRequired();
        }
    }

    @Override
    public boolean wantsToPickUp(final ServerLevel level, final ItemStack itemStack) {
        return level.getGameRules().get(GameRules.MOB_GRIEFING) && this.canPickUpLoot() && PiglinAi.wantsToPickup(this, itemStack);
    }

    protected boolean canReplaceCurrentItem(final ItemStack newItemStack) {
        EquipmentSlot slot = this.getEquipmentSlotForItem(newItemStack);
        ItemStack currentItemStackInCorrespondingSlot = this.getItemBySlot(slot);
        return this.canReplaceCurrentItem(newItemStack, currentItemStackInCorrespondingSlot, slot);
    }

    @Override
    protected boolean canReplaceCurrentItem(final ItemStack newItemStack, final ItemStack currentItemStack, final EquipmentSlot slot) {
        if (EnchantmentHelper.has(currentItemStack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) {
            return false;
        } else {
            TagKey<Item> preferredWeaponType = this.getPreferredWeaponType();
            boolean newItemWanted = PiglinAi.isLovedItem(newItemStack) || preferredWeaponType != null && newItemStack.is(preferredWeaponType);
            boolean currentItemWanted = PiglinAi.isLovedItem(currentItemStack) || preferredWeaponType != null && currentItemStack.is(preferredWeaponType);
            if (newItemWanted && !currentItemWanted) {
                return true;
            } else {
                return !newItemWanted && currentItemWanted ? false : super.canReplaceCurrentItem(newItemStack, currentItemStack, slot);
            }
        }
    }

    @Override
    protected void pickUpItem(final ServerLevel level, final ItemEntity entity) {
        this.onItemPickup(entity);
        PiglinAi.pickUpItem(level, this, entity);
    }

    @Override
    public boolean startRiding(Entity entityToRide, final boolean force, final boolean sendEventAndTriggers) {
        if (this.isBaby() && entityToRide.is(EntityTypes.HOGLIN)) {
            entityToRide = this.getTopPassenger(entityToRide, 3);
        }

        return super.startRiding(entityToRide, force, sendEventAndTriggers);
    }

    private Entity getTopPassenger(final Entity vehicle, final int counter) {
        List<Entity> passengers = vehicle.getPassengers();
        return counter != 1 && !passengers.isEmpty() ? this.getTopPassenger(passengers.getFirst(), counter - 1) : vehicle;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return this.level().isClientSide() ? null : PiglinAi.getSoundForCurrentActivity(this).orElse(null);
    }

    @Override
    protected SoundEvent getHurtSound(final DamageSource source) {
        return SoundEvents.PIGLIN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PIGLIN_DEATH;
    }

    @Override
    protected void playStepSound(final BlockPos pos, final BlockState blockState) {
        this.playSound(SoundEvents.PIGLIN_STEP, 0.15F, 1.0F);
    }

    @Override
    protected void playConvertedSound() {
        this.makeSound(SoundEvents.PIGLIN_CONVERTED_TO_ZOMBIFIED);
    }
}
