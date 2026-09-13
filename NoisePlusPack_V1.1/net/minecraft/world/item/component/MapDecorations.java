package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.util.Util;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;

public record MapDecorations(Map<String, MapDecorations.Entry> decorations) {
   public static final MapDecorations EMPTY = new MapDecorations(Map.of());
   public static final Codec<MapDecorations> CODEC = Codec.unboundedMap(Codec.STRING, MapDecorations.Entry.CODEC)
      .xmap(MapDecorations::new, MapDecorations::decorations);

   public MapDecorations withDecoration(String p_327714_, MapDecorations.Entry p_334134_) {
      return new MapDecorations(Util.copyAndPut(this.decorations, p_327714_, p_334134_));
   }

   public record Entry(Holder<MapDecorationType> type, double x, double z, float rotation) {
      public static final Codec<MapDecorations.Entry> CODEC = RecordCodecBuilder.create(
         p_334294_ -> p_334294_.group(
               MapDecorationType.CODEC.fieldOf("type").forGetter(MapDecorations.Entry::type),
               Codec.DOUBLE.fieldOf("x").forGetter(MapDecorations.Entry::x),
               Codec.DOUBLE.fieldOf("z").forGetter(MapDecorations.Entry::z),
               Codec.FLOAT.fieldOf("rotation").forGetter(MapDecorations.Entry::rotation)
            )
            .apply(p_334294_, MapDecorations.Entry::new)
      );
   }
}
