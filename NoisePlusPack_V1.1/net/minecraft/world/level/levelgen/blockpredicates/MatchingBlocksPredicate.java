package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

class MatchingBlocksPredicate extends StateTestingPredicate {
   private final HolderSet<Block> blocks;
   public static final MapCodec<MatchingBlocksPredicate> CODEC = RecordCodecBuilder.mapCodec(
      p_259004_ -> stateTestingCodec(p_259004_)
         .and(RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("blocks").forGetter(p_204693_ -> p_204693_.blocks))
         .apply(p_259004_, MatchingBlocksPredicate::new)
   );

   public MatchingBlocksPredicate(Vec3i p_204690_, HolderSet<Block> p_204691_) {
      super(p_204690_);
      this.blocks = p_204691_;
   }

   @Override
   protected boolean test(BlockState p_190487_) {
      return p_190487_.is(this.blocks);
   }

   @Override
   public BlockPredicateType<?> type() {
      return BlockPredicateType.MATCHING_BLOCKS;
   }
}
