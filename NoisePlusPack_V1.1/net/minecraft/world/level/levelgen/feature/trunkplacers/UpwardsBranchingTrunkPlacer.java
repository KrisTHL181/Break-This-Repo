package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;

public class UpwardsBranchingTrunkPlacer extends TrunkPlacer {
   public static final MapCodec<UpwardsBranchingTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(
      p_259008_ -> trunkPlacerParts(p_259008_)
         .and(
            p_259008_.group(
               IntProvider.POSITIVE_CODEC.fieldOf("extra_branch_steps").forGetter(p_226242_ -> p_226242_.extraBranchSteps),
               Codec.floatRange(0.0F, 1.0F).fieldOf("place_branch_per_log_probability").forGetter(p_226240_ -> p_226240_.placeBranchPerLogProbability),
               IntProvider.NON_NEGATIVE_CODEC.fieldOf("extra_branch_length").forGetter(p_226238_ -> p_226238_.extraBranchLength),
               RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("can_grow_through").forGetter(p_226234_ -> p_226234_.canGrowThrough)
            )
         )
         .apply(p_259008_, UpwardsBranchingTrunkPlacer::new)
   );
   private final IntProvider extraBranchSteps;
   private final float placeBranchPerLogProbability;
   private final IntProvider extraBranchLength;
   private final HolderSet<Block> canGrowThrough;

   public UpwardsBranchingTrunkPlacer(
      int p_226201_, int p_226202_, int p_226203_, IntProvider p_226204_, float p_226205_, IntProvider p_226206_, HolderSet<Block> p_226207_
   ) {
      super(p_226201_, p_226202_, p_226203_);
      this.extraBranchSteps = p_226204_;
      this.placeBranchPerLogProbability = p_226205_;
      this.extraBranchLength = p_226206_;
      this.canGrowThrough = p_226207_;
   }

   @Override
   protected TrunkPlacerType<?> type() {
      return TrunkPlacerType.UPWARDS_BRANCHING_TRUNK_PLACER;
   }

   @Override
   public List<FoliagePlacer.FoliageAttachment> placeTrunk(
      LevelSimulatedReader p_226225_,
      BiConsumer<BlockPos, BlockState> p_226226_,
      RandomSource p_226227_,
      int p_226228_,
      BlockPos p_226229_,
      TreeConfiguration p_226230_
   ) {
      List<FoliagePlacer.FoliageAttachment> list = Lists.newArrayList();
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

      for (int i = 0; i < p_226228_; i++) {
         int j = p_226229_.getY() + i;
         if (this.placeLog(p_226225_, p_226226_, p_226227_, blockpos$mutableblockpos.set(p_226229_.getX(), j, p_226229_.getZ()), p_226230_)
            && i < p_226228_ - 1
            && p_226227_.nextFloat() < this.placeBranchPerLogProbability) {
            Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(p_226227_);
            int k = this.extraBranchLength.sample(p_226227_);
            int l = Math.max(0, k - this.extraBranchLength.sample(p_226227_) - 1);
            int i1 = this.extraBranchSteps.sample(p_226227_);
            this.placeBranch(p_226225_, p_226226_, p_226227_, p_226228_, p_226230_, list, blockpos$mutableblockpos, j, direction, l, i1);
         }

         if (i == p_226228_ - 1) {
            list.add(new FoliagePlacer.FoliageAttachment(blockpos$mutableblockpos.set(p_226229_.getX(), j + 1, p_226229_.getZ()), 0, false));
         }
      }

      return list;
   }

   private void placeBranch(
      LevelSimulatedReader p_226213_,
      BiConsumer<BlockPos, BlockState> p_226214_,
      RandomSource p_226215_,
      int p_226216_,
      TreeConfiguration p_226217_,
      List<FoliagePlacer.FoliageAttachment> p_226218_,
      BlockPos.MutableBlockPos p_226219_,
      int p_226220_,
      Direction p_226221_,
      int p_226222_,
      int p_226223_
   ) {
      int i = p_226220_ + p_226222_;
      int j = p_226219_.getX();
      int k = p_226219_.getZ();
      int l = p_226222_;

      while (l < p_226216_ && p_226223_ > 0) {
         if (l >= 1) {
            int i1 = p_226220_ + l;
            j += p_226221_.getStepX();
            k += p_226221_.getStepZ();
            i = i1;
            if (this.placeLog(p_226213_, p_226214_, p_226215_, p_226219_.set(j, i1, k), p_226217_)) {
               i = i1 + 1;
            }

            p_226218_.add(new FoliagePlacer.FoliageAttachment(p_226219_.immutable(), 0, false));
         }

         l++;
         p_226223_--;
      }

      if (i - p_226220_ > 1) {
         BlockPos blockpos = new BlockPos(j, i, k);
         p_226218_.add(new FoliagePlacer.FoliageAttachment(blockpos, 0, false));
         p_226218_.add(new FoliagePlacer.FoliageAttachment(blockpos.below(2), 0, false));
      }
   }

   @Override
   protected boolean validTreePos(LevelSimulatedReader p_226210_, BlockPos p_226211_) {
      return super.validTreePos(p_226210_, p_226211_) || p_226210_.isStateAtPosition(p_226211_, p_226232_ -> p_226232_.is(this.canGrowThrough));
   }
}
