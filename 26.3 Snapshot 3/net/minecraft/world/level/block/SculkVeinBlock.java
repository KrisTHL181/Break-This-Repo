package net.minecraft.world.level.block;

import java.util.Collection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public class SculkVeinBlock extends MultifaceSpreadeableBlock implements SculkBehaviour {
   private final MultifaceSpreader veinSpreader = new MultifaceSpreader(new SculkVeinBlock.SculkVeinSpreaderConfig(MultifaceSpreader.DEFAULT_SPREAD_ORDER));
   private final MultifaceSpreader sameSpaceSpreader = new MultifaceSpreader(
      new SculkVeinBlock.SculkVeinSpreaderConfig(MultifaceSpreader.SpreadType.SAME_POSITION)
   );

   public SculkVeinBlock(final BlockBehaviour.Properties properties) {
      super(properties);
   }

   @Override
   public MultifaceSpreader getSpreader() {
      return this.veinSpreader;
   }

   public MultifaceSpreader getSameSpaceSpreader() {
      return this.sameSpaceSpreader;
   }

   public static boolean regrow(final LevelAccessor level, final BlockPos pos, final BlockState existing, final Collection<Direction> faces) {
      boolean hasAtLeastOneFace = false;
      BlockState newState = Blocks.SCULK_VEIN.defaultBlockState();

      for (Direction face : faces) {
         if (canAttachTo(level, pos, face)) {
            newState = newState.setValue(getFaceProperty(face), true);
            hasAtLeastOneFace = true;
         }
      }

      if (!hasAtLeastOneFace) {
         return false;
      }

      if (!existing.getFluidState().isEmpty()) {
         newState = newState.setValue(MultifaceBlock.WATERLOGGED, true);
      }

      level.setBlockAndUpdate(pos, newState);
      return true;
   }

   @Override
   public void onDischarged(final LevelAccessor level, BlockState state, final BlockPos pos, final RandomSource random) {
      if (state.is(this)) {
         for (Direction dir : DIRECTIONS) {
            BooleanProperty sideProperty = getFaceProperty(dir);
            if (state.getValue(sideProperty) && level.getBlockState(pos.relative(dir)).is(Blocks.SCULK)) {
               state = state.setValue(sideProperty, false);
            }
         }

         if (!hasAnyFace(state)) {
            FluidState fluidState = level.getFluidState(pos);
            state = (fluidState.isEmpty() ? Blocks.AIR : Blocks.WATER).defaultBlockState();
         }

         level.setBlockAndUpdate(pos, state);
         SculkBehaviour.super.onDischarged(level, state, pos, random);
      }
   }

   @Override
   public int attemptUseCharge(
      final SculkSpreader.ChargeCursor cursor,
      final LevelAccessor level,
      final BlockPos originPos,
      final RandomSource random,
      final SculkSpreader spreader,
      final boolean spreadVeins
   ) {
      if (spreadVeins && this.attemptPlaceSculk(spreader, level, cursor.getPos(), random)) {
         return cursor.getCharge() - 1;
      } else {
         return random.nextInt(spreader.chargeDecayRate()) == 0 ? Mth.floor(cursor.getCharge() * 0.5F) : cursor.getCharge();
      }
   }

   private boolean attemptPlaceSculk(final SculkSpreader spreader, final LevelAccessor level, final BlockPos pos, final RandomSource random) {
      BlockState state = level.getBlockState(pos);
      TagKey<Block> replaceTag = spreader.replaceableBlocks();

      for (Direction support : Direction.allShuffled(random)) {
         if (hasFace(state, support)) {
            BlockPos supportPos = pos.relative(support);
            BlockState supportState = level.getBlockState(supportPos);
            if (supportState.is(replaceTag)) {
               BlockState defaultSculk = Blocks.SCULK.defaultBlockState();
               level.setBlockAndUpdate(supportPos, defaultSculk);
               Block.pushEntitiesUp(supportState, defaultSculk, level, supportPos);
               level.playSound(null, supportPos, SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.BLOCKS, 1.0F, 1.0F);
               this.veinSpreader.spreadAll(defaultSculk, level, supportPos, spreader.isWorldGeneration());
               Direction skip = support.getOpposite();

               for (Direction veinBlocks : DIRECTIONS) {
                  if (veinBlocks != skip) {
                     BlockPos veinPos = supportPos.relative(veinBlocks);
                     BlockState possibleVeinBlock = level.getBlockState(veinPos);
                     if (possibleVeinBlock.is(this)) {
                        this.onDischarged(level, possibleVeinBlock, veinPos, random);
                     }
                  }
               }

               return true;
            }
         }
      }

      return false;
   }

   public static boolean hasSubstrateAccess(final LevelAccessor level, final BlockState state, final BlockPos pos) {
      if (!state.is(Blocks.SCULK_VEIN)) {
         return false;
      }

      for (Direction direction : DIRECTIONS) {
         if (hasFace(state, direction) && level.getBlockState(pos.relative(direction)).is(BlockTags.SCULK_REPLACEABLE)) {
            return true;
         }
      }

      return false;
   }

   private class SculkVeinSpreaderConfig extends MultifaceSpreader.DefaultSpreaderConfig {
      private final MultifaceSpreader.SpreadType[] spreadTypes;

      public SculkVeinSpreaderConfig(final MultifaceSpreader.SpreadType... spreadTypes) {
         super(SculkVeinBlock.this);
         this.spreadTypes = spreadTypes;
      }

      @Override
      public boolean stateCanBeReplaced(
         final BlockGetter level, final BlockPos sourcePos, final BlockPos placementPos, final Direction placementDirection, final BlockState existingState
      ) {
         BlockState againstState = level.getBlockState(placementPos.relative(placementDirection));
         if (!againstState.is(Blocks.SCULK) && !againstState.is(Blocks.SCULK_CATALYST) && !againstState.is(Blocks.MOVING_PISTON)) {
            if (sourcePos.distManhattan(placementPos) == 2) {
               BlockPos neighourPos = sourcePos.relative(placementDirection.getOpposite());
               if (level.getBlockState(neighourPos).isFaceSturdy(level, neighourPos, placementDirection)) {
                  return false;
               }
            }

            FluidState fluidState = existingState.getFluidState();
            if (!fluidState.isEmpty() && !fluidState.is(Fluids.WATER)) {
               return false;
            } else {
               return existingState.is(BlockTags.FIRE)
                  ? false
                  : existingState.canBeReplaced() || super.stateCanBeReplaced(level, sourcePos, placementPos, placementDirection, existingState);
            }
         } else {
            return false;
         }
      }

      @Override
      public MultifaceSpreader.SpreadType[] getSpreadTypes() {
         return this.spreadTypes;
      }

      @Override
      public boolean isOtherBlockValidAsSource(final BlockState state) {
         return !state.is(Blocks.SCULK_VEIN);
      }
   }
}
