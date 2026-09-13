package net.minecraft.client.resources;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record WaypointStyle(int nearDistance, int farDistance, List<Identifier> sprites, List<Identifier> spriteLocations) {
   @VisibleForTesting
   public static final String ICON_LOCATION_PREFIX = "hud/locator_bar_dot/";
   public static final int DEFAULT_NEAR_DISTANCE = 128;
   public static final int DEFAULT_FAR_DISTANCE = 332;
   private static final Codec<Integer> DISTANCE_CODEC = Codec.intRange(0, 60000000);
   public static final Codec<WaypointStyle> CODEC = RecordCodecBuilder.create(
         p_448423_ -> p_448423_.group(
               DISTANCE_CODEC.optionalFieldOf("near_distance", 128).forGetter(WaypointStyle::nearDistance),
               DISTANCE_CODEC.optionalFieldOf("far_distance", 332).forGetter(WaypointStyle::farDistance),
               ExtraCodecs.nonEmptyList(Identifier.CODEC.listOf()).fieldOf("sprites").forGetter(WaypointStyle::sprites)
            )
            .apply(p_448423_, WaypointStyle::new)
      )
      .validate(WaypointStyle::validate);

   public WaypointStyle(int p_407902_, int p_407519_, List<Identifier> p_407571_) {
      this(p_407902_, p_407519_, p_407571_, p_407571_.stream().map(p_448422_ -> p_448422_.withPrefix("hud/locator_bar_dot/")).toList());
   }

   @VisibleForTesting
   public DataResult<WaypointStyle> validate() {
      if (this.sprites.isEmpty()) {
         return DataResult.error(() -> "Must have at least one sprite icon");
      } else if (this.nearDistance <= 0) {
         return DataResult.error(() -> "Near distance (" + this.nearDistance + ") must be greater than zero");
      } else {
         return this.nearDistance >= this.farDistance
            ? DataResult.error(() -> "Far distance (" + this.farDistance + ") cannot be closer or equal to near distance (" + this.nearDistance + ")")
            : DataResult.success(this);
      }
   }

   public Identifier sprite(float p_407304_) {
      if (p_407304_ < this.nearDistance) {
         return this.spriteLocations.getFirst();
      }

      if (p_407304_ >= this.farDistance) {
         return this.spriteLocations.getLast();
      }

      if (this.spriteLocations.size() == 1) {
         return this.spriteLocations.getFirst();
      }

      if (this.spriteLocations.size() == 3) {
         return this.spriteLocations.get(1);
      }

      int i = Mth.lerpInt((p_407304_ - this.nearDistance) / (this.farDistance - this.nearDistance), 1, this.spriteLocations.size() - 1);
      return this.spriteLocations.get(i);
   }
}
