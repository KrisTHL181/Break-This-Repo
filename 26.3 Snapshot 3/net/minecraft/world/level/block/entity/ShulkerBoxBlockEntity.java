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

   public ShulkerBoxBlockEntity(final @Nullable DyeColor color, final BlockPos worldPosition, final BlockState blockState) {
      super(BlockEntityTypes.SHULKER_BOX, worldPosition, blockState);
      this.color = color;
   }

   public ShulkerBoxBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
      super(BlockEntityTypes.SHULKER_BOX, worldPosition, blockState);
      this.color = blockState.getBlock() instanceof ShulkerBoxBlock shulkerBoxBlock ? shulkerBoxBlock.getColor() : null;
   }

   public static void tick(final Level level, final BlockPos pos, final BlockState state, final ShulkerBoxBlockEntity entity) {
      entity.updateAnimation(level, pos, state);
   }

   private void updateAnimation(final Level level, final BlockPos pos, final BlockState blockState) {
      this.progressOld = this.progress;
      switch (this.animationStatus) {
         case CLOSED:
            this.progress = 0.0F;
            break;
         case OPENING:
            this.progress += 0.1F;
            if (this.progressOld == 0.0F) {
               doNeighborUpdates(level, pos, blockState);
            }

            if (this.progress >= 1.0F) {
               this.animationStatus = ShulkerBoxBlockEntity.AnimationStatus.OPENED;
               this.progress = 1.0F;
               doNeighborUpdates(level, pos, blockState);
            }

            this.moveCollidedEntities(level, pos, blockState);
            break;
         case OPENED:
            this.progress = 1.0F;
            break;
         case CLOSING:
            this.progress -= 0.1F;
            if (this.progressOld == 1.0F) {
               doNeighborUpdates(level, pos, blockState);
            }

            if (this.progress <= 0.0F) {
               this.animationStatus = ShulkerBoxBlockEntity.AnimationStatus.CLOSED;
               this.progress = 0.0F;
               doNeighborUpdates(level, pos, blockState);
            }
      }
   }

   public ShulkerBoxBlockEntity.AnimationStatus getAnimationStatus() {
      return this.animationStatus;
   }

   public AABB getBoundingBox(final BlockState state) {
      Vec3 bottomCenter = new Vec3(0.5, 0.0, 0.5);
      return Shulker.getProgressAabb(1.0F, state.getValue(ShulkerBoxBlock.FACING), 0.5F * this.getProgress(1.0F), bottomCenter);
   }

   private void moveCollidedEntities(final Level level, final BlockPos pos, final BlockState state) {
      if (state.getBlock() instanceof ShulkerBoxBlock) {
         Direction direction = state.getValue(ShulkerBoxBlock.FACING);
         AABB aabb = Shulker.getProgressDeltaAabb(1.0F, direction, this.progressOld, this.progress, Vec3.atBottomCenterOf(pos));
         List<Entity> entities = level.getEntities(null, aabb);
         if (!entities.isEmpty()) {
            for (Entity entity : entities) {
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
   public boolean triggerEvent(final int b0, final int b1) {
      if (b0 == 1) {
         this.openCount = b1;
         if (b1 == 0) {
            this.animationStatus = ShulkerBoxBlockEntity.AnimationStatus.CLOSING;
         }

         if (b1 == 1) {
            this.animationStatus = ShulkerBoxBlockEntity.AnimationStatus.OPENING;
         }

         return true;
      } else {
         return super.triggerEvent(b0, b1);
      }
   }

   private static void doNeighborUpdates(final Level level, final BlockPos pos, final BlockState blockState) {
      blockState.updateNeighbourShapes(level, pos, 3);
      level.updateNeighborsAt(pos, blockState.getBlock());
   }

   @Override
   public void preRemoveSideEffects(final BlockPos pos, final BlockState state) {
   }

   @Override
   public void startOpen(final ContainerUser containerUser) {
      if (!this.remove && !containerUser.getLivingEntity().isSpectator()) {
         if (this.openCount < 0) {
            this.openCount = 0;
         }

         this.openCount++;
         this.level.blockEvent(this.worldPosition, this.getBlockState().getBlock(), 1, this.openCount);
         if (this.openCount == 1) {
            this.level.gameEvent(containerUser.getLivingEntity(), GameEvent.CONTAINER_OPEN, this.worldPosition);
            this.level
               .playSound(null, this.worldPosition, SoundEvents.SHULKER_BOX_OPEN, SoundSource.BLOCKS, 0.5F, this.level.getRandom().nextFloat() * 0.1F + 0.9F);
         }
      }
   }

   @Override
   public void stopOpen(final ContainerUser containerUser) {
      if (!this.remove && !containerUser.getLivingEntity().isSpectator()) {
         this.openCount--;
         this.level.blockEvent(this.worldPosition, this.getBlockState().getBlock(), 1, this.openCount);
         if (this.openCount <= 0) {
            this.level.gameEvent(containerUser.getLivingEntity(), GameEvent.CONTAINER_CLOSE, this.worldPosition);
            this.level
               .playSound(null, this.worldPosition, SoundEvents.SHULKER_BOX_CLOSE, SoundSource.BLOCKS, 0.5F, this.level.getRandom().nextFloat() * 0.1F + 0.9F);
         }
      }
   }

   @Override
   protected Component getDefaultName() {
      return DEFAULT_NAME;
   }

   @Override
   protected void loadAdditional(final ValueInput input) {
      super.loadAdditional(input);
      this.loadFromTag(input);
   }

   @Override
   protected void saveAdditional(final ValueOutput output) {
      super.saveAdditional(output);
      if (!this.trySaveLootTable(output)) {
         ContainerHelper.saveAllItems(output, this.itemStacks, false);
      }
   }

   public void loadFromTag(final ValueInput input) {
      this.itemStacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
      if (!this.tryLoadLootTable(input)) {
         ContainerHelper.loadAllItems(input, this.itemStacks);
      }
   }

   @Override
   protected NonNullList<ItemStack> getItems() {
      return this.itemStacks;
   }

   @Override
   protected void setItems(final NonNullList<ItemStack> items) {
      this.itemStacks = items;
   }

   @Override
   public int[] getSlotsForFace(final Direction direction) {
      return SLOTS;
   }

   @Override
   public boolean canPlaceItemThroughFace(final int slot, final ItemStack itemStack, final @Nullable Direction direction) {
      return !(Block.byItem(itemStack.getItem()) instanceof ShulkerBoxBlock);
   }

   @Override
   public boolean canTakeItemThroughFace(final int slot, final ItemStack itemStack, final Direction direction) {
      return true;
   }

   public float getProgress(final float a) {
      return Mth.lerp(a, this.progressOld, this.progress);
   }

   public @Nullable DyeColor getColor() {
      return this.color;
   }

   @Override
   protected AbstractContainerMenu createMenu(final int containerId, final Inventory inventory) {
      return new ShulkerBoxMenu(containerId, inventory, this);
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
