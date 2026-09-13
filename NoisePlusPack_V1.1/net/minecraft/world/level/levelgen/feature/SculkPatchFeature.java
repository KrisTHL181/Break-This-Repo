package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SculkBehaviour;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.SculkSpreader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.SculkPatchConfiguration;

public class SculkPatchFeature extends Feature<SculkPatchConfiguration> {
   public SculkPatchFeature(Codec<SculkPatchConfiguration> p_225237_) {
      super(p_225237_);
   }

   @Override
   public boolean place(FeaturePlaceContext<SculkPatchConfiguration> p_225242_) {
      WorldGenLevel worldgenlevel = p_225242_.level();
      BlockPos blockpos = p_225242_.origin();
      if (!this.canSpreadFrom(worldgenlevel, blockpos)) {
         return false;
      }

      SculkPatchConfiguration sculkpatchconfiguration = p_225242_.config();
      RandomSource randomsource = p_225242_.random();
      SculkSpreader sculkspreader = SculkSpreader.createWorldGenSpreader();
      int i = sculkpatchconfiguration.spreadRounds() + sculkpatchconfiguration.growthRounds();

      for (int j = 0; j < i; j++) {
         for (int k = 0; k < sculkpatchconfiguration.chargeCount(); k++) {
            sculkspreader.addCursors(blockpos, sculkpatchconfiguration.amountPerCharge());
         }

         boolean flag = j < sculkpatchconfiguration.spreadRounds();

         for (int l = 0; l < sculkpatchconfiguration.spreadAttempts(); l++) {
            sculkspreader.updateCursors(worldgenlevel, blockpos, randomsource, flag);
         }

         sculkspreader.clear();
      }

      BlockPos blockpos2 = blockpos.below();
      if (randomsource.nextFloat() <= sculkpatchconfiguration.catalystChance()
         && worldgenlevel.getBlockState(blockpos2).isCollisionShapeFullBlock(worldgenlevel, blockpos2)) {
         worldgenlevel.setBlock(blockpos, Blocks.SCULK_CATALYST.defaultBlockState(), 3);
      }

      int i1 = sculkpatchconfiguration.extraRareGrowths().sample(randomsource);

      for (int j1 = 0; j1 < i1; j1++) {
         BlockPos blockpos1 = blockpos.offset(randomsource.nextInt(5) - 2, 0, randomsource.nextInt(5) - 2);
         if (worldgenlevel.getBlockState(blockpos1).isAir()
            && worldgenlevel.getBlockState(blockpos1.below()).isFaceSturdy(worldgenlevel, blockpos1.below(), Direction.UP)) {
            worldgenlevel.setBlock(blockpos1, Blocks.SCULK_SHRIEKER.defaultBlockState().setValue(SculkShriekerBlock.CAN_SUMMON, true), 3);
         }
      }

      return true;
   }

   private boolean canSpreadFrom(LevelAccessor p_225239_, BlockPos p_225240_) {
      BlockState blockstate = p_225239_.getBlockState(p_225240_);
      if (blockstate.getBlock() instanceof SculkBehaviour) {
         return true;
      } else {
         return blockstate.isAir() || blockstate.is(Blocks.WATER) && blockstate.getFluidState().isSource()
            ? Direction.stream()
               .map(p_225240_::relative)
               .anyMatch(p_360609_ -> p_225239_.getBlockState(p_360609_).isCollisionShapeFullBlock(p_225239_, p_360609_))
            : false;
      }
   }
}
