package net.minecraft.client.renderer.texture.atlas.sources;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public record SingleFile(Identifier resourceId, Optional<Identifier> spriteId) implements SpriteSource {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final MapCodec<SingleFile> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_448418_ -> p_448418_.group(
            Identifier.CODEC.fieldOf("resource").forGetter(SingleFile::resourceId), Identifier.CODEC.optionalFieldOf("sprite").forGetter(SingleFile::spriteId)
         )
         .apply(p_448418_, SingleFile::new)
   );

   public SingleFile(Identifier p_460815_) {
      this(p_460815_, Optional.empty());
   }

   @Override
   public void run(ResourceManager p_261920_, SpriteSource.Output p_261578_) {
      Identifier identifier = TEXTURE_ID_CONVERTER.idToFile(this.resourceId);
      Optional<Resource> optional = p_261920_.getResource(identifier);
      if (optional.isPresent()) {
         p_261578_.add(this.spriteId.orElse(this.resourceId), optional.get());
      } else {
         LOGGER.warn("Missing sprite: {}", identifier);
      }
   }

   @Override
   public MapCodec<SingleFile> codec() {
      return MAP_CODEC;
   }
}
