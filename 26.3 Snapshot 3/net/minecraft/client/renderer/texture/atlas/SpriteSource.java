package net.minecraft.client.renderer.texture.atlas;

import com.mojang.serialization.MapCodec;
import java.util.function.Predicate;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jspecify.annotations.Nullable;

public interface SpriteSource {
   FileToIdConverter TEXTURE_ID_CONVERTER = new FileToIdConverter("textures", ".png");

   void run(ResourceManager resourceManager, SpriteSource.Output output);

   MapCodec<? extends SpriteSource> codec();

   interface DiscardableLoader extends SpriteSource.Loader {
      default void discard() {
      }
   }

   @FunctionalInterface
   interface Loader {
      @Nullable SpriteContents get(SpriteResourceLoader loader);
   }

   interface Output {
      default void add(final Identifier id, final Resource resource) {
         this.add(id, loader -> loader.loadSprite(id, resource));
      }

      void add(Identifier id, SpriteSource.DiscardableLoader sprite);

      void removeAll(Predicate<Identifier> predicate);
   }
}
