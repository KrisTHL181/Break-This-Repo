package net.minecraft.world.entity.animal.equine;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class AbstractChestedHorse extends AbstractHorse {
   private static final EntityDataAccessor<Boolean> DATA_ID_CHEST = SynchedEntityData.defineId(AbstractChestedHorse.class, EntityDataSerializers.BOOLEAN);
   private static final boolean DEFAULT_HAS_CHEST = false;
   private final EntityDimensions babyDimensions;

   protected AbstractChestedHorse(EntityType<? extends AbstractChestedHorse> p_456887_, Level p_455389_) {
      super(p_456887_, p_455389_);
      this.canGallop = false;
      this.babyDimensions = p_456887_.getDimensions()
         .withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0.0F, p_456887_.getHeight() - 0.15625F, 0.0F))
         .scale(0.5F);
   }

   @Override
   protected void randomizeAttributes(RandomSource p_450867_) {
      this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(generateMaxHealth(p_450867_::nextInt));
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_459886_) {
      super.defineSynchedData(p_459886_);
      p_459886_.define(DATA_ID_CHEST, false);
   }

   public static AttributeSupplier.Builder createBaseChestedHorseAttributes() {
      return createBaseHorseAttributes().add(Attributes.MOVEMENT_SPEED, 0.175F).add(Attributes.JUMP_STRENGTH, 0.5);
   }

   public boolean hasChest() {
      return this.entityData.get(DATA_ID_CHEST);
   }

   public void setChest(boolean p_451035_) {
      this.entityData.set(DATA_ID_CHEST, p_451035_);
   }

   @Override
   public EntityDimensions getDefaultDimensions(Pose p_454170_) {
      return this.isBaby() ? this.babyDimensions : super.getDefaultDimensions(p_454170_);
   }

   @Override
   protected void dropEquipment(ServerLevel p_455975_) {
      super.dropEquipment(p_455975_);
      if (this.hasChest()) {
         this.spawnAtLocation(p_455975_, Blocks.CHEST);
         this.setChest(false);
      }
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_458170_) {
      super.addAdditionalSaveData(p_458170_);
      p_458170_.putBoolean("ChestedHorse", this.hasChest());
      if (this.hasChest()) {
         ValueOutput.TypedOutputList<ItemStackWithSlot> typedoutputlist = p_458170_.list("Items", ItemStackWithSlot.CODEC);

         for (int i = 0; i < this.inventory.getContainerSize(); i++) {
            ItemStack itemstack = this.inventory.getItem(i);
            if (!itemstack.isEmpty()) {
               typedoutputlist.add(new ItemStackWithSlot(i, itemstack));
            }
         }
      }
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_455423_) {
      super.readAdditionalSaveData(p_455423_);
      this.setChest(p_455423_.getBooleanOr("ChestedHorse", false));
      this.createInventory();
      if (this.hasChest()) {
         for (ItemStackWithSlot itemstackwithslot : p_455423_.listOrEmpty("Items", ItemStackWithSlot.CODEC)) {
            if (itemstackwithslot.isValidInContainer(this.inventory.getContainerSize())) {
               this.inventory.setItem(itemstackwithslot.slot(), itemstackwithslot.stack());
            }
         }
      }
   }

   @Override
   public @Nullable SlotAccess getSlot(int p_453018_) {
      return p_453018_ == 499 ? new SlotAccess() {
         @Override
         public ItemStack get() {
            return AbstractChestedHorse.this.hasChest() ? new ItemStack(Items.CHEST) : ItemStack.EMPTY;
         }

         @Override
         public boolean set(ItemStack p_452644_) {
            if (p_452644_.isEmpty()) {
               if (AbstractChestedHorse.this.hasChest()) {
                  AbstractChestedHorse.this.setChest(false);
                  AbstractChestedHorse.this.createInventory();
               }

               return true;
            } else if (p_452644_.is(Items.CHEST)) {
               if (!AbstractChestedHorse.this.hasChest()) {
                  AbstractChestedHorse.this.setChest(true);
                  AbstractChestedHorse.this.createInventory();
               }

               return true;
            } else {
               return false;
            }
         }
      } : super.getSlot(p_453018_);
   }

   @Override
   public InteractionResult mobInteract(Player p_451846_, InteractionHand p_453651_) {
      boolean flag = !this.isBaby() && this.isTamed() && p_451846_.isSecondaryUseActive();
      if (!this.isVehicle() && !flag) {
         ItemStack itemstack = p_451846_.getItemInHand(p_453651_);
         if (!itemstack.isEmpty()) {
            if (this.isFood(itemstack)) {
               return this.fedFood(p_451846_, itemstack);
            }

            if (!this.isTamed()) {
               this.makeMad();
               return InteractionResult.SUCCESS;
            }

            if (!this.hasChest() && itemstack.is(Items.CHEST)) {
               this.equipChest(p_451846_, itemstack);
               return InteractionResult.SUCCESS;
            }
         }

         return super.mobInteract(p_451846_, p_453651_);
      } else {
         return super.mobInteract(p_451846_, p_453651_);
      }
   }

   private void equipChest(Player p_458982_, ItemStack p_450952_) {
      this.setChest(true);
      this.playChestEquipsSound();
      p_450952_.consume(1, p_458982_);
      this.createInventory();
   }

   @Override
   public Vec3[] getQuadLeashOffsets() {
      return Leashable.createQuadLeashOffsets(this, 0.04, 0.41, 0.18, 0.73);
   }

   protected void playChestEquipsSound() {
      this.playSound(SoundEvents.DONKEY_CHEST, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
   }

   @Override
   public int getInventoryColumns() {
      return this.hasChest() ? 5 : 0;
   }
}
