package net.minecraft.client.resources.palette;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.logging.LogUtils;
import java.time.Duration;
import java.util.Map;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

public class PaletteMappingCache {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final LoadingCache<PaletteMappingCache.IdPair, PaletteMapping> cache;

   public PaletteMappingCache(final Map<Identifier, Palette> palettes) {
      this.cache = CacheBuilder.newBuilder()
         .expireAfterAccess(Duration.ofMinutes(5L))
         .maximumSize(256L)
         .build(new CacheLoader<PaletteMappingCache.IdPair, PaletteMapping>() {
            public PaletteMapping load(final PaletteMappingCache.IdPair pair) {
               Palette basePalette = palettes.get(pair.base);
               Palette targetPalette = palettes.get(pair.target);
               if (basePalette == null) {
                  PaletteMappingCache.LOGGER.warn("Couldn't find base palette {}", pair.base);
                  return PaletteMapping.NONE;
               } else if (targetPalette == null) {
                  PaletteMappingCache.LOGGER.warn("Couldn't find target palette {}", pair.target);
                  return PaletteMapping.NONE;
               } else if (basePalette.size() != targetPalette.size()) {
                  PaletteMappingCache.LOGGER.warn("Could not create palette mapping for {}, had different sizes", pair);
                  return PaletteMapping.NONE;
               } else {
                  return PaletteMapping.create(basePalette, targetPalette);
               }
            }
         });
   }

   public PaletteMapping get(final Identifier baseId, final Identifier targetId) {
      return (PaletteMapping)this.cache.getUnchecked(new PaletteMappingCache.IdPair(baseId, targetId));
   }

   private record IdPair(Identifier base, Identifier target) {
   }
}
