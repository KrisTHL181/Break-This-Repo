package net.minecraft.client.resources.palette;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntArrays;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.thread.ParallelMapTransform;
import org.slf4j.Logger;

public class Palette {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Palette EMPTY = new Palette(IntArrays.EMPTY_ARRAY);
   public static final FileToIdConverter ID_CONVERTER = new FileToIdConverter("textures/palettes", ".png");
   private final int[] colors;

   private Palette(final int[] colors) {
      this.colors = colors;
   }

   public static CompletableFuture<Map<Identifier, Palette>> listAndLoad(final ResourceManager resourceManager, final Executor taskExecutor) {
      return CompletableFuture.<Map<Identifier, Resource>>supplyAsync(() -> ID_CONVERTER.listMatchingResources(resourceManager), taskExecutor)
         .thenCompose(resources -> ParallelMapTransform.schedule((Map<Identifier, Resource>)resources, Palette::tryLoad, taskExecutor))
         .thenApply(
            palettes -> palettes.entrySet()
               .stream()
               .filter(entry -> !entry.getValue().isEmpty())
               .collect(Collectors.toMap(entry -> ID_CONVERTER.fileToId(entry.getKey()), Entry::getValue))
         );
   }

   private static Palette tryLoad(final Identifier id, final Resource resource) {
      try {
         return load(resource);
      } catch (IOException e) {
         LOGGER.error("Failed to load palette with id {}", id, e);
         return EMPTY;
      }
   }

   public static Palette load(final Resource resource) throws IOException {
      try (
         InputStream input = resource.open();
         NativeImage image = NativeImage.read(input);
      ) {
         return from(image);
      }
   }

   public static Palette from(final NativeImage image) {
      return new Palette(image.getPixels());
   }

   @VisibleForTesting
   public static Palette of(final int... colors) {
      return new Palette(IntArrays.copy(colors));
   }

   public int get(final int index) {
      return this.colors[index];
   }

   public int size() {
      return this.colors.length;
   }

   public boolean isEmpty() {
      return this.colors.length == 0;
   }
}
