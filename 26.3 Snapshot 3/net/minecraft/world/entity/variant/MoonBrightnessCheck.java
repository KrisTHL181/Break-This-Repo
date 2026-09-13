package net.minecraft.world.entity.variant;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;

public record MoonBrightnessCheck(MinMaxBounds.Doubles range) implements SpawnCondition {
   public static final MapCodec<MoonBrightnessCheck> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(MinMaxBounds.Doubles.CODEC.fieldOf("range").forGetter(MoonBrightnessCheck::range)).apply(i, MoonBrightnessCheck::new)
   );

   public boolean test(final SpawnContext context) {
      MoonPhase moonPhase = context.environmentAttributes().getValue(EnvironmentAttributes.MOON_PHASE, Vec3.atCenterOf(context.pos()));
      float moonBrightness = DimensionType.MOON_BRIGHTNESS_PER_PHASE[moonPhase.index()];
      return this.range.matches(moonBrightness);
   }

   @Override
   public MapCodec<MoonBrightnessCheck> codec() {
      return MAP_CODEC;
   }
}
