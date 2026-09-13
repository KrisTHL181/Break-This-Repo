package net.minecraft.world.level.levelgen.heightproviders;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;

public class ConstantHeight extends HeightProvider {
   public static final ConstantHeight ZERO = new ConstantHeight(VerticalAnchor.absolute(0));
   public static final MapCodec<ConstantHeight> CODEC = VerticalAnchor.CODEC.fieldOf("value").xmap(ConstantHeight::new, ConstantHeight::getValue);
   private final VerticalAnchor value;

   public static ConstantHeight of(final VerticalAnchor value) {
      return new ConstantHeight(value);
   }

   private ConstantHeight(final VerticalAnchor value) {
      this.value = value;
   }

   public VerticalAnchor getValue() {
      return this.value;
   }

   @Override
   public int sample(final RandomSource random, final WorldGenerationContext context) {
      return this.value.resolveY(context);
   }

   @Override
   public HeightProviderType<?> getType() {
      return HeightProviderType.CONSTANT;
   }

   @Override
   public String toString() {
      return this.value.toString();
   }
}
