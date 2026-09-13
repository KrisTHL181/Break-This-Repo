package net.minecraft.world.entity.animal.panda;

import com.mojang.serialization.Codec;
import java.util.EnumSet;
import java.util.List;
import java.util.function.IntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Panda extends Animal {
   private static final EntityDataAccessor<Integer> UNHAPPY_COUNTER = SynchedEntityData.defineId(Panda.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> SNEEZE_COUNTER = SynchedEntityData.defineId(Panda.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> EAT_COUNTER = SynchedEntityData.defineId(Panda.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Byte> MAIN_GENE_ID = SynchedEntityData.defineId(Panda.class, EntityDataSerializers.BYTE);
   private static final EntityDataAccessor<Byte> HIDDEN_GENE_ID = SynchedEntityData.defineId(Panda.class, EntityDataSerializers.BYTE);
   private static final EntityDataAccessor<Byte> DATA_ID_FLAGS = SynchedEntityData.defineId(Panda.class, EntityDataSerializers.BYTE);
   private static final TargetingConditions BREED_TARGETING = TargetingConditions.forNonCombat().range(8.0);
   private static final EntityDimensions BABY_DIMENSIONS = EntityTypes.PANDA
      .getDimensions()
      .scale(0.5F)
      .withEyeHeight(0.28125F)
      .withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0.0F, 0.5625F, 0.0F));
   private static final int FLAG_SNEEZE = 2;
   private static final int FLAG_ROLL = 4;
   private static final int FLAG_SIT = 8;
   private static final int FLAG_ON_BACK = 16;
   private static final int EAT_TICK_INTERVAL = 5;
   public static final int TOTAL_ROLL_STEPS = 32;
   private static final int TOTAL_UNHAPPY_TIME = 32;
   private boolean gotBamboo;
   private boolean didBite;
   public int rollCounter;
   private Vec3 rollDelta;
   private float sitAmount;
   private float sitAmountO;
   private float onBackAmount;
   private float onBackAmountO;
   private float rollAmount;
   private float rollAmountO;
   private Panda.PandaLookAtPlayerGoal lookAtPlayerGoal;

   public Panda(final EntityType<? extends Panda> type, final Level level) {
      super(type, level);
      this.moveControl = new Panda.PandaMoveControl<>(this);
      if (!this.isBaby()) {
         this.setCanPickUpLoot(true);
      }
   }

   @Override
   protected boolean canDispenserEquipIntoSlot(final EquipmentSlot slot) {
      return slot == EquipmentSlot.MAINHAND && this.canPickUpLoot();
   }

   public int getUnhappyCounter() {
      return this.entityData.get(UNHAPPY_COUNTER);
   }

   public void setUnhappyCounter(final int value) {
      this.entityData.set(UNHAPPY_COUNTER, value);
   }

   public boolean isSneezing() {
      return this.getFlag(2);
   }

   public boolean isSitting() {
      return this.getFlag(8);
   }

   public void sit(final boolean value) {
      this.setFlag(8, value);
   }

   public boolean isOnBack() {
      return this.getFlag(16);
   }

   public void setOnBack(final boolean value) {
      this.setFlag(16, value);
   }

   public boolean isEating() {
      return this.entityData.get(EAT_COUNTER) > 0;
   }

   public void eat(final boolean value) {
      this.entityData.set(EAT_COUNTER, value ? 1 : 0);
   }

   private int getEatCounter() {
      return this.entityData.get(EAT_COUNTER);
   }

   private void setEatCounter(final int value) {
      this.entityData.set(EAT_COUNTER, value);
   }

   public void sneeze(final boolean value) {
      this.setFlag(2, value);
      if (!value) {
         this.setSneezeCounter(0);
      }
   }

   public int getSneezeCounter() {
      return this.entityData.get(SNEEZE_COUNTER);
   }

   public void setSneezeCounter(final int value) {
      this.entityData.set(SNEEZE_COUNTER, value);
   }

   public Panda.Gene getMainGene() {
      return Panda.Gene.byId(this.entityData.get(MAIN_GENE_ID));
   }

   public void setMainGene(Panda.Gene gene) {
      if (gene.getId() > 6) {
         gene = Panda.Gene.getRandom(this.random);
      }

      this.entityData.set(MAIN_GENE_ID, (byte)gene.getId());
   }

   public Panda.Gene getHiddenGene() {
      return Panda.Gene.byId(this.entityData.get(HIDDEN_GENE_ID));
   }

   public void setHiddenGene(Panda.Gene gene) {
      if (gene.getId() > 6) {
         gene = Panda.Gene.getRandom(this.random);
      }

      this.entityData.set(HIDDEN_GENE_ID, (byte)gene.getId());
   }

   public boolean isRolling() {
      return this.getFlag(4);
   }

   public void roll(final boolean value) {
      this.setFlag(4, value);
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      super.defineSynchedData(entityData);
      entityData.define(UNHAPPY_COUNTER, 0);
      entityData.define(SNEEZE_COUNTER, 0);
      entityData.define(MAIN_GENE_ID, (byte)0);
      entityData.define(HIDDEN_GENE_ID, (byte)0);
      entityData.define(DATA_ID_FLAGS, (byte)0);
      entityData.define(EAT_COUNTER, 0);
   }

   private boolean getFlag(final int flag) {
      return (this.entityData.get(DATA_ID_FLAGS) & flag) != 0;
   }

   private void setFlag(final int flag, final boolean value) {
      byte current = this.entityData.get(DATA_ID_FLAGS);
      if (value) {
         this.entityData.set(DATA_ID_FLAGS, (byte)(current | flag));
      } else {
         this.entityData.set(DATA_ID_FLAGS, (byte)(current & ~flag));
      }
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.store("MainGene", Panda.Gene.CODEC, this.getMainGene());
      output.store("HiddenGene", Panda.Gene.CODEC, this.getHiddenGene());
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      super.readAdditionalSaveData(input);
      this.setMainGene(input.<Panda.Gene>read("MainGene", Panda.Gene.CODEC).orElse(Panda.Gene.NORMAL));
      this.setHiddenGene(input.<Panda.Gene>read("HiddenGene", Panda.Gene.CODEC).orElse(Panda.Gene.NORMAL));
   }

   @Override
   public @Nullable AgeableMob getBreedOffspring(final ServerLevel level, final AgeableMob partner) {
      Panda baby = EntityTypes.PANDA.create(level, EntitySpawnReason.BREEDING);
      if (baby != null) {
         if (partner instanceof Panda partnerPanda) {
            baby.setGeneFromParents(this, partnerPanda);
         }

         baby.setAttributes();
      }

      return baby;
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(2, new Panda.PandaPanicGoal(this, 2.0));
      this.goalSelector.addGoal(2, new Panda.PandaBreedGoal(this, 1.0));
      this.goalSelector.addGoal(3, new Panda.PandaAttackGoal(this, 1.2F, true));
      this.goalSelector.addGoal(4, new TemptGoal(this, 1.0, i -> i.is(ItemTags.PANDA_FOOD), false));
      this.goalSelector.addGoal(6, new Panda.PandaAvoidGoal<>(this, Player.class, 8.0F, 2.0, 2.0));
      this.goalSelector.addGoal(6, new Panda.PandaAvoidGoal<>(this, Monster.class, 4.0F, 2.0, 2.0));
      this.goalSelector.addGoal(7, new Panda.PandaSitGoal());
      this.goalSelector.addGoal(8, new Panda.PandaLieOnBackGoal(this));
      this.goalSelector.addGoal(8, new Panda.PandaSneezeGoal(this));
      this.lookAtPlayerGoal = new Panda.PandaLookAtPlayerGoal(this, Player.class, 6.0F);
      this.goalSelector.addGoal(9, this.lookAtPlayerGoal);
      this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
      this.goalSelector.addGoal(12, new Panda.PandaRollGoal(this));
      this.goalSelector.addGoal(13, new FollowParentGoal(this, 1.25));
      this.goalSelector.addGoal(14, new WaterAvoidingRandomStrollGoal(this, 1.0));
      this.targetSelector.addGoal(1, new Panda.PandaHurtByTargetGoal(this).setAlertOthers());
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Animal.createAnimalAttributes().add(Attributes.MOVEMENT_SPEED, 0.15F).add(Attributes.ATTACK_DAMAGE, 6.0);
   }

   public Panda.Gene getVariant() {
      return Panda.Gene.getVariantFromGenes(this.getMainGene(), this.getHiddenGene());
   }

   public boolean isLazy() {
      return this.getVariant() == Panda.Gene.LAZY;
   }

   public boolean isWorried() {
      return this.getVariant() == Panda.Gene.WORRIED;
   }

   public boolean isPlayful() {
      return this.getVariant() == Panda.Gene.PLAYFUL;
   }

   public boolean isBrown() {
      return this.getVariant() == Panda.Gene.BROWN;
   }

   public boolean isWeak() {
      return this.getVariant() == Panda.Gene.WEAK;
   }

   @Override
   public boolean isAggressive() {
      return this.getVariant() == Panda.Gene.AGGRESSIVE;
   }

   @Override
   public boolean canBeLeashed() {
      return false;
   }

   @Override
   public boolean doHurtTarget(final ServerLevel level, final Entity target) {
      if (!this.isAggressive()) {
         this.didBite = true;
      }

      return super.doHurtTarget(level, target);
   }

   @Override
   public void playAttackSound() {
      this.playSound(SoundEvents.PANDA_BITE, 1.0F, 1.0F);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.isWorried()) {
         if (this.level().isThundering() && !this.isInWater()) {
            this.sit(true);
            this.eat(false);
         } else if (!this.isEating()) {
            this.sit(false);
         }
      }

      LivingEntity target = this.getTarget();
      if (target == null) {
         this.gotBamboo = false;
         this.didBite = false;
      }

      if (this.getUnhappyCounter() > 0) {
         if (target != null) {
            this.lookAt(target, 90.0F, 90.0F);
         }

         if (this.getUnhappyCounter() == 29 || this.getUnhappyCounter() == 14) {
            this.playSound(SoundEvents.PANDA_CANT_BREED, 1.0F, 1.0F);
         }

         this.setUnhappyCounter(this.getUnhappyCounter() - 1);
      }

      if (this.isSneezing()) {
         this.setSneezeCounter(this.getSneezeCounter() + 1);
         if (this.getSneezeCounter() > 20) {
            this.sneeze(false);
            this.afterSneeze();
         } else if (this.getSneezeCounter() == 1) {
            this.playSound(SoundEvents.PANDA_PRE_SNEEZE, 1.0F, 1.0F);
         }
      }

      if (this.isRolling()) {
         this.handleRoll();
      } else {
         this.rollCounter = 0;
      }

      if (this.isSitting()) {
         this.setXRot(0.0F);
      }

      this.updateSitAmount();
      this.handleEating();
      this.updateOnBackAnimation();
      this.updateRollAmount();
   }

   public boolean isScared() {
      return this.isWorried() && this.level().isThundering();
   }

   private void handleEating() {
      if (!this.isEating() && this.isSitting() && !this.isScared() && !this.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty() && this.random.nextInt(80) == 1) {
         this.eat(true);
      } else if (this.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty() || !this.isSitting()) {
         this.eat(false);
      }

      if (this.isEating()) {
         this.addEatingParticles();
         if (!this.level().isClientSide() && this.getEatCounter() > 80 && this.random.nextInt(20) == 1) {
            if (this.getEatCounter() > 100 && this.getItemBySlot(EquipmentSlot.MAINHAND).is(ItemTags.PANDA_EATS_FROM_GROUND)) {
               if (!this.level().isClientSide()) {
                  this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                  this.gameEvent(GameEvent.EAT);
               }

               this.sit(false);
            }

            this.eat(false);
            return;
         }

         this.setEatCounter(this.getEatCounter() + 1);
      }
   }

   private void addEatingParticles() {
      if (this.getEatCounter() % 5 == 0) {
         this.playSound(SoundEvents.PANDA_EAT, 0.5F + 0.5F * this.random.nextInt(2), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
         ItemStack heldItem = this.getItemBySlot(EquipmentSlot.MAINHAND);
         if (!heldItem.isEmpty()) {
            ItemParticleOption breakParticle = new ItemParticleOption(ParticleTypes.ITEM, ItemStackTemplate.fromNonEmptyStack(heldItem));

            for (int i = 0; i < 6; i++) {
               Vec3 velocity = new Vec3((this.random.nextFloat() - 0.5) * 0.1, this.random.nextFloat() * 0.1 + 0.1, (this.random.nextFloat() - 0.5) * 0.1)
                  .xRot(-this.getXRot() * (float) (Math.PI / 180.0))
                  .yRot(-this.getYRot() * (float) (Math.PI / 180.0));
               Vec3 position = new Vec3(
                     (this.random.nextFloat() - 0.5) * 0.8, -this.random.nextFloat() * 0.6 - 0.3, 1.0 + (this.random.nextFloat() - 0.5) * 0.4
                  )
                  .yRot(-this.yBodyRot * (float) (Math.PI / 180.0))
                  .add(this.getX(), this.getEyeY() + 1.0, this.getZ());
               this.level().addParticle(breakParticle, position.x, position.y, position.z, velocity.x, velocity.y + 0.05, velocity.z);
            }
         }
      }
   }

   private void updateSitAmount() {
      this.sitAmountO = this.sitAmount;
      if (this.isSitting()) {
         this.sitAmount = Math.min(1.0F, this.sitAmount + 0.15F);
      } else {
         this.sitAmount = Math.max(0.0F, this.sitAmount - 0.19F);
      }
   }

   private void updateOnBackAnimation() {
      this.onBackAmountO = this.onBackAmount;
      if (this.isOnBack()) {
         this.onBackAmount = Math.min(1.0F, this.onBackAmount + 0.15F);
      } else {
         this.onBackAmount = Math.max(0.0F, this.onBackAmount - 0.19F);
      }
   }

   private void updateRollAmount() {
      this.rollAmountO = this.rollAmount;
      if (this.isRolling()) {
         this.rollAmount = Math.min(1.0F, this.rollAmount + 0.15F);
      } else {
         this.rollAmount = Math.max(0.0F, this.rollAmount - 0.19F);
      }
   }

   public float getSitAmount(final float a) {
      return Mth.lerp(a, this.sitAmountO, this.sitAmount);
   }

   public float getLieOnBackAmount(final float a) {
      return Mth.lerp(a, this.onBackAmountO, this.onBackAmount);
   }

   public float getRollAmount(final float a) {
      return Mth.lerp(a, this.rollAmountO, this.rollAmount);
   }

   private void handleRoll() {
      this.rollCounter++;
      if (this.rollCounter > 32) {
         this.roll(false);
      } else {
         if (!this.level().isClientSide()) {
            Vec3 movement = this.getDeltaMovement();
            if (this.rollCounter == 1) {
               float angle = this.getYRot() * (float) (Math.PI / 180.0);
               float multiplier = this.isBaby() ? 0.1F : 0.2F;
               this.rollDelta = new Vec3(movement.x + -Mth.sin(angle) * multiplier, 0.0, movement.z + Mth.cos(angle) * multiplier);
               this.setDeltaMovement(this.rollDelta.add(0.0, 0.27, 0.0));
            } else if (this.rollCounter != 7.0F && this.rollCounter != 15.0F && this.rollCounter != 23.0F) {
               this.setDeltaMovement(this.rollDelta.x, movement.y, this.rollDelta.z);
            } else {
               this.setDeltaMovement(0.0, this.onGround() ? 0.27 : movement.y, 0.0);
            }
         }
      }
   }

   private void afterSneeze() {
      Vec3 movement = this.getDeltaMovement();
      Level level = this.level();
      level.addParticle(
         ParticleTypes.SNEEZE,
         this.getX() - (this.getBbWidth() + 1.0F) * 0.5 * Mth.sin(this.yBodyRot * (float) (Math.PI / 180.0)),
         this.getEyeY() - 0.1F,
         this.getZ() + (this.getBbWidth() + 1.0F) * 0.5 * Mth.cos(this.yBodyRot * (float) (Math.PI / 180.0)),
         movement.x,
         0.0,
         movement.z
      );
      this.playSound(SoundEvents.PANDA_SNEEZE, 1.0F, 1.0F);

      for (Panda panda : level.getEntitiesOfClass(Panda.class, this.getBoundingBox().inflate(10.0))) {
         if (!panda.isBaby() && panda.onGround() && !panda.isInWater() && panda.canPerformAction()) {
            panda.jumpFromGround();
         }
      }

      if (this.level() instanceof ServerLevel serverLevel && serverLevel.getGameRules().get(GameRules.MOB_DROPS)) {
         this.dropFromGiftLootTable(serverLevel, BuiltInLootTables.PANDA_SNEEZE, this::spawnAtLocation);
      }
   }

   @Override
   protected void pickUpItem(final ServerLevel level, final ItemEntity entity) {
      if (this.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty() && canPickUpAndEat(entity)) {
         this.onItemPickup(entity);
         ItemStack itemStack = entity.getItem();
         this.setItemSlot(EquipmentSlot.MAINHAND, itemStack);
         this.setGuaranteedDrop(EquipmentSlot.MAINHAND);
         this.take(entity, itemStack.getCount());
         entity.discard();
      }
   }

   @Override
   public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
      this.sit(false);
      return super.hurtServer(level, source, damage);
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      final ServerLevelAccessor level, final DifficultyInstance difficulty, final EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData
   ) {
      RandomSource random = level.getRandom();
      this.setMainGene(Panda.Gene.getRandom(random));
      this.setHiddenGene(Panda.Gene.getRandom(random));
      this.setAttributes();
      if (groupData == null) {
         groupData = new AgeableMob.AgeableMobGroupData(0.2F);
      }

      return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
   }

   public void setGeneFromParents(final Panda parent1, final @Nullable Panda parent2) {
      if (parent2 == null) {
         if (this.random.nextBoolean()) {
            this.setMainGene(parent1.getOneOfGenesRandomly());
            this.setHiddenGene(Panda.Gene.getRandom(this.random));
         } else {
            this.setMainGene(Panda.Gene.getRandom(this.random));
            this.setHiddenGene(parent1.getOneOfGenesRandomly());
         }
      } else if (this.random.nextBoolean()) {
         this.setMainGene(parent1.getOneOfGenesRandomly());
         this.setHiddenGene(parent2.getOneOfGenesRandomly());
      } else {
         this.setMainGene(parent2.getOneOfGenesRandomly());
         this.setHiddenGene(parent1.getOneOfGenesRandomly());
      }

      if (this.random.nextInt(32) == 0) {
         this.setMainGene(Panda.Gene.getRandom(this.random));
      }

      if (this.random.nextInt(32) == 0) {
         this.setHiddenGene(Panda.Gene.getRandom(this.random));
      }
   }

   private Panda.Gene getOneOfGenesRandomly() {
      return this.random.nextBoolean() ? this.getMainGene() : this.getHiddenGene();
   }

   public void setAttributes() {
      if (this.isWeak()) {
         this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10.0);
      }

      if (this.isLazy()) {
         this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.07F);
      }
   }

   private void tryToSit() {
      if (!this.isInWater()) {
         this.setZza(0.0F);
         this.getNavigation().stop();
         this.sit(true);
      }
   }

   @Override
   public InteractionResult mobInteract(final Player player, final InteractionHand hand) {
      ItemStack interactionItemStack = player.getItemInHand(hand);
      if (this.isScared()) {
         return InteractionResult.PASS;
      }

      if (this.isOnBack()) {
         this.setOnBack(false);
         return InteractionResult.SUCCESS;
      }

      if (this.isFood(interactionItemStack)) {
         if (this.getTarget() != null) {
            this.gotBamboo = true;
         }

         if (this.canAgeUp()) {
            this.usePlayerItem(player, hand, interactionItemStack);
            this.ageUp((int)(-this.getAge() / 20 * 0.1F), true);
         } else {
            if (this.isBaby()) {
               return InteractionResult.PASS;
            }

            if (!this.level().isClientSide() && this.getAge() == 0 && this.canFallInLove()) {
               this.usePlayerItem(player, hand, interactionItemStack);
               this.setInLove(player);
            } else {
               if (!(this.level() instanceof ServerLevel level) || this.isSitting() || this.isInWater()) {
                  return InteractionResult.PASS;
               }

               this.tryToSit();
               this.eat(true);
               ItemStack pandasCurrentItem = this.getItemBySlot(EquipmentSlot.MAINHAND);
               if (!pandasCurrentItem.isEmpty() && !player.hasInfiniteMaterials()) {
                  this.spawnAtLocation(level, pandasCurrentItem);
               }

               this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(interactionItemStack.getItem(), 1));
               this.usePlayerItem(player, hand, interactionItemStack);
            }
         }

         return InteractionResult.SUCCESS_SERVER;
      } else {
         return this.isBaby() && player.isHolding(Items.GOLDEN_DANDELION) ? super.mobInteract(player, hand) : InteractionResult.PASS;
      }
   }

   @Override
   protected @Nullable SoundEvent getAmbientSound() {
      if (this.isAggressive()) {
         return SoundEvents.PANDA_AGGRESSIVE_AMBIENT;
      } else {
         return this.isWorried() ? SoundEvents.PANDA_WORRIED_AMBIENT : SoundEvents.PANDA_AMBIENT;
      }
   }

   @Override
   protected void playStepSound(final BlockPos pos, final BlockState blockState) {
      this.playSound(SoundEvents.PANDA_STEP, 0.15F, 1.0F);
   }

   @Override
   public boolean isFood(final ItemStack itemStack) {
      return itemStack.is(ItemTags.PANDA_FOOD);
   }

   @Override
   protected @Nullable SoundEvent getDeathSound() {
      return SoundEvents.PANDA_DEATH;
   }

   @Override
   protected @Nullable SoundEvent getHurtSound(final DamageSource source) {
      return SoundEvents.PANDA_HURT;
   }

   public boolean canPerformAction() {
      return !this.isOnBack() && !this.isScared() && !this.isEating() && !this.isRolling() && !this.isSitting();
   }

   @Override
   public EntityDimensions getDefaultDimensions(final Pose pose) {
      return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(pose);
   }

   private static boolean canPickUpAndEat(final ItemEntity entity) {
      return entity.getItem().is(ItemTags.PANDA_EATS_FROM_GROUND) && entity.isAlive() && !entity.hasPickUpDelay();
   }

   public enum Gene implements StringRepresentable {
      NORMAL(0, "normal", false),
      LAZY(1, "lazy", false),
      WORRIED(2, "worried", false),
      PLAYFUL(3, "playful", false),
      BROWN(4, "brown", true),
      WEAK(5, "weak", true),
      AGGRESSIVE(6, "aggressive", false);

      public static final Codec<Panda.Gene> CODEC = StringRepresentable.fromEnum(Panda.Gene::values);
      private static final IntFunction<Panda.Gene> BY_ID = ByIdMap.continuous(Panda.Gene::getId, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
      private static final int MAX_GENE = 6;
      private final int id;
      private final String name;
      private final boolean isRecessive;

      Gene(final int id, final String name, final boolean isRecessive) {
         this.id = id;
         this.name = name;
         this.isRecessive = isRecessive;
      }

      public int getId() {
         return this.id;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }

      public boolean isRecessive() {
         return this.isRecessive;
      }

      private static Panda.Gene getVariantFromGenes(final Panda.Gene mainGene, final Panda.Gene hiddenGene) {
         if (mainGene.isRecessive()) {
            return mainGene == hiddenGene ? mainGene : NORMAL;
         } else {
            return mainGene;
         }
      }

      public static Panda.Gene byId(final int id) {
         return BY_ID.apply(id);
      }

      public static Panda.Gene getRandom(final RandomSource random) {
         int nextInt = random.nextInt(16);
         if (nextInt == 0) {
            return LAZY;
         } else if (nextInt == 1) {
            return WORRIED;
         } else if (nextInt == 2) {
            return PLAYFUL;
         } else if (nextInt == 4) {
            return AGGRESSIVE;
         } else if (nextInt < 9) {
            return WEAK;
         } else {
            return nextInt < 11 ? BROWN : NORMAL;
         }
      }
   }

   private static class PandaAttackGoal extends MeleeAttackGoal {
      private final Panda panda;

      public PandaAttackGoal(final Panda mob, final double speedModifier, final boolean trackTarget) {
         super(mob, speedModifier, trackTarget);
         this.panda = mob;
      }

      @Override
      public boolean canUse() {
         return this.panda.canPerformAction() && super.canUse();
      }
   }

   private static class PandaAvoidGoal<T extends LivingEntity> extends AvoidEntityGoal<T> {
      private final Panda panda;

      public PandaAvoidGoal(final Panda panda, final Class<T> avoidClass, final float maxDist, final double walkSpeedModifier, final double sprintSpeedModifier) {
         super(panda, avoidClass, maxDist, walkSpeedModifier, sprintSpeedModifier, EntitySelector.NO_SPECTATORS);
         this.panda = panda;
      }

      @Override
      public boolean canUse() {
         return this.panda.isWorried() && this.panda.canPerformAction() && super.canUse();
      }
   }

   private static class PandaBreedGoal extends BreedGoal {
      private final Panda panda;
      private int unhappyCooldown;

      public PandaBreedGoal(final Panda panda, final double speedModifier) {
         super(panda, speedModifier);
         this.panda = panda;
      }

      @Override
      public boolean canUse() {
         if (!super.canUse() || this.panda.getUnhappyCounter() != 0) {
            return false;
         }

         if (!this.canFindBamboo()) {
            if (this.unhappyCooldown <= this.panda.tickCount) {
               this.panda.setUnhappyCounter(32);
               this.unhappyCooldown = this.panda.tickCount + 600;
               if (this.panda.isEffectiveAi()) {
                  Player player = this.level.getNearestPlayer(Panda.BREED_TARGETING, this.panda);
                  this.panda.lookAtPlayerGoal.setTarget(player);
               }
            }

            return false;
         } else {
            return true;
         }
      }

      private boolean canFindBamboo() {
         BlockPos pandaPos = this.panda.blockPosition();
         BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

         for (int yOff = 0; yOff < 3; yOff++) {
            for (int r = 0; r < 8; r++) {
               for (int x = 0; x <= r; x = x > 0 ? -x : 1 - x) {
                  for (int z = x < r && x > -r ? r : 0; z <= r; z = z > 0 ? -z : 1 - z) {
                     pos.setWithOffset(pandaPos, x, yOff, z);
                     if (this.level.getBlockState(pos).is(Blocks.BAMBOO)) {
                        return true;
                     }
                  }
               }
            }
         }

         return false;
      }
   }

   private static class PandaHurtByTargetGoal extends HurtByTargetGoal {
      private final Panda panda;

      public PandaHurtByTargetGoal(final Panda mob, final Class<?>... ignoreDamageFromTheseTypes) {
         super(mob, ignoreDamageFromTheseTypes);
         this.panda = mob;
      }

      @Override
      public boolean canContinueToUse() {
         if (!this.panda.gotBamboo && !this.panda.didBite) {
            return super.canContinueToUse();
         }

         this.panda.setTarget(null);
         return false;
      }

      @Override
      protected void alertOther(final Mob other, final LivingEntity hurtByMob) {
         if (other instanceof Panda && other.isAggressive()) {
            other.setTarget(hurtByMob);
         }
      }
   }

   private static class PandaLieOnBackGoal extends Goal {
      private final Panda panda;
      private int cooldown;

      public PandaLieOnBackGoal(final Panda panda) {
         this.panda = panda;
      }

      @Override
      public boolean canUse() {
         return this.cooldown < this.panda.tickCount
            && this.panda.isLazy()
            && this.panda.canPerformAction()
            && this.panda.random.nextInt(reducedTickDelay(400)) == 1;
      }

      @Override
      public boolean canContinueToUse() {
         return !this.panda.isInWater() && (this.panda.isLazy() || this.panda.random.nextInt(reducedTickDelay(600)) != 1)
            ? this.panda.random.nextInt(reducedTickDelay(2000)) != 1
            : false;
      }

      @Override
      public void start() {
         this.panda.setOnBack(true);
         this.cooldown = 0;
      }

      @Override
      public void stop() {
         this.panda.setOnBack(false);
         this.cooldown = this.panda.tickCount + 200;
      }
   }

   private static class PandaLookAtPlayerGoal extends LookAtPlayerGoal {
      private final Panda panda;

      public PandaLookAtPlayerGoal(final Panda mob, final Class<? extends LivingEntity> lookAtType, final float lookDistance) {
         super(mob, lookAtType, lookDistance);
         this.panda = mob;
      }

      public void setTarget(final LivingEntity entity) {
         this.lookAt = entity;
      }

      @Override
      public boolean canContinueToUse() {
         return this.lookAt != null && super.canContinueToUse();
      }

      @Override
      public boolean canUse() {
         if (this.mob.getRandom().nextFloat() >= this.probability) {
            return false;
         }

         if (this.lookAt == null) {
            ServerLevel level = getServerLevel(this.mob);
            if (this.lookAtType == Player.class) {
               this.lookAt = level.getNearestPlayer(this.lookAtContext, this.mob, this.mob.getX(), this.mob.getEyeY(), this.mob.getZ());
            } else {
               this.lookAt = level.getNearestEntity(
                  this.mob
                     .level()
                     .getEntitiesOfClass(this.lookAtType, this.mob.getBoundingBox().inflate(this.lookDistance, 3.0, this.lookDistance), entity -> true),
                  this.lookAtContext,
                  this.mob,
                  this.mob.getX(),
                  this.mob.getEyeY(),
                  this.mob.getZ()
               );
            }
         }

         return this.panda.canPerformAction() && this.lookAt != null;
      }

      @Override
      public void tick() {
         if (this.lookAt != null) {
            super.tick();
         }
      }
   }

   private static class PandaMoveControl<T extends Panda> extends MoveControl<T> {
      public PandaMoveControl(final T mob) {
         super(mob);
      }

      @Override
      public void tick() {
         if (this.mob.canPerformAction()) {
            super.tick();
         }
      }
   }

   private static class PandaPanicGoal extends PanicGoal {
      private final Panda panda;

      public PandaPanicGoal(final Panda mob, final double speedModifier) {
         super(mob, speedModifier, DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES);
         this.panda = mob;
      }

      @Override
      public boolean canContinueToUse() {
         if (this.panda.isSitting()) {
            this.panda.getNavigation().stop();
            return false;
         } else {
            return super.canContinueToUse();
         }
      }
   }

   private static class PandaRollGoal extends Goal {
      private final Panda panda;

      public PandaRollGoal(final Panda panda) {
         this.panda = panda;
         this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
      }

      @Override
      public boolean canUse() {
         if ((this.panda.isBaby() || this.panda.isPlayful()) && this.panda.onGround()) {
            if (!this.panda.canPerformAction()) {
               return false;
            } else {
               float angle = this.panda.getYRot() * (float) (Math.PI / 180.0);
               float xDir = -Mth.sin(angle);
               float zDir = Mth.cos(angle);
               int xStep = Math.abs(xDir) > 0.5 ? Mth.sign(xDir) : 0;
               int zStep = Math.abs(zDir) > 0.5 ? Mth.sign(zDir) : 0;
               if (this.panda.level().getBlockState(this.panda.blockPosition().offset(xStep, -1, zStep)).isAir()) {
                  return true;
               } else {
                  return this.panda.isPlayful() && this.panda.random.nextInt(reducedTickDelay(60)) == 1
                     ? true
                     : this.panda.random.nextInt(reducedTickDelay(500)) == 1;
               }
            }
         } else {
            return false;
         }
      }

      @Override
      public boolean canContinueToUse() {
         return false;
      }

      @Override
      public void start() {
         this.panda.roll(true);
      }

      @Override
      public boolean isInterruptable() {
         return false;
      }
   }

   private class PandaSitGoal extends Goal {
      private int cooldown;

      public PandaSitGoal() {
         this.setFlags(EnumSet.of(Goal.Flag.MOVE));
      }

      @Override
      public boolean canUse() {
         if (this.cooldown > Panda.this.tickCount
            || Panda.this.isBaby()
            || Panda.this.isInWater()
            || !Panda.this.canPerformAction()
            || Panda.this.getUnhappyCounter() > 0) {
            return false;
         } else {
            return !Panda.this.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()
               ? true
               : !Panda.this.level().getEntitiesOfClass(ItemEntity.class, Panda.this.getBoundingBox().inflate(6.0, 6.0, 6.0), Panda::canPickUpAndEat).isEmpty();
         }
      }

      @Override
      public boolean canContinueToUse() {
         return !Panda.this.isInWater() && (Panda.this.isLazy() || Panda.this.random.nextInt(reducedTickDelay(600)) != 1)
            ? Panda.this.random.nextInt(reducedTickDelay(2000)) != 1
            : false;
      }

      @Override
      public void tick() {
         if (!Panda.this.isSitting() && !Panda.this.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) {
            Panda.this.tryToSit();
         }
      }

      @Override
      public void start() {
         if (Panda.this.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) {
            List<ItemEntity> items = Panda.this.level()
               .getEntitiesOfClass(ItemEntity.class, Panda.this.getBoundingBox().inflate(8.0, 8.0, 8.0), Panda::canPickUpAndEat);
            if (!items.isEmpty()) {
               Panda.this.getNavigation().moveTo(items.getFirst(), 1.2F);
            }
         } else {
            Panda.this.tryToSit();
         }

         this.cooldown = 0;
      }

      @Override
      public void stop() {
         ItemStack itemStack = Panda.this.getItemBySlot(EquipmentSlot.MAINHAND);
         if (!itemStack.isEmpty()) {
            Panda.this.spawnAtLocation(getServerLevel(Panda.this.level()), itemStack);
            Panda.this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            int waitSeconds = Panda.this.isLazy() ? Panda.this.random.nextInt(50) + 10 : Panda.this.random.nextInt(150) + 10;
            this.cooldown = Panda.this.tickCount + waitSeconds * 20;
         }

         Panda.this.sit(false);
      }
   }

   private static class PandaSneezeGoal extends Goal {
      private final Panda panda;

      public PandaSneezeGoal(final Panda panda) {
         this.panda = panda;
      }

      @Override
      public boolean canUse() {
         if (this.panda.isBaby() && this.panda.canPerformAction()) {
            return this.panda.isWeak() && this.panda.random.nextInt(reducedTickDelay(500)) == 1 ? true : this.panda.random.nextInt(reducedTickDelay(6000)) == 1;
         } else {
            return false;
         }
      }

      @Override
      public boolean canContinueToUse() {
         return false;
      }

      @Override
      public void start() {
         this.panda.sneeze(true);
      }
   }
}
