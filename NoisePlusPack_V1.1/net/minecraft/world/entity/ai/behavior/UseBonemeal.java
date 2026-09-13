package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

public class UseBonemeal extends Behavior<Villager> {
   private static final int BONEMEALING_DURATION = 80;
   private long nextWorkCycleTime;
   private long lastBonemealingSession;
   private int timeWorkedSoFar;
   private Optional<BlockPos> cropPos = Optional.empty();

   public UseBonemeal() {
      super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT));
   }

   protected boolean checkExtraStartConditions(ServerLevel p_24474_, Villager p_458848_) {
      if (p_458848_.tickCount % 10 == 0 && (this.lastBonemealingSession == 0L || this.lastBonemealingSession + 160L <= p_458848_.tickCount)) {
         if (p_458848_.getInventory().countItem(Items.BONE_MEAL) <= 0) {
            return false;
         }

         this.cropPos = this.pickNextTarget(p_24474_, p_458848_);
         return this.cropPos.isPresent();
      } else {
         return false;
      }
   }

   protected boolean canStillUse(ServerLevel p_24477_, Villager p_455041_, long p_24479_) {
      return this.timeWorkedSoFar < 80 && this.cropPos.isPresent();
   }

   private Optional<BlockPos> pickNextTarget(ServerLevel p_24493_, Villager p_451835_) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
      Optional<BlockPos> optional = Optional.empty();
      int i = 0;

      for (int j = -1; j <= 1; j++) {
         for (int k = -1; k <= 1; k++) {
            for (int l = -1; l <= 1; l++) {
               blockpos$mutableblockpos.setWithOffset(p_451835_.blockPosition(), j, k, l);
               if (this.validPos(blockpos$mutableblockpos, p_24493_)) {
                  if (p_24493_.random.nextInt(++i) == 0) {
                     optional = Optional.of(blockpos$mutableblockpos.immutable());
                  }
               }
            }
         }
      }

      return optional;
   }

   private boolean validPos(BlockPos p_24486_, ServerLevel p_24487_) {
      BlockState blockstate = p_24487_.getBlockState(p_24486_);
      Block block = blockstate.getBlock();
      return block instanceof CropBlock && !((CropBlock)block).isMaxAge(blockstate);
   }

   protected void start(ServerLevel p_24496_, Villager p_459025_, long p_24498_) {
      this.setCurrentCropAsTarget(p_459025_);
      p_459025_.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BONE_MEAL));
      this.nextWorkCycleTime = p_24498_;
      this.timeWorkedSoFar = 0;
   }

   private void setCurrentCropAsTarget(Villager p_460621_) {
      this.cropPos.ifPresent(p_449567_ -> {
         BlockPosTracker blockpostracker = new BlockPosTracker(p_449567_);
         p_460621_.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, blockpostracker);
         p_460621_.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(blockpostracker, 0.5F, 1));
      });
   }

   protected void stop(ServerLevel p_24504_, Villager p_457344_, long p_24506_) {
      p_457344_.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
      this.lastBonemealingSession = p_457344_.tickCount;
   }

   protected void tick(ServerLevel p_24512_, Villager p_457932_, long p_24514_) {
      BlockPos blockpos = this.cropPos.get();
      if (p_24514_ >= this.nextWorkCycleTime && blockpos.closerToCenterThan(p_457932_.position(), 1.0)) {
         ItemStack itemstack = ItemStack.EMPTY;
         SimpleContainer simplecontainer = p_457932_.getInventory();
         int i = simplecontainer.getContainerSize();

         for (int j = 0; j < i; j++) {
            ItemStack itemstack1 = simplecontainer.getItem(j);
            if (itemstack1.is(Items.BONE_MEAL)) {
               itemstack = itemstack1;
               break;
            }
         }

         if (!itemstack.isEmpty() && BoneMealItem.growCrop(itemstack, p_24512_, blockpos)) {
            p_24512_.levelEvent(1505, blockpos, 15);
            this.cropPos = this.pickNextTarget(p_24512_, p_457932_);
            this.setCurrentCropAsTarget(p_457932_);
            this.nextWorkCycleTime = p_24514_ + 40L;
         }

         this.timeWorkedSoFar++;
      }
   }
}
