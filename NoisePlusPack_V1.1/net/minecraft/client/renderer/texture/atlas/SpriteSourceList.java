package net.minecraft.client.renderer.texture.atlas;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.StrictJsonParser;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class SpriteSourceList {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final FileToIdConverter ATLAS_INFO_CONVERTER = new FileToIdConverter("atlases", ".json");
   private final List<SpriteSource> sources;

   private SpriteSourceList(List<SpriteSource> p_297576_) {
      this.sources = p_297576_;
   }

   public List<SpriteSource.Loader> list(ResourceManager p_298985_) {
      final Map<Identifier, SpriteSource.DiscardableLoader> map = new HashMap<>();
      SpriteSource.Output spritesource$output = new SpriteSource.Output() {
         @Override
         public void add(Identifier p_455897_, SpriteSource.DiscardableLoader p_455073_) {
            SpriteSource.DiscardableLoader spritesource$discardableloader = map.put(p_455897_, p_455073_);
            if (spritesource$discardableloader != null) {
               spritesource$discardableloader.discard();
            }
         }

         @Override
         public void removeAll(Predicate<Identifier> p_299726_) {
            Iterator<Entry<Identifier, SpriteSource.DiscardableLoader>> iterator = map.entrySet().iterator();

            while (iterator.hasNext()) {
               Entry<Identifier, SpriteSource.DiscardableLoader> entry = iterator.next();
               if (p_299726_.test(entry.getKey())) {
                  entry.getValue().discard();
                  iterator.remove();
               }
            }
         }
      };
      this.sources.forEach(p_299872_ -> p_299872_.run(p_298985_, spritesource$output));
      Builder<SpriteSource.Loader> builder = ImmutableList.builder();
      builder.add((SpriteSource.Loader)p_299121_ -> MissingTextureAtlasSprite.create());
      builder.addAll(map.values());
      return builder.build();
   }

   public static SpriteSourceList load(ResourceManager p_300689_, Identifier p_451225_) {
      Identifier identifier = ATLAS_INFO_CONVERTER.idToFile(p_451225_);
      List<SpriteSource> list = new ArrayList<>();

      for (Resource resource : p_300689_.getResourceStack(identifier)) {
         try (BufferedReader bufferedreader = resource.openAsReader()) {
            Dynamic<JsonElement> dynamic = new Dynamic(JsonOps.INSTANCE, StrictJsonParser.parse(bufferedreader));
            list.addAll((Collection<? extends SpriteSource>)SpriteSources.FILE_CODEC.parse(dynamic).getOrThrow());
         } catch (Exception exception) {
            LOGGER.error("Failed to parse atlas definition {} in pack {}", new Object[]{identifier, resource.sourcePackId(), exception});
         }
      }

      return new SpriteSourceList(list);
   }
}
