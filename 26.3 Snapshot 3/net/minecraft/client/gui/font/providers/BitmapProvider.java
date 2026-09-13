package net.minecraft.client.gui.font.providers;

import com.mojang.blaze3d.font.GlyphBitmap;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.UnbakedGlyph;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.font.CodepointMap;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

public class BitmapProvider implements GlyphProvider {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final BitmapProvider.ImageDataHolder imageData;
   private final CodepointMap<BitmapProvider.Glyph> glyphs;

   private BitmapProvider(final BitmapProvider.ImageDataHolder imageData, final CodepointMap<BitmapProvider.Glyph> glyphs) {
      this.imageData = imageData;
      this.glyphs = glyphs;
   }

   @Override
   public void close() {
      this.imageData.close();
   }

   @Override
   public @Nullable UnbakedGlyph getGlyph(final int codepoint) {
      return this.glyphs.get(codepoint);
   }

   @Override
   public IntSet getSupportedGlyphs() {
      return IntSets.unmodifiable(this.glyphs.keySet());
   }

   public record Definition(Identifier file, int height, int ascent, int[][] codepointGrid) implements GlyphProviderDefinition {
      // ===== 修改：CODEPOINT_GRID_CODEC 的 validate 改为显式 lambda =====
      private static final Codec<int[][]> CODEPOINT_GRID_CODEC = Codec.STRING.listOf().xmap(input -> {
         int lineCount = input.size();
         int[][] result = new int[lineCount][];

         for (int i = 0; i < lineCount; i++) {
            result[i] = ((String)input.get(i)).codePoints().toArray();
         }

         return result;
      }, grid -> {
         List<String> result = new ArrayList<>(grid.length);

         for (int[] line : grid) {
            result.add(new String(line, 0, line.length));
         }

         return result;
      }).validate((int[][] grid) -> BitmapProvider.Definition.validateDimensions(grid));

