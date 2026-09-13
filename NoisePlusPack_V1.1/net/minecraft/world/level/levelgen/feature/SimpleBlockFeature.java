package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.MossyCarpetBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;

public class SimpleBlockFeature extends Feature<SimpleBlockConfiguration> {
   public SimpleBlockFeature(Codec<SimpleBlockConfiguration> p_66808_) {
      super(p_66808_);
   }

   @Override
   public boolean place(FeaturePlaceContext<SimpleBlockConfiguration> p_160341_) {
      SimpleBlockConfiguration simpleblockconfiguration = p_160341_.config();
      WorldGenLevel worldgenlevel = p_160341_.level();
      BlockPos blockpos = p_160341_.origin();
      BlockState blockstate = simpleblockconfiguration.toPlace().getState(p_160341_.random(), blockpos);
      if (blockstate.canSurvive(worldgenlevel, blockpos)) {
         if (blockstate.getBlock() instanceof DoublePlantBlock) {
            if (!worldgenlevel.isEmptyBlock(blockpos.above())) {
               return false;
            }

            DoublePlantBlock.placeAt(worldgenlevel, blockstate, blockpos, 2);
         } else if (blockstate.getBlock() instanceof MossyCarpetBlock) {
            MossyCarpetBlock.placeAt(worldgenlevel, blockpos, worldgenlevel.getRandom(), 2);
         } else {
            worldgenlevel.setBlock(blockpos, blockstate, 2);
         }

         if (simpleblockconfiguration.scheduleTick()) {
            worldgenlevel.scheduleTick(blockpos, worldgenlevel.getBlockState(blockpos).getBlock(), 1);
         }

         return true;
      } else {
         return false;
      }
   }
}
