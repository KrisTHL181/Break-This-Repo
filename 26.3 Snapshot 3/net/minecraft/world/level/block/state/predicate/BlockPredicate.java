package net.minecraft.world.level.block.state.predicate;

import java.util.function.Predicate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class BlockPredicate implements Predicate<BlockState> {
   private final Block block;

   public BlockPredicate(final Block block) {
      this.block = block;
   }

   public static BlockPredicate forBlock(final Block block) {
      return new BlockPredicate(block);
   }

   public boolean test(final @Nullable BlockState input) {
      return input != null && input.is(this.block);
   }
}
