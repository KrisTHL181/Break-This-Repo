package net.minecraft.world.level.levelgen.carver;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;

public class CaveCarverConfiguration extends CarverConfiguration {
   public static final Codec<CaveCarverConfiguration> CODEC = RecordCodecBuilder.create(
      p_159184_ -> p_159184_.group(
            CarverConfiguration.CODEC.forGetter(p_159192_ -> p_159192_),
            FloatProvider.CODEC.fieldOf("horizontal_radius_multiplier").forGetter(p_159190_ -> p_159190_.horizontalRadiusMultiplier),
            FloatProvider.CODEC.fieldOf("vertical_radius_multiplier").forGetter(p_159188_ -> p_159188_.verticalRadiusMultiplier),
            FloatProvider.codec(-1.0F, 1.0F).fieldOf("floor_level").forGetter(p_159186_ -> p_159186_.floorLevel)
         )
         .apply(p_159184_, CaveCarverConfiguration::new)
   );
   public final FloatProvider horizontalRadiusMultiplier;
   public final FloatProvider verticalRadiusMultiplier;
   final FloatProvider floorLevel;

   public CaveCarverConfiguration(
      float p_224853_,
      HeightProvider p_224854_,
      FloatProvider p_224855_,
      VerticalAnchor p_224856_,
      CarverDebugSettings p_224857_,
      HolderSet<Block> p_224858_,
      FloatProvider p_224859_,
      FloatProvider p_224860_,
      FloatProvider p_224861_
   ) {
      super(p_224853_, p_224854_, p_224855_, p_224856_, p_224857_, p_224858_);
      this.horizontalRadiusMultiplier = p_224859_;
      this.verticalRadiusMultiplier = p_224860_;
      this.floorLevel = p_224861_;
   }

   public CaveCarverConfiguration(
      float p_224863_,
      HeightProvider p_224864_,
      FloatProvider p_224865_,
      VerticalAnchor p_224866_,
      HolderSet<Block> p_224867_,
      FloatProvider p_224868_,
      FloatProvider p_224869_,
      FloatProvider p_224870_
   ) {
      this(p_224863_, p_224864_, p_224865_, p_224866_, CarverDebugSettings.DEFAULT, p_224867_, p_224868_, p_224869_, p_224870_);
   }

   public CaveCarverConfiguration(CarverConfiguration p_159179_, FloatProvider p_159180_, FloatProvider p_159181_, FloatProvider p_159182_) {
      this(
         p_159179_.probability,
         p_159179_.y,
         p_159179_.yScale,
         p_159179_.lavaLevel,
         p_159179_.debugSettings,
         p_159179_.replaceable,
         p_159180_,
         p_159181_,
         p_159182_
      );
   }
}
