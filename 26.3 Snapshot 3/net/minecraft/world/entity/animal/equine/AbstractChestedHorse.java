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

   protected AbstractChestedHorse(final EntityType<? extends AbstractChestedHorse> type, final Level level) {
      super(type, level);
      this.canGallop = false;
      this.babyDimensions = type.getDimensions()
         .withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0.0F, type.getHeight() + 0.03125F, -0.3125F))
         .scale(0.5F);
   }

   @Override
   protected void randomizeAttributes(final RandomSource random) {
      this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(generateMaxHealth(random::nextInt));
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      super.defineSynchedData(entityData);
      entityData.define(DATA_ID_CHEST, false);
   }

   public static AttributeSupplier.Builder createBaseChestedHorseAttributes() {
      return createBaseHorseAttributes().add(Attributes.MOVEMENT_SPEED, 0.175F).add(Attributes.JUMP_STRENGTH, 0.5);
   }

   public boolean hasChest() {
      return this.entityData.get(DATA_ID_CHEST);
   }

   public void setChest(final boolean flag) {
      this.entityData.set(DATA_ID_CHEST, flag);
   }

   @Override
   public EntityDimensions getDefaultDimensions(final Pose pose) {
      return this.isBaby() ? this.babyDimensions : super.getDefaultDimensions(pose);
   }

   @Override
   protected void dropEquipment(final ServerLevel level) {
      super.dropEquipment(level);
      if (this.hasChest()) {
         this.spawnAtLocation(level, Blocks.CHEST);
         this.setChest(false);
      }
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.putBoolean("ChestedHorse", this.hasChest());
      if (this.hasChest()) {
         ValueOutput.TypedOutputList<ItemStackWithSlot> items = output.list("Items", ItemStackWithSlot.CODEC);

         for (int i = 0; i < this.inventory.getContainerSize(); i++) {
            ItemStack stack = this.inventory.getItem(i);
            if (!stack.isEmpty()) {
               items.add(new ItemStackWithSlot(i, stack));
            }
         }
      }
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      super.readAdditionalSaveData(input);
      this.setChest(input.getBooleanOr("ChestedHorse", false));
      this.createInventory();
      if (this.hasChest()) {
         for (ItemStackWithSlot item : input.listOrEmpty("Items", ItemStackWithSlot.CODEC)) {
            if (item.isValidInContainer(this.inventory.getContainerSize())) {
               this.inventory.setItem(item.slot(), item.stack());
            }
         }
      }
   }

   @Override
   public @Nullable SlotAccess getSlot(final int slot) {
      return slot == 499 ? new SlotAccess() {
         @Override
         public ItemStack get() {
            return AbstractChestedHorse.this.hasChest() ? new ItemStack(Items.CHEST) : ItemStack.EMPTY;
         }

         @Override
         public boolean set(final ItemStack itemStack) {
            if (itemStack.isEmpty()) {
               if (AbstractChestedHorse.this.hasChest()) {
                  AbstractChestedHorse.this.setChest(false);
                  AbstractChestedHorse.this.createInventory();
               }

               return true;
            } else if (itemStack.is(Items.CHEST)) {
               if (!AbstractChestedHorse.this.hasChest()) {
                  AbstractChestedHorse.this.setChest(true);
                  AbstractChestedHorse.this.createInventory();
               }

               return true;
            } else {
               return false;
            }
         }
      } : super.getSlot(slot);
   }

   @Override
   public InteractionResult mobInteract(final Player player, final InteractionHand hand) {
      boolean shouldOpenInventory = !this.isBaby() && this.isTamed() && player.isSecondaryUseActive();
      if (!this.isVehicle() && !shouldOpenInventory && (!this.isBaby() || !player.isHolding(Items.GOLDEN_DANDELION))) {
         ItemStack itemStack = player.getItemInHand(hand);
         if (!itemStack.isEmpty()) {
            if (this.isFood(itemStack)) {
               return this.fedFood(player, itemStack);
            }

            if (!this.isTamed()) {
               this.makeMad();
               return InteractionResult.SUCCESS;
            }

            if (!this.hasChest() && itemStack.is(Items.CHEST)) {
               this.equipChest(player, itemStack);
               return InteractionResult.SUCCESS;
            }
         }

         return super.mobInteract(player, hand);
      } else {
         return super.mobInteract(player, hand);
      }
   }

   private void equipChest(final Player player, final ItemStack itemStack) {
      this.setChest(true);
      this.playChestEquipsSound();
      itemStack.consume(1, player);
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
