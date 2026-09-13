package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.heightproviders.TrapezoidHeight;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;

public record HeightRangePlacement(HeightProvider height) implements PlacementModifier {
   public static final MapCodec<HeightRangePlacement> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(HeightProvider.CODEC.fieldOf("height").forGetter(HeightRangePlacement::height)).apply(i, HeightRangePlacement::new)
   );

   public static HeightRangePlacement of(final HeightProvider height) {
      return new HeightRangePlacement(height);
   }

   public static HeightRangePlacement uniform(final VerticalAnchor minInclusive, final VerticalAnchor maxInclusive) {
      return of(UniformHeight.of(minInclusive, maxInclusive));
   }

   public static HeightRangePlacement triangle(final VerticalAnchor minInclusive, final VerticalAnchor maxInclusive) {
      return of(TrapezoidHeight.of(minInclusive, maxInclusive));
   }

   @Override
   public void modify(final PlacementContext context, final RandomSource random, final BlockPos origin, final Consumer<BlockPos> output) {
      output.accept(origin.atY(this.height.sample(random, context)));
   }

   @Override
   public MapCodec<HeightRangePlacement> codec() {
      return CODEC;
   }
}
