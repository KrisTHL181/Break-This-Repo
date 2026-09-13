package net.minecraft.world.entity.ai.goal;

import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import org.apache.commons.lang3.function.TriFunction;

public class CatSitOnBlockGoal extends MoveToBlockGoal {
   private final Cat cat;
   private static final List<Pair<Predicate<BlockState>, TriFunction<BlockState, LevelReader, BlockPos, Boolean>>> BLOCK_TYPE_VALIDATION = List.of(
      Pair.of(
         (Predicate<BlockState>)blockState -> blockState.is(Blocks.CHEST),
         (TriFunction)(blockState, level, pos) -> ChestBlockEntity.getOpenCount(level, pos) < 1
      ),
      Pair.of(
         (Predicate<BlockState>)blockState -> blockState.is(Blocks.FURNACE), (TriFunction)(blockState, level, pos) -> blockState.getValue(FurnaceBlock.LIT)
      ),
      Pair.of(
         (Predicate<BlockState>)blockState -> blockState.is(BlockTags.BEDS),
         (TriFunction)(blockState, level, pos) -> blockState.getOptionalValue(BedBlock.PART).map(v -> v != BedPart.HEAD).orElse(true)
      )
   );

   public CatSitOnBlockGoal(final Cat cat, final double speedModifier) {
      super(cat, speedModifier, 8);
      this.cat = cat;
   }

   @Override
   public boolean canUse() {
      return this.cat.isTame() && !this.cat.isOrderedToSit() && super.canUse();
   }

   @Override
   public void start() {
      super.start();
      this.cat.setInSittingPose(false);
   }

   @Override
   public void stop() {
      super.stop();
      this.cat.setInSittingPose(false);
   }

   @Override
   public void tick() {
      super.tick();
      this.cat.setInSittingPose(this.isReachedTarget());
   }

   @Override
   protected boolean isValidTarget(final LevelReader level, final BlockPos pos) {
      if (!level.isEmptyBlock(pos.above())) {
         return false;
      }

      BlockState blockState = level.getBlockState(pos);
      if (!blockState.is(BlockTags.CATS_CAN_SIT_ON)) {
         return false;
      }

      for (Pair<Predicate<BlockState>, TriFunction<BlockState, LevelReader, BlockPos, Boolean>> blockValidation : BLOCK_TYPE_VALIDATION) {
         if (((Predicate)blockValidation.getFirst()).test(blockState)) {
            return (Boolean)((TriFunction)blockValidation.getSecond()).apply(blockState, level, pos);
         }
      }

      return true;
   }
}
