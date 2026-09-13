package net.minecraft.world.level.levelgen;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import java.util.function.Function;
import net.minecraft.world.level.dimension.DimensionType;

public interface VerticalAnchor {
   Codec<VerticalAnchor> CODEC = Codec.xor(VerticalAnchor.Absolute.CODEC, Codec.xor(VerticalAnchor.AboveBottom.CODEC, VerticalAnchor.BelowTop.CODEC))
      .xmap(VerticalAnchor::merge, VerticalAnchor::split);
   VerticalAnchor BOTTOM = aboveBottom(0);
   VerticalAnchor TOP = belowTop(0);

   static VerticalAnchor absolute(int p_158923_) {
      return new VerticalAnchor.Absolute(p_158923_);
   }

   static VerticalAnchor aboveBottom(int p_158931_) {
      return new VerticalAnchor.AboveBottom(p_158931_);
   }

   static VerticalAnchor belowTop(int p_158936_) {
      return new VerticalAnchor.BelowTop(p_158936_);
   }

   static VerticalAnchor bottom() {
      return BOTTOM;
   }

   static VerticalAnchor top() {
      return TOP;
   }

   private static VerticalAnchor merge(Either<VerticalAnchor.Absolute, Either<VerticalAnchor.AboveBottom, VerticalAnchor.BelowTop>> p_158925_) {
      return (VerticalAnchor)p_158925_.map(Function.identity(), Either::unwrap);
   }

   private static Either<VerticalAnchor.Absolute, Either<VerticalAnchor.AboveBottom, VerticalAnchor.BelowTop>> split(VerticalAnchor p_158927_) {
      return p_158927_ instanceof VerticalAnchor.Absolute
         ? Either.left((VerticalAnchor.Absolute)p_158927_)
         : Either.right(
            p_158927_ instanceof VerticalAnchor.AboveBottom
               ? Either.left((VerticalAnchor.AboveBottom)p_158927_)
               : Either.right((VerticalAnchor.BelowTop)p_158927_)
         );
   }

   int resolveY(WorldGenerationContext var1);

   record AboveBottom(int offset) implements VerticalAnchor {
      public static final Codec<VerticalAnchor.AboveBottom> CODEC = Codec.intRange(DimensionType.MIN_Y, DimensionType.MAX_Y)
         .fieldOf("above_bottom")
         .xmap(VerticalAnchor.AboveBottom::new, VerticalAnchor.AboveBottom::offset)
         .codec();

      @Override
      public int resolveY(WorldGenerationContext p_158942_) {
         return p_158942_.getMinGenY() + this.offset;
      }

      @Override
      public String toString() {
         return this.offset + " above bottom";
      }
   }

   record Absolute(int y) implements VerticalAnchor {
      public static final Codec<VerticalAnchor.Absolute> CODEC = Codec.intRange(DimensionType.MIN_Y, DimensionType.MAX_Y)
         .fieldOf("absolute")
         .xmap(VerticalAnchor.Absolute::new, VerticalAnchor.Absolute::y)
         .codec();

      @Override
      public int resolveY(WorldGenerationContext p_158949_) {
         return this.y;
      }

      @Override
      public String toString() {
         return this.y + " absolute";
      }
   }

   record BelowTop(int offset) implements VerticalAnchor {
      public static final Codec<VerticalAnchor.BelowTop> CODEC = Codec.intRange(DimensionType.MIN_Y, DimensionType.MAX_Y)
         .fieldOf("below_top")
         .xmap(VerticalAnchor.BelowTop::new, VerticalAnchor.BelowTop::offset)
         .codec();

      @Override
      public int resolveY(WorldGenerationContext p_158956_) {
         return p_158956_.getGenDepth() - 1 + p_158956_.getMinGenY() - this.offset;
      }

      @Override
      public String toString() {
         return this.offset + " below top";
      }
   }
}
