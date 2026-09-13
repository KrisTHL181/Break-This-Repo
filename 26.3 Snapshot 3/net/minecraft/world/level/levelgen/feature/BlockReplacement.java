package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

public record BlockReplacement(RuleTest target, BlockState state) {
   public static final Codec<BlockReplacement> CODEC = RecordCodecBuilder.create(
      i -> i.group(RuleTest.CODEC.fieldOf("target").forGetter(c -> c.target), BlockState.CODEC.fieldOf("state").forGetter(c -> c.state))
         .apply(i, BlockReplacement::new)
   );

   public static BlockReplacement replace(final RuleTest rule, final BlockState state) {
      return new BlockReplacement(rule, state);
   }
}
