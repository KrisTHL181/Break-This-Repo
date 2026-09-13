package net.minecraft.client.renderer.texture.atlas.sources;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record DirectoryLister(String sourcePath, String idPrefix) implements SpriteSource {
   public static final MapCodec<DirectoryLister> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_262096_ -> p_262096_.group(
            Codec.STRING.fieldOf("source").forGetter(DirectoryLister::sourcePath), Codec.STRING.fieldOf("prefix").forGetter(DirectoryLister::idPrefix)
         )
         .apply(p_262096_, DirectoryLister::new)
   );

   @Override
   public void run(ResourceManager p_261582_, SpriteSource.Output p_261898_) {
      FileToIdConverter filetoidconverter = new FileToIdConverter("textures/" + this.sourcePath, ".png");
      filetoidconverter.listMatchingResources(p_261582_).forEach((p_448411_, p_448412_) -> {
         Identifier identifier = filetoidconverter.fileToId(p_448411_).withPrefix(this.idPrefix);
         p_261898_.add(identifier, p_448412_);
      });
   }

   @Override
   public MapCodec<DirectoryLister> codec() {
      return MAP_CODEC;
   }
}
