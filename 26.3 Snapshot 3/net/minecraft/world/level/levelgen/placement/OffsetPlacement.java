package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.util.valueproviders.TrapezoidInt;

public record OffsetPlacement(IntProvider x, IntProvider y, IntProvider z) implements PlacementModifier {
   public static final MapCodec<OffsetPlacement> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            IntProviders.codec(-16, 16).fieldOf("x").forGetter(OffsetPlacement::x),
            IntProviders.codec(-16, 16).fieldOf("y").forGetter(OffsetPlacement::y),
            IntProviders.codec(-16, 16).fieldOf("z").forGetter(OffsetPlacement::z)
         )
         .apply(i, OffsetPlacement::new)
   );

   public static OffsetPlacement of(final IntProvider xzSpread, final IntProvider ySpread) {
      return new OffsetPlacement(xzSpread, ySpread, xzSpread);
   }

   public static OffsetPlacement ofTriangle(final int xzRange, final int yRange) {
      return of(TrapezoidInt.triangle(xzRange), TrapezoidInt.triangle(yRange));
   }

   public static OffsetPlacement vertical(final IntProvider ySpread) {
      return of(ConstantInt.of(0), ySpread);
   }

   public static OffsetPlacement horizontal(final IntProvider xzSpread) {
      return of(xzSpread, ConstantInt.of(0));
   }

   public static OffsetPlacement of(final int x, final int y, final int z) {
      return new OffsetPlacement(ConstantInt.of(x), ConstantInt.of(y), ConstantInt.of(z));
   }

   public static OffsetPlacement of(final Direction direction) {
      return of(direction.getStepX(), direction.getStepY(), direction.getStepZ());
   }

   public static OffsetPlacement above() {
      return of(0, 1, 0);
   }

   @Override
   public void modify(final PlacementContext context, final RandomSource random, final BlockPos origin, final Consumer<BlockPos> output) {
      output.accept(origin.offset(this.x.sample(random), this.y.sample(random), this.z.sample(random)));
   }

   @Override
   public MapCodec<OffsetPlacement> codec() {
      return CODEC;
   }
}
