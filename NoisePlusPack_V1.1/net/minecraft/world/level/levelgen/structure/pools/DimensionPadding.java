package net.minecraft.world.level.levelgen.structure.pools;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Function;
import net.minecraft.util.ExtraCodecs;

public record DimensionPadding(int bottom, int top) {
   private static final Codec<DimensionPadding> RECORD_CODEC = RecordCodecBuilder.create(
      p_344055_ -> p_344055_.group(
            ExtraCodecs.NON_NEGATIVE_INT.lenientOptionalFieldOf("bottom", 0).forGetter(p_344937_ -> p_344937_.bottom),
            ExtraCodecs.NON_NEGATIVE_INT.lenientOptionalFieldOf("top", 0).forGetter(p_343653_ -> p_343653_.top)
         )
         .apply(p_344055_, DimensionPadding::new)
   );
   public static final Codec<DimensionPadding> CODEC = Codec.either(ExtraCodecs.NON_NEGATIVE_INT, RECORD_CODEC)
      .xmap(
         p_342286_ -> (DimensionPadding)p_342286_.map(DimensionPadding::new, Function.identity()),
         p_342199_ -> p_342199_.hasEqualTopAndBottom() ? Either.left(p_342199_.bottom) : Either.right(p_342199_)
      );
   public static final DimensionPadding ZERO = new DimensionPadding(0);

   public DimensionPadding(int p_345079_) {
      this(p_345079_, p_345079_);
   }

   public boolean hasEqualTopAndBottom() {
      return this.top == this.bottom;
   }
}
