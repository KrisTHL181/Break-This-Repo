package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class GeodeLayerSettings {
   private static final Codec<Double> LAYER_RANGE = Codec.doubleRange(0.01, 50.0);
   public static final Codec<GeodeLayerSettings> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            LAYER_RANGE.optionalFieldOf("filling", 1.7).forGetter(c -> c.filling),
            LAYER_RANGE.optionalFieldOf("inner_layer", 2.2).forGetter(c -> c.innerLayer),
            LAYER_RANGE.optionalFieldOf("middle_layer", 3.2).forGetter(c -> c.middleLayer),
            LAYER_RANGE.optionalFieldOf("outer_layer", 4.2).forGetter(c -> c.outerLayer)
         )
         .apply(i, GeodeLayerSettings::new)
   );
   public final double filling;
   public final double innerLayer;
   public final double middleLayer;
   public final double outerLayer;

   public GeodeLayerSettings(final double filling, final double innerLayer, final double middleLayer, final double outerLayer) {
      this.filling = filling;
      this.innerLayer = innerLayer;
      this.middleLayer = middleLayer;
      this.outerLayer = outerLayer;
   }
}
