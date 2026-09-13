package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;

public class InSquarePlacement implements PlacementModifier {
   private static final InSquarePlacement INSTANCE = new InSquarePlacement();
   public static final MapCodec<InSquarePlacement> CODEC = MapCodec.unit(() -> INSTANCE);

   public static InSquarePlacement spread() {
      return INSTANCE;
   }

   @Override
   public void modify(final PlacementContext context, final RandomSource random, final BlockPos origin, final Consumer<BlockPos> output) {
      int x = random.nextInt(16) + origin.getX();
      int z = random.nextInt(16) + origin.getZ();
      output.accept(new BlockPos(x, origin.getY(), z));
   }

   @Override
   public MapCodec<InSquarePlacement> codec() {
      return CODEC;
   }
}