      // ===== 修改：RecordCodecBuilder.<Definition>group → i.group，validate 改为显式 lambda =====
      public static final MapCodec<BitmapProvider.Definition> CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                  Identifier.CODEC.fieldOf("file").forGetter(BitmapProvider.Definition::file),
                  Codec.INT.optionalFieldOf("height", 8).forGetter(BitmapProvider.Definition::height),
                  Codec.INT.fieldOf("ascent").forGetter(BitmapProvider.Definition::ascent),
                  CODEPOINT_GRID_CODEC.fieldOf("chars").forGetter(BitmapProvider.Definition::codepointGrid)
               )
               .apply(i, BitmapProvider.Definition::new)
         )
         .validate((BitmapProvider.Definition def) -> BitmapProvider.Definition.validate(def));

      private static DataResult<int[][]> validateDimensions(final int[][] grid) {
         int lineCount = grid.length;
         if (lineCount == 0) {
            return DataResult.error(() -> "Expected to find data in codepoint grid");
         }

         int[] firstLine = grid[0];
         int lineWidth = firstLine.length;
         if (lineWidth == 0) {
            return DataResult.error(() -> "Expected to find data in codepoint grid");
         }

         for (int i = 1; i < lineCount; i++) {
            int[] line = grid[i];
            if (line.length != lineWidth) {
               return DataResult.error(
                  () -> "Lines in codepoint grid have to be the same length (found: "
                     + line.length
                     + " codepoints, expected: "
                     + lineWidth
                     + "), pad with \\u0000"
               );
            }
         }

         return DataResult.success(grid);
      }

      private static DataResult<BitmapProvider.Definition> validate(final BitmapProvider.Definition builder) {
         return builder.ascent > builder.height
            ? DataResult.error(() -> "Ascent " + builder.ascent + " higher than height " + builder.height)
            : DataResult.success(builder);
      }

      @Override
      public GlyphProviderType type() {
         return GlyphProviderType.BITMAP;
      }

      @Override
      public Either<GlyphProviderDefinition.Loader, GlyphProviderDefinition.Reference> unpack() {
         return Either.left(this::load);
      }

      private GlyphProvider load(final ResourceManager resourceManager) throws IOException {
         Identifier texture = this.file.withPrefix("textures/");

         try (InputStream resource = resourceManager.open(texture)) {
            NativeImage image = NativeImage.read(NativeImage.Format.RGBA, resource);
            int w = image.getWidth();
            int h = image.getHeight();
            int glyphWidth = w / this.codepointGrid[0].length;
            int glyphHeight = h / this.codepointGrid.length;
            float pixelScale = (float)this.height / glyphHeight;
            CodepointMap<BitmapProvider.Glyph> charMap = new CodepointMap<>(BitmapProvider.Glyph[]::new, BitmapProvider.Glyph[][]::new);
            BitmapProvider.ImageDataHolder imageDataHolder = new BitmapProvider.ImageDataHolder(texture, image);

            for (int slotY = 0; slotY < this.codepointGrid.length; slotY++) {
               int linePos = 0;

               for (int c : this.codepointGrid[slotY]) {
                  int slotX = linePos++;
                  if (c != 0) {
                     int actualGlyphWidth = this.getActualGlyphWidth(image, glyphWidth, glyphHeight, slotX, slotY);
                     BitmapProvider.Glyph prev = charMap.put(
                        c,
                        new BitmapProvider.Glyph(
                           pixelScale,
                           imageDataHolder,
                           slotX * glyphWidth,
                           slotY * glyphHeight,
                           glyphWidth,
                           glyphHeight,
                           (int)(0.5 + actualGlyphWidth * pixelScale) + 1,
                           this.ascent
                        )
                     );
                     if (prev != null) {
                        BitmapProvider.LOGGER.warn("Codepoint '{}' declared multiple times in {}", Integer.toHexString(c), texture);
                     }
                  }
               }
            }

            return new BitmapProvider(imageDataHolder, charMap);
         }
      }

      private int getActualGlyphWidth(final NativeImage image, final int glyphWidth, final int glyphHeight, final int xGlyph, final int yGlyph) {
         int width;
         for (width = glyphWidth - 1; width >= 0; width--) {
            int xPixel = xGlyph * glyphWidth + width;

            for (int y = 0; y < glyphHeight; y++) {
               int yPixel = yGlyph * glyphHeight + y;
               if (image.getLuminanceOrAlpha(xPixel, yPixel) != 0) {
                  return width + 1;
               }
            }
         }

         return width + 1;
      }
   }

   private record Glyph(float scale, BitmapProvider.ImageDataHolder imageData, int offsetX, int offsetY, int width, int height, int advance, int ascent)
      implements UnbakedGlyph {
      @Override
      public GlyphInfo info() {
         return GlyphInfo.simple(this.advance);
      }

      @Override
      public BakedGlyph bake(final UnbakedGlyph.Stitcher stitcher) {
         return stitcher.stitch(
            this.info(),
            new GlyphBitmap() {
               @Override
               public float getOversample() {
                  return 1.0F / Glyph.this.scale;
               }

               @Override
               public int getPixelWidth() {
                  return Glyph.this.width;
               }

               @Override
               public int getPixelHeight() {
                  return Glyph.this.height;
               }

               @Override
               public float getBearingTop() {
                  return Glyph.this.ascent;
               }

               @Override
               public void upload(final int x, final int y, final GpuTexture texture) {
                  RenderSystem.getDevice()
                     .createCommandEncoder()
                     .copyBufferToTexture(
                        Glyph.this.imageData.gpuData().slice(),
                        Glyph.this.offsetX,
                        Glyph.this.offsetY,
                        Glyph.this.imageData.image.getWidth(),
                        Glyph.this.imageData.image.getHeight(),
                        texture,
                        x,
                        y,
                        Glyph.this.width,
                        Glyph.this.height,
                        0,
                        0
                     );
               }

               @Override
               public boolean isColored() {
                  return Glyph.this.imageData.image.format().components() > 1;
               }
            }
         );
      }
   }

   private static class ImageDataHolder implements AutoCloseable {
      private final Identifier identifier;
      private final NativeImage image;
      private @Nullable GpuBuffer gpuBuffer = null;

      private ImageDataHolder(final Identifier identifier, final NativeImage image) {
         this.identifier = identifier;
         this.image = image;
      }

      private GpuBuffer gpuData() {
         if (this.gpuBuffer == null) {
            ByteBuffer imageBytes = this.image.getPixelBytes();
            this.gpuBuffer = RenderSystem.getDevice().createBuffer(() -> this.identifier + " staging buffer", 22, imageBytes.remaining());

            try (GpuBufferSlice.MappedView mappedView = this.gpuBuffer.map(false, true)) {
               MemoryUtil.memCopy(imageBytes, mappedView.data());
            }
         }

         return this.gpuBuffer;
      }

      @Override
      public void close() {
         this.image.close();
         if (this.gpuBuffer != null) {
            this.gpuBuffer.close();
         }
      }
   }
}