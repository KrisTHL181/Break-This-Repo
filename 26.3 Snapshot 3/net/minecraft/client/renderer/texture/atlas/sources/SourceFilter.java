package net.minecraft.client.renderer.texture.atlas.sources;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.IdentifierPattern;

public record SourceFilter(IdentifierPattern filter) implements SpriteSource {
   public static final MapCodec<SourceFilter> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(IdentifierPattern.CODEC.fieldOf("pattern").forGetter(SourceFilter::filter)).apply(i, SourceFilter::new)
   );

   @Override
   public void run(final ResourceManager resourceManager, final SpriteSource.Output output) {
      output.removeAll(this.filter.locationPredicate());
   }

   @Override
   public MapCodec<SourceFilter> codec() {
      return MAP_CODEC;
   }
}
