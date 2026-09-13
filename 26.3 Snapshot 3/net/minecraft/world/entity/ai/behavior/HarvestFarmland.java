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
import net.minecraft.world.level.block.FarmlandBlock;
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

   protected boolean checkExtraStartConditions(final ServerLevel level, final Villager body) {
      if (!level.getGameRules().get(GameRules.MOB_GRIEFING)) {
         return false;
      }

      if (!body.getVillagerData().profession().is(VillagerProfession.FARMER)) {
         return false;
      }

      BlockPos.MutableBlockPos mutPos = body.blockPosition().mutable();
      this.validFarmlandAroundVillager.clear();

      for (int x = -1; x <= 1; x++) {
         for (int y = -1; y <= 1; y++) {
            for (int z = -1; z <= 1; z++) {
               mutPos.set(body.getX() + x, body.getY() + y, body.getZ() + z);
               if (this.validPos(mutPos, level)) {
                  this.validFarmlandAroundVillager.add(mutPos.immutable());
               }
            }
         }
      }

      this.aboveFarmlandPos = this.getValidFarmland(level);
      return this.aboveFarmlandPos != null;
   }

   private @Nullable BlockPos getValidFarmland(final ServerLevel level) {
      return this.validFarmlandAroundVillager.isEmpty()
         ? null
         : this.validFarmlandAroundVillager.get(level.getRandom().nextInt(this.validFarmlandAroundVillager.size()));
   }

   private boolean validPos(final BlockPos blockPos, final ServerLevel level) {
      BlockState state = level.getBlockState(blockPos);
      Block block = state.getBlock();
      Block blockBelow = level.getBlockState(blockPos.below()).getBlock();
      return block instanceof CropBlock cropBlock && cropBlock.isMaxAge(state) || state.isAir() && blockBelow instanceof FarmlandBlock;
   }

   protected void start(final ServerLevel level, final Villager body, final long timestamp) {
      if (timestamp > this.nextOkStartTime && this.aboveFarmlandPos != null) {
         body.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(this.aboveFarmlandPos));
         body.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new BlockPosTracker(this.aboveFarmlandPos), 0.5F, 1));
      }
   }

   protected void stop(final ServerLevel level, final Villager body, final long timestamp) {
      body.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
      body.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
      this.timeWorkedSoFar = 0;
      this.nextOkStartTime = timestamp + 40L;
   }

   protected void tick(final ServerLevel level, final Villager body, final long timestamp) {
      if (this.aboveFarmlandPos == null || this.aboveFarmlandPos.closerToCenterThan(body.position(), 1.0)) {
         if (this.aboveFarmlandPos != null && timestamp > this.nextOkStartTime) {
            BlockState blockState = level.getBlockState(this.aboveFarmlandPos);
            Block block = blockState.getBlock();
            Block blockBelow = level.getBlockState(this.aboveFarmlandPos.below()).getBlock();
            if (block instanceof CropBlock cropBlock && cropBlock.isMaxAge(blockState)) {
               level.destroyBlock(this.aboveFarmlandPos, true, body);
            }

            if (blockState.isAir() && blockBelow instanceof FarmlandBlock && body.hasFarmSeeds()) {
               SimpleContainer inventory = body.getInventory();

               for (int i = 0; i < inventory.getContainerSize(); i++) {
                  ItemStack itemStack = inventory.getItem(i);
                  boolean ok = false;
                  if (!itemStack.isEmpty() && itemStack.is(ItemTags.VILLAGER_PLANTABLE_SEEDS) && itemStack.getItem() instanceof BlockItem blockItem) {
                     BlockState place = blockItem.getBlock().defaultBlockState();
                     level.setBlockAndUpdate(this.aboveFarmlandPos, place);
                     level.gameEvent(GameEvent.BLOCK_PLACE, this.aboveFarmlandPos, GameEvent.Context.of(body, place));
                     ok = true;
                  }

                  if (ok) {
                     level.playSound(
                        null,
                        this.aboveFarmlandPos.getX(),
                        this.aboveFarmlandPos.getY(),
                        this.aboveFarmlandPos.getZ(),
                        SoundEvents.CROP_PLANTED,
                        SoundSource.BLOCKS,
                        1.0F,
                        1.0F
                     );
                     itemStack.shrink(1);
                     if (itemStack.isEmpty()) {
                        inventory.setItem(i, ItemStack.EMPTY);
                     }
                     break;
                  }
               }
            }

            if (block instanceof CropBlock cropBlock && !cropBlock.isMaxAge(blockState)) {
               this.validFarmlandAroundVillager.remove(this.aboveFarmlandPos);
               this.aboveFarmlandPos = this.getValidFarmland(level);
               if (this.aboveFarmlandPos != null) {
                  this.nextOkStartTime = timestamp + 20L;
                  body.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new BlockPosTracker(this.aboveFarmlandPos), 0.5F, 1));
                  body.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(this.aboveFarmlandPos));
               }
            }
         }

         this.timeWorkedSoFar++;
      }
   }

   protected boolean canStillUse(final ServerLevel level, final Villager body, final long timestamp) {
      return this.timeWorkedSoFar < 200;
   }
}
