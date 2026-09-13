package net.minecraft.world.level.levelgen.structure.placement;

import com.mojang.serialization.Codec;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;

public enum RandomSpreadType implements StringRepresentable {
   LINEAR("linear"),
   TRIANGULAR("triangular");

   public static final Codec<RandomSpreadType> CODEC = StringRepresentable.fromEnum(RandomSpreadType::values);
   private final String id;

   RandomSpreadType(final String id) {
      this.id = id;
   }

   @Override
   public String getSerializedName() {
      return this.id;
   }

   public int evaluate(final RandomSource random, final int limit) {
      return switch (this) {
         case LINEAR -> random.nextInt(limit);
         case TRIANGULAR -> (random.nextInt(limit) + random.nextInt(limit)) / 2;
      };
   }
}
