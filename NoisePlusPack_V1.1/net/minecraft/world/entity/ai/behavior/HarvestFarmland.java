package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import org.jspecify.annotations.Nullable;

public class HarvestFarmland extends Behavior<Villager> {
   private static final int HARVEST_DURATION = 200;
   public static final float SPEED_MODIFIER = 0.5F;
   private @Nullable BlockPos aboveFarmlandPos;
   private long nextOkStartTime;
   private int timeWorkedSoFar;
   private final List<BlockPos> validFarmlandAroundVillager = Lists.newArrayList();

   public HarvestFarmland() {
      super(
         ImmutableMap.of(
            MemoryModuleType.LOOK_TARGET,
            MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.WALK_TARGET,
            MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.SECONDARY_JOB_SITE,
            MemoryStatus.VALUE_PRESENT
         )
      );
   }

   protected boolean checkExtraStartConditions(ServerLevel p_23174_, Villager p_460921_) {
      if (!p_23174_.getGameRules().get(GameRules.MOB_GRIEFING)) {
         return false;
      }

      if (!p_460921_.getVillagerData().profession().is(VillagerProfession.FARMER)) {
         return false;
      }

      BlockPos.MutableBlockPos blockpos$mutableblockpos = p_460921_.blockPosition().mutable();
      this.validFarmlandAroundVillager.clear();

      for (int i = -1; i <= 1; i++) {
         for (int j = -1; j <= 1; j++) {
            for (int k = -1; k <= 1; k++) {
               blockpos$mutableblockpos.set(p_460921_.getX() + i, p_460921_.getY() + j, p_460921_.getZ() + k);
               if (this.validPos(blockpos$mutableblockpos, p_23174_)) {
                  this.validFarmlandAroundVillager.add(new BlockPos(blockpos$mutableblockpos));
               }
            }
         }
      }

      this.aboveFarmlandPos = this.getValidFarmland(p_23174_);
      return this.aboveFarmlandPos != null;
   }

   private @Nullable BlockPos getValidFarmland(ServerLevel p_23165_) {
      return this.validFarmlandAroundVillager.isEmpty()
         ? null
         : this.validFarmlandAroundVillager.get(p_23165_.getRandom().nextInt(this.validFarmlandAroundVillager.size()));
   }

   private boolean validPos(BlockPos p_23181_, ServerLevel p_23182_) {
      BlockState blockstate = p_23182_.getBlockState(p_23181_);
      Block block = blockstate.getBlock();
      Block block1 = p_23182_.getBlockState(p_23181_.below()).getBlock();
      return block instanceof CropBlock && ((CropBlock)block).isMaxAge(blockstate) || blockstate.isAir() && block1 instanceof FarmBlock;
   }

   protected void start(ServerLevel p_23177_, Villager p_450495_, long p_23179_) {
      if (p_23179_ > this.nextOkStartTime && this.aboveFarmlandPos != null) {
         p_450495_.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(this.aboveFarmlandPos));
         p_450495_.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new BlockPosTracker(this.aboveFarmlandPos), 0.5F, 1));
      }
   }

   protected void stop(ServerLevel p_23188_, Villager p_453577_, long p_23190_) {
      p_453577_.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
      p_453577_.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
      this.timeWorkedSoFar = 0;
      this.nextOkStartTime = p_23190_ + 40L;
   }

   protected void tick(ServerLevel p_23196_, Villager p_457898_, long p_23198_) {
      if (this.aboveFarmlandPos == null || this.aboveFarmlandPos.closerToCenterThan(p_457898_.position(), 1.0)) {
         if (this.aboveFarmlandPos != null && p_23198_ > this.nextOkStartTime) {
            BlockState blockstate = p_23196_.getBlockState(this.aboveFarmlandPos);
            Block block = blockstate.getBlock();
            Block block1 = p_23196_.getBlockState(this.aboveFarmlandPos.below()).getBlock();
            if (block instanceof CropBlock && ((CropBlock)block).isMaxAge(blockstate)) {
               p_23196_.destroyBlock(this.aboveFarmlandPos, true, p_457898_);
            }

            if (blockstate.isAir() && block1 instanceof FarmBlock && p_457898_.hasFarmSeeds()) {
               SimpleContainer simplecontainer = p_457898_.getInventory();

               for (int i = 0; i < simplecontainer.getContainerSize(); i++) {
                  ItemStack itemstack = simplecontainer.getItem(i);
                  boolean flag = false;
                  if (!itemstack.isEmpty() && itemstack.is(ItemTags.VILLAGER_PLANTABLE_SEEDS) && itemstack.getItem() instanceof BlockItem blockitem) {
                     BlockState blockstate1 = blockitem.getBlock().defaultBlockState();
                     p_23196_.setBlockAndUpdate(this.aboveFarmlandPos, blockstate1);
                     p_23196_.gameEvent(GameEvent.BLOCK_PLACE, this.aboveFarmlandPos, GameEvent.Context.of(p_457898_, blockstate1));
                     flag = true;
                  }

                  if (flag) {
                     p_23196_.playSound(
                        null,
                        this.aboveFarmlandPos.getX(),
                        this.aboveFarmlandPos.getY(),
                        this.aboveFarmlandPos.getZ(),
                        SoundEvents.CROP_PLANTED,
                        SoundSource.BLOCKS,
                        1.0F,
                        1.0F
                     );
                     itemstack.shrink(1);
                     if (itemstack.isEmpty()) {
                        simplecontainer.setItem(i, ItemStack.EMPTY);
                     }
                     break;
                  }
               }
            }

            if (block instanceof CropBlock && !((CropBlock)block).isMaxAge(blockstate)) {
               this.validFarmlandAroundVillager.remove(this.aboveFarmlandPos);
               this.aboveFarmlandPos = this.getValidFarmland(p_23196_);
               if (this.aboveFarmlandPos != null) {
                  this.nextOkStartTime = p_23198_ + 20L;
                  p_457898_.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new BlockPosTracker(this.aboveFarmlandPos), 0.5F, 1));
                  p_457898_.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(this.aboveFarmlandPos));
               }
            }
         }

         this.timeWorkedSoFar++;
      }
   }

   protected boolean canStillUse(ServerLevel p_23204_, Villager p_453116_, long p_23206_) {
      return this.timeWorkedSoFar < 200;
   }
}
