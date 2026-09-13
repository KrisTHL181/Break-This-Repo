package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.VegetationPatchConfiguration;

public class VegetationPatchFeature extends Feature<VegetationPatchConfiguration> {
   public VegetationPatchFeature(Codec<VegetationPatchConfiguration> p_160588_) {
      super(p_160588_);
   }

   @Override
   public boolean place(FeaturePlaceContext<VegetationPatchConfiguration> p_160612_) {
      WorldGenLevel worldgenlevel = p_160612_.level();
      VegetationPatchConfiguration vegetationpatchconfiguration = p_160612_.config();
      RandomSource randomsource = p_160612_.random();
      BlockPos blockpos = p_160612_.origin();
      Predicate<BlockState> predicate = p_204782_ -> p_204782_.is(vegetationpatchconfiguration.replaceable);
      int i = vegetationpatchconfiguration.xzRadius.sample(randomsource) + 1;
      int j = vegetationpatchconfiguration.xzRadius.sample(randomsource) + 1;
      Set<BlockPos> set = this.placeGroundPatch(worldgenlevel, vegetationpatchconfiguration, randomsource, blockpos, predicate, i, j);
      this.distributeVegetation(p_160612_, worldgenlevel, vegetationpatchconfiguration, randomsource, set, i, j);
      return !set.isEmpty();
   }

   protected Set<BlockPos> placeGroundPatch(
      WorldGenLevel p_225311_,
      VegetationPatchConfiguration p_225312_,
      RandomSource p_225313_,
      BlockPos p_225314_,
      Predicate<BlockState> p_225315_,
      int p_225316_,
      int p_225317_
   ) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = p_225314_.mutable();
      BlockPos.MutableBlockPos blockpos$mutableblockpos1 = blockpos$mutableblockpos.mutable();
      Direction direction = p_225312_.surface.getDirection();
      Direction direction1 = direction.getOpposite();
      Set<BlockPos> set = new HashSet<>();

      for (int i = -p_225316_; i <= p_225316_; i++) {
         boolean flag = i == -p_225316_ || i == p_225316_;

         for (int j = -p_225317_; j <= p_225317_; j++) {
            boolean flag1 = j == -p_225317_ || j == p_225317_;
            boolean flag2 = flag || flag1;
            boolean flag3 = flag && flag1;
            boolean flag4 = flag2 && !flag3;
            if (!flag3 && (!flag4 || p_225312_.extraEdgeColumnChance != 0.0F && !(p_225313_.nextFloat() > p_225312_.extraEdgeColumnChance))) {
               blockpos$mutableblockpos.setWithOffset(p_225314_, i, 0, j);

               for (int k = 0; p_225311_.isStateAtPosition(blockpos$mutableblockpos, BlockBehaviour.BlockStateBase::isAir) && k < p_225312_.verticalRange; k++) {
                  blockpos$mutableblockpos.move(direction);
               }

               for (int i1 = 0; p_225311_.isStateAtPosition(blockpos$mutableblockpos, p_360612_ -> !p_360612_.isAir()) && i1 < p_225312_.verticalRange; i1++) {
                  blockpos$mutableblockpos.move(direction1);
               }

               blockpos$mutableblockpos1.setWithOffset(blockpos$mutableblockpos, p_225312_.surface.getDirection());
               BlockState blockstate = p_225311_.getBlockState(blockpos$mutableblockpos1);
               if (p_225311_.isEmptyBlock(blockpos$mutableblockpos)
                  && blockstate.isFaceSturdy(p_225311_, blockpos$mutableblockpos1, p_225312_.surface.getDirection().getOpposite())) {
                  int l = p_225312_.depth.sample(p_225313_)
                     + (p_225312_.extraBottomBlockChance > 0.0F && p_225313_.nextFloat() < p_225312_.extraBottomBlockChance ? 1 : 0);
                  BlockPos blockpos = blockpos$mutableblockpos1.immutable();
                  boolean flag5 = this.placeGround(p_225311_, p_225312_, p_225315_, p_225313_, blockpos$mutableblockpos1, l);
                  if (flag5) {
                     set.add(blockpos);
                  }
               }
            }
         }
      }

      return set;
   }

   protected void distributeVegetation(
      FeaturePlaceContext<VegetationPatchConfiguration> p_225331_,
      WorldGenLevel p_225332_,
      VegetationPatchConfiguration p_225333_,
      RandomSource p_225334_,
      Set<BlockPos> p_225335_,
      int p_225336_,
      int p_225337_
   ) {
      for (BlockPos blockpos : p_225335_) {
         if (p_225333_.vegetationChance > 0.0F && p_225334_.nextFloat() < p_225333_.vegetationChance) {
            this.placeVegetation(p_225332_, p_225333_, p_225331_.chunkGenerator(), p_225334_, blockpos);
         }
      }
   }

   protected boolean placeVegetation(
      WorldGenLevel p_225318_, VegetationPatchConfiguration p_225319_, ChunkGenerator p_225320_, RandomSource p_225321_, BlockPos p_225322_
   ) {
      return p_225319_.vegetationFeature.value().place(p_225318_, p_225320_, p_225321_, p_225322_.relative(p_225319_.surface.getDirection().getOpposite()));
   }

   protected boolean placeGround(
      WorldGenLevel p_225324_,
      VegetationPatchConfiguration p_225325_,
      Predicate<BlockState> p_225326_,
      RandomSource p_225327_,
      BlockPos.MutableBlockPos p_225328_,
      int p_225329_
   ) {
      for (int i = 0; i < p_225329_; i++) {
         BlockState blockstate = p_225325_.groundState.getState(p_225327_, p_225328_);
         BlockState blockstate1 = p_225324_.getBlockState(p_225328_);
         if (!blockstate.is(blockstate1.getBlock())) {
            if (!p_225326_.test(blockstate1)) {
               return i != 0;
            }

            p_225324_.setBlock(p_225328_, blockstate, 2);
            p_225328_.move(p_225325_.surface.getDirection());
         }
      }

      return true;
   }
}
