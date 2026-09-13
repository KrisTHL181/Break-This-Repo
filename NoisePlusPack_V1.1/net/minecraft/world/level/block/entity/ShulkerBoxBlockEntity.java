package net.minecraft.world.level.block.entity;

import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ShulkerBoxBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
   public static final int COLUMNS = 9;
   public static final int ROWS = 3;
   public static final int CONTAINER_SIZE = 27;
   public static final int EVENT_SET_OPEN_COUNT = 1;
   public static final int OPENING_TICK_LENGTH = 10;
   public static final float MAX_LID_HEIGHT = 0.5F;
   public static final float MAX_LID_ROTATION = 270.0F;
   private static final int[] SLOTS = IntStream.range(0, 27).toArray();
   private static final Component DEFAULT_NAME = Component.translatable("container.shulkerBox");
   private NonNullList<ItemStack> itemStacks = NonNullList.withSize(27, ItemStack.EMPTY);
   private int openCount;
   private ShulkerBoxBlockEntity.AnimationStatus animationStatus = ShulkerBoxBlockEntity.AnimationStatus.CLOSED;
   private float progress;
   private float progressOld;
   private final @Nullable DyeColor color;

   public ShulkerBoxBlockEntity(@Nullable DyeColor p_155666_, BlockPos p_155667_, BlockState p_155668_) {
      super(BlockEntityType.SHULKER_BOX, p_155667_, p_155668_);
      this.color = p_155666_;
   }

   public ShulkerBoxBlockEntity(BlockPos p_155670_, BlockState p_155671_) {
      super(BlockEntityType.SHULKER_BOX, p_155670_, p_155671_);
      this.color = p_155671_.getBlock() instanceof ShulkerBoxBlock shulkerboxblock ? shulkerboxblock.getColor() : null;
   }

   public static void tick(Level p_155673_, BlockPos p_155674_, BlockState p_155675_, ShulkerBoxBlockEntity p_155676_) {
      p_155676_.updateAnimation(p_155673_, p_155674_, p_155675_);
   }

   private void updateAnimation(Level p_155680_, BlockPos p_155681_, BlockState p_155682_) {
      this.progressOld = this.progress;
      switch (this.animationStatus) {
         case CLOSED:
            this.progress = 0.0F;
            break;
         case OPENING:
            this.progress += 0.1F;
            if (this.progressOld == 0.0F) {
               doNeighborUpdates(p_155680_, p_155681_, p_155682_);
            }

            if (this.progress >= 1.0F) {
               this.animationStatus = ShulkerBoxBlockEntity.AnimationStatus.OPENED;
               this.progress = 1.0F;
               doNeighborUpdates(p_155680_, p_155681_, p_155682_);
            }

            this.moveCollidedEntities(p_155680_, p_155681_, p_155682_);
            break;
         case OPENED:
            this.progress = 1.0F;
            break;
         case CLOSING:
            this.progress -= 0.1F;
            if (this.progressOld == 1.0F) {
               doNeighborUpdates(p_155680_, p_155681_, p_155682_);
            }

            if (this.progress <= 0.0F) {
               this.animationStatus = ShulkerBoxBlockEntity.AnimationStatus.CLOSED;
               this.progress = 0.0F;
               doNeighborUpdates(p_155680_, p_155681_, p_155682_);
            }
      }
   }

   public ShulkerBoxBlockEntity.AnimationStatus getAnimationStatus() {
      return this.animationStatus;
   }

   public AABB getBoundingBox(BlockState p_59667_) {
      Vec3 vec3 = new Vec3(0.5, 0.0, 0.5);
      return Shulker.getProgressAabb(1.0F, p_59667_.getValue(ShulkerBoxBlock.FACING), 0.5F * this.getProgress(1.0F), vec3);
   }

   private void moveCollidedEntities(Level p_155684_, BlockPos p_155685_, BlockState p_155686_) {
      if (p_155686_.getBlock() instanceof ShulkerBoxBlock) {
         Direction direction = p_155686_.getValue(ShulkerBoxBlock.FACING);
         AABB aabb = Shulker.getProgressDeltaAabb(1.0F, direction, this.progressOld, this.progress, p_155685_.getBottomCenter());
         List<Entity> list = p_155684_.getEntities(null, aabb);
         if (!list.isEmpty()) {
            for (Entity entity : list) {
               if (entity.getPistonPushReaction() != PushReaction.IGNORE) {
                  entity.move(
                     MoverType.SHULKER_BOX,
                     new Vec3(
                        (aabb.getXsize() + 0.01) * direction.getStepX(),
                        (aabb.getYsize() + 0.01) * direction.getStepY(),
                        (aabb.getZsize() + 0.01) * direction.getStepZ()
                     )
                  );
               }
            }
         }
      }
   }

   @Override
   public int getContainerSize() {
      return this.itemStacks.size();
   }

   @Override
   public boolean triggerEvent(int p_59678_, int p_59679_) {
      if (p_59678_ == 1) {
         this.openCount = p_59679_;
         if (p_59679_ == 0) {
            this.animationStatus = ShulkerBoxBlockEntity.AnimationStatus.CLOSING;
         }

         if (p_59679_ == 1) {
            this.animationStatus = ShulkerBoxBlockEntity.AnimationStatus.OPENING;
         }

         return true;
      } else {
         return super.triggerEvent(p_59678_, p_59679_);
      }
   }

   private static void doNeighborUpdates(Level p_155688_, BlockPos p_155689_, BlockState p_155690_) {
      p_155690_.updateNeighbourShapes(p_155688_, p_155689_, 3);
      p_155688_.updateNeighborsAt(p_155689_, p_155690_.getBlock());
   }

   @Override
   public void preRemoveSideEffects(BlockPos p_394542_, BlockState p_391727_) {
   }

   @Override
   public void startOpen(ContainerUser p_423201_) {
      if (!this.remove && !p_423201_.getLivingEntity().isSpectator()) {
         if (this.openCount < 0) {
            this.openCount = 0;
         }

         this.openCount++;
         this.level.blockEvent(this.worldPosition, this.getBlockState().getBlock(), 1, this.openCount);
         if (this.openCount == 1) {
            this.level.gameEvent(p_423201_.getLivingEntity(), GameEvent.CONTAINER_OPEN, this.worldPosition);
            this.level.playSound(null, this.worldPosition, SoundEvents.SHULKER_BOX_OPEN, SoundSource.BLOCKS, 0.5F, this.level.random.nextFloat() * 0.1F + 0.9F);
         }
      }
   }

   @Override
   public void stopOpen(ContainerUser p_429383_) {
      if (!this.remove && !p_429383_.getLivingEntity().isSpectator()) {
         this.openCount--;
         this.level.blockEvent(this.worldPosition, this.getBlockState().getBlock(), 1, this.openCount);
         if (this.openCount <= 0) {
            this.level.gameEvent(p_429383_.getLivingEntity(), GameEvent.CONTAINER_CLOSE, this.worldPosition);
            this.level
               .playSound(null, this.worldPosition, SoundEvents.SHULKER_BOX_CLOSE, SoundSource.BLOCKS, 0.5F, this.level.random.nextFloat() * 0.1F + 0.9F);
         }
      }
   }

   @Override
   protected Component getDefaultName() {
      return DEFAULT_NAME;
   }

   @Override
   protected void loadAdditional(ValueInput p_407419_) {
      super.loadAdditional(p_407419_);
      this.loadFromTag(p_407419_);
   }

   @Override
   protected void saveAdditional(ValueOutput p_408177_) {
      super.saveAdditional(p_408177_);
      if (!this.trySaveLootTable(p_408177_)) {
         ContainerHelper.saveAllItems(p_408177_, this.itemStacks, false);
      }
   }

   public void loadFromTag(ValueInput p_410262_) {
      this.itemStacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
      if (!this.tryLoadLootTable(p_410262_)) {
         ContainerHelper.loadAllItems(p_410262_, this.itemStacks);
      }
   }

   @Override
   protected NonNullList<ItemStack> getItems() {
      return this.itemStacks;
   }

   @Override
   protected void setItems(NonNullList<ItemStack> p_59674_) {
      this.itemStacks = p_59674_;
   }

   @Override
   public int[] getSlotsForFace(Direction p_59672_) {
      return SLOTS;
   }

   @Override
   public boolean canPlaceItemThroughFace(int p_59663_, ItemStack p_59664_, @Nullable Direction p_59665_) {
      return !(Block.byItem(p_59664_.getItem()) instanceof ShulkerBoxBlock);
   }

   @Override
   public boolean canTakeItemThroughFace(int p_59682_, ItemStack p_59683_, Direction p_59684_) {
      return true;
   }

   public float getProgress(float p_59658_) {
      return Mth.lerp(p_59658_, this.progressOld, this.progress);
   }

   public @Nullable DyeColor getColor() {
      return this.color;
   }

   @Override
   protected AbstractContainerMenu createMenu(int p_59660_, Inventory p_59661_) {
      return new ShulkerBoxMenu(p_59660_, p_59661_, this);
   }

   public boolean isClosed() {
      return this.animationStatus == ShulkerBoxBlockEntity.AnimationStatus.CLOSED;
   }

   public enum AnimationStatus {
      CLOSED,
      OPENING,
      OPENED,
      CLOSING;
   }
}
