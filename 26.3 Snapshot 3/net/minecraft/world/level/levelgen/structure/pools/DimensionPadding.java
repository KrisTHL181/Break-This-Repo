package net.minecraft.world.level.levelgen.structure.pools;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Function;
import net.minecraft.util.ExtraCodecs;

public record DimensionPadding(int bottom, int top) {
   private static final Codec<DimensionPadding> RECORD_CODEC = RecordCodecBuilder.create(
      i -> i.group(
            ExtraCodecs.NON_NEGATIVE_INT.lenientOptionalFieldOf("bottom", 0).forGetter(r -> r.bottom),
            ExtraCodecs.NON_NEGATIVE_INT.lenientOptionalFieldOf("top", 0).forGetter(r -> r.top)
         )
         .apply(i, DimensionPadding::new)
   );
   public static final Codec<DimensionPadding> CODEC = Codec.either(ExtraCodecs.NON_NEGATIVE_INT, RECORD_CODEC)
      .xmap(
         e -> (DimensionPadding)e.map(DimensionPadding::new, Function.identity()),
         padding -> padding.hasEqualTopAndBottom() ? Either.left(padding.bottom) : Either.right(padding)
      );
   public static final DimensionPadding ZERO = new DimensionPadding(0);

   public DimensionPadding(final int value) {
      this(value, value);
   }

   public boolean hasEqualTopAndBottom() {
      return this.top == this.bottom;
   }
}
