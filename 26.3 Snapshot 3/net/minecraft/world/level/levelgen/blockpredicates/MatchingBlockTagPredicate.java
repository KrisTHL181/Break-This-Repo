package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class MatchingBlockTagPredicate extends StateTestingPredicate {
   public static final MapCodec<MatchingBlockTagPredicate> CODEC = RecordCodecBuilder.mapCodec(
      i -> stateTestingCodec(i).and(TagKey.codec(Registries.BLOCK).fieldOf("tag").forGetter(c -> c.tag)).apply(i, MatchingBlockTagPredicate::new)
   );
   private final TagKey<Block> tag;

   protected MatchingBlockTagPredicate(final Vec3i offset, final TagKey<Block> tag) {
      super(offset);
      this.tag = tag;
   }

   @Override
   protected boolean test(final BlockState state) {
      return state.is(this.tag);
   }

   @Override
   public BlockPredicateType<?> type() {
      return BlockPredicateType.MATCHING_BLOCK_TAG;
   }
}
