package net.minecraft.world.level.levelgen.structure.placement;

import com.mojang.serialization.Codec;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;

public enum RandomSpreadType implements StringRepresentable {
   LINEAR("linear"),
   TRIANGULAR("triangular");

   public static final Codec<RandomSpreadType> CODEC = StringRepresentable.fromEnum(RandomSpreadType::values);
   private final String id;

   RandomSpreadType(final String p_205022_) {
      this.id = p_205022_;
   }

   @Override
   public String getSerializedName() {
      return this.id;
   }

   public int evaluate(RandomSource p_227019_, int p_227020_) {
      return switch (this) {
         case LINEAR -> p_227019_.nextInt(p_227020_);
         case TRIANGULAR -> (p_227019_.nextInt(p_227020_) + p_227019_.nextInt(p_227020_)) / 2;
      };
   }
}
