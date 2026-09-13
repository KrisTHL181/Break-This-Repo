package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.heightproviders.TrapezoidHeight;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;

public class HeightRangePlacement extends PlacementModifier {
   public static final MapCodec<HeightRangePlacement> CODEC = RecordCodecBuilder.mapCodec(
      p_191679_ -> p_191679_.group(HeightProvider.CODEC.fieldOf("height").forGetter(p_191686_ -> p_191686_.height)).apply(p_191679_, HeightRangePlacement::new)
   );
   private final HeightProvider height;

   private HeightRangePlacement(HeightProvider p_191677_) {
      this.height = p_191677_;
   }

   public static HeightRangePlacement of(HeightProvider p_191684_) {
      return new HeightRangePlacement(p_191684_);
   }

   public static HeightRangePlacement uniform(VerticalAnchor p_191681_, VerticalAnchor p_191682_) {
      return of(UniformHeight.of(p_191681_, p_191682_));
   }

   public static HeightRangePlacement triangle(VerticalAnchor p_191693_, VerticalAnchor p_191694_) {
      return of(TrapezoidHeight.of(p_191693_, p_191694_));
   }

   @Override
   public Stream<BlockPos> getPositions(PlacementContext p_226340_, RandomSource p_226341_, BlockPos p_226342_) {
      return Stream.of(p_226342_.atY(this.height.sample(p_226341_, p_226340_)));
   }

   @Override
   public PlacementModifierType<?> type() {
      return PlacementModifierType.HEIGHT_RANGE;
   }
}
