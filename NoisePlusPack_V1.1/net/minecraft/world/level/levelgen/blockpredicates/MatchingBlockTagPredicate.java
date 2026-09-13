package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class MatchingBlockTagPredicate extends StateTestingPredicate {
   final TagKey<Block> tag;
   public static final MapCodec<MatchingBlockTagPredicate> CODEC = RecordCodecBuilder.mapCodec(
      p_259003_ -> stateTestingCodec(p_259003_)
         .and(TagKey.codec(Registries.BLOCK).fieldOf("tag").forGetter(p_204686_ -> p_204686_.tag))
         .apply(p_259003_, MatchingBlockTagPredicate::new)
   );

   protected MatchingBlockTagPredicate(Vec3i p_204683_, TagKey<Block> p_204684_) {
      super(p_204683_);
      this.tag = p_204684_;
   }

   @Override
   protected boolean test(BlockState p_198343_) {
      return p_198343_.is(this.tag);
   }

   @Override
   public BlockPredicateType<?> type() {
      return BlockPredicateType.MATCHING_BLOCK_TAG;
   }
}
