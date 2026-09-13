package net.minecraft.client.gui.font.providers;

import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.TrueTypeGlyphProvider;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Util;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FreeType;

public record TrueTypeGlyphProviderDefinition(Identifier location, float size, float oversample, TrueTypeGlyphProviderDefinition.Shift shift, String skip)
   implements GlyphProviderDefinition {
   private static final Codec<String> SKIP_LIST_CODEC = Codec.withAlternative(Codec.STRING, Codec.STRING.listOf(), list -> String.join("", list));
   public static final MapCodec<TrueTypeGlyphProviderDefinition> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            Identifier.CODEC.fieldOf("file").forGetter(TrueTypeGlyphProviderDefinition::location),
            Codec.FLOAT.optionalFieldOf("size", 11.0F).forGetter(TrueTypeGlyphProviderDefinition::size),
            Codec.FLOAT.optionalFieldOf("oversample", 1.0F).forGetter(TrueTypeGlyphProviderDefinition::oversample),
            TrueTypeGlyphProviderDefinition.Shift.CODEC
               .optionalFieldOf("shift", TrueTypeGlyphProviderDefinition.Shift.NONE)
               .forGetter(TrueTypeGlyphProviderDefinition::shift),
            SKIP_LIST_CODEC.optionalFieldOf("skip", "").forGetter(TrueTypeGlyphProviderDefinition::skip)
         )
         .apply(i, TrueTypeGlyphProviderDefinition::new)
   );

   @Override
   public GlyphProviderType type() {
      return GlyphProviderType.TTF;
   }

   @Override
   public Either<GlyphProviderDefinition.Loader, GlyphProviderDefinition.Reference> unpack() {
      return Either.left(this::load);
   }

   private GlyphProvider load(final ResourceManager resourceManager) throws IOException {
      FT_Face face = null;
      ByteBuffer fontData = null;

      try (InputStream resource = resourceManager.open(this.location.withPrefix("font/"))) {
         fontData = TextureUtil.readResource(resource);
         synchronized (FreeTypeUtil.LIBRARY_LOCK) {
            MemoryStack stack = MemoryStack.stackPush();

            try {
               PointerBuffer faceBuffer = stack.mallocPointer(1);
               FreeTypeUtil.assertError(FreeType.FT_New_Memory_Face(FreeTypeUtil.getLibrary(), fontData, 0L, faceBuffer), "Initializing font face");
               face = FT_Face.create(faceBuffer.get());
            } catch (Throwable var14) {
               if (stack != null) {
                  try {
                     stack.close();
                  } catch (Throwable var12) {
                     var14.addSuppressed(var12);
                  }
               }

               throw var14;
            }

            if (stack != null) {
               stack.close();
            }

            String format = FreeType.FT_Get_Font_Format(face);
            if (!"TrueType".equals(format)) {
               throw new IOException("Font is not in TTF format, was " + format);
            }

            FreeTypeUtil.assertError(FreeType.FT_Select_Charmap(face, FreeType.FT_ENCODING_UNICODE), "Find unicode charmap");
            return new TrueTypeGlyphProvider(fontData, face, this.size, this.oversample, this.shift.x, this.shift.y, this.skip);
         }
      } catch (Exception ex) {
         synchronized (FreeTypeUtil.LIBRARY_LOCK) {
            if (face != null) {
               FreeType.FT_Done_Face(face);
            }
         }

         MemoryUtil.memFree(fontData);
         throw ex;
      }
   }

   public record Shift(float x, float y) {
      public static final TrueTypeGlyphProviderDefinition.Shift NONE = new TrueTypeGlyphProviderDefinition.Shift(0.0F, 0.0F);
      public static final Codec<TrueTypeGlyphProviderDefinition.Shift> CODEC = Codec.floatRange(-512.0F, 512.0F)
         .listOf()
         .comapFlatMap(
            input -> Util.fixedSize(input, 2).map(floats -> new TrueTypeGlyphProviderDefinition.Shift((Float)floats.get(0), (Float)floats.get(1))),
            shift -> List.of(shift.x, shift.y)
         );
   }
}
