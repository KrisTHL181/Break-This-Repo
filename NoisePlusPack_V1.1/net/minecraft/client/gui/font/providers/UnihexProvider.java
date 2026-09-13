package net.minecraft.client.gui.font.providers;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.blaze3d.font.GlyphBitmap;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.UnbakedGlyph;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import net.minecraft.client.gui.font.CodepointMap;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.FastBufferedInputStream;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class UnihexProvider implements GlyphProvider {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final int GLYPH_HEIGHT = 16;
   private static final int DIGITS_PER_BYTE = 2;
   private static final int DIGITS_FOR_WIDTH_8 = 32;
   private static final int DIGITS_FOR_WIDTH_16 = 64;
   private static final int DIGITS_FOR_WIDTH_24 = 96;
   private static final int DIGITS_FOR_WIDTH_32 = 128;
   private final CodepointMap<UnihexProvider.Glyph> glyphs;

   UnihexProvider(CodepointMap<UnihexProvider.Glyph> p_285457_) {
      this.glyphs = p_285457_;
   }

   @Override
   public @Nullable UnbakedGlyph getGlyph(int p_285239_) {
      return this.glyphs.get(p_285239_);
   }

   @Override
   public IntSet getSupportedGlyphs() {
      return this.glyphs.keySet();
   }

   @VisibleForTesting
   static void unpackBitsToBytes(IntBuffer p_285211_, int p_285508_, int p_285312_, int p_285412_) {
      int i = 32 - p_285312_ - 1;
      int j = 32 - p_285412_ - 1;

      for (int k = i; k >= j; k--) {
         if (k < 32 && k >= 0) {
            boolean flag = (p_285508_ >> k & 1) != 0;
            p_285211_.put(flag ? -1 : 0);
         } else {
            p_285211_.put(0);
         }
      }
   }

   static void unpackBitsToBytes(IntBuffer p_285283_, UnihexProvider.LineData p_285485_, int p_284940_, int p_284950_) {
      for (int i = 0; i < 16; i++) {
         int j = p_285485_.line(i);
         unpackBitsToBytes(p_285283_, j, p_284940_, p_284950_);
      }
   }

   @VisibleForTesting
   static void readFromStream(InputStream p_285315_, UnihexProvider.ReaderOutput p_285353_) throws IOException {
      int i = 0;
      ByteList bytelist = new ByteArrayList(128);

      while (true) {
         boolean flag = copyUntil(p_285315_, bytelist, 58);
         int j = bytelist.size();
         if (j == 0 && !flag) {
            return;
         }

         if (!flag || j != 4 && j != 5 && j != 6) {
            throw new IllegalArgumentException("Invalid entry at line " + i + ": expected 4, 5 or 6 hex digits followed by a colon");
         }

         int k = 0;

         for (int l = 0; l < j; l++) {
            k = k << 4 | decodeHex(i, bytelist.getByte(l));
         }

         bytelist.clear();
         copyUntil(p_285315_, bytelist, 10);
         int i1 = bytelist.size();

         UnihexProvider.LineData unihexprovider$linedata = switch (i1) {
            case 32 -> UnihexProvider.ByteContents.read(i, bytelist);
            case 64 -> UnihexProvider.ShortContents.read(i, bytelist);
            case 96 -> UnihexProvider.IntContents.read24(i, bytelist);
            case 128 -> UnihexProvider.IntContents.read32(i, bytelist);
            default -> throw new IllegalArgumentException(
               "Invalid entry at line " + i + ": expected hex number describing (8,16,24,32) x 16 bitmap, followed by a new line"
            );
         };
         p_285353_.accept(k, unihexprovider$linedata);
         i++;
         bytelist.clear();
      }
   }

   static int decodeHex(int p_285205_, ByteList p_285268_, int p_285345_) {
      return decodeHex(p_285205_, p_285268_.getByte(p_285345_));
   }

   private static int decodeHex(int p_284952_, byte p_285036_) {
      return switch (p_285036_) {
         case 48 -> 0;
         case 49 -> 1;
         case 50 -> 2;
         case 51 -> 3;
         case 52 -> 4;
         case 53 -> 5;
         case 54 -> 6;
         case 55 -> 7;
         case 56 -> 8;
         case 57 -> 9;
         default -> throw new IllegalArgumentException("Invalid entry at line " + p_284952_ + ": expected hex digit, got " + (char)p_285036_);
         case 65 -> 10;
         case 66 -> 11;
         case 67 -> 12;
         case 68 -> 13;
         case 69 -> 14;
         case 70 -> 15;
      };
   }

   private static boolean copyUntil(InputStream p_284994_, ByteList p_285351_, int p_285177_) throws IOException {
      while (true) {
         int i = p_284994_.read();
         if (i == -1) {
            return false;
         }

         if (i == p_285177_) {
            return true;
         }

         p_285351_.add((byte)i);
      }
   }

   @OnlyIn(Dist.CLIENT)
   record ByteContents(byte[] contents) implements UnihexProvider.LineData {
      @Override
      public int line(int p_285203_) {
         return this.contents[p_285203_] << 24;
      }

      static UnihexProvider.LineData read(int p_285080_, ByteList p_285481_) {
         byte[] abyte = new byte[16];
         int i = 0;

         for (int j = 0; j < 16; j++) {
            int k = UnihexProvider.decodeHex(p_285080_, p_285481_, i++);
            int l = UnihexProvider.decodeHex(p_285080_, p_285481_, i++);
            byte b0 = (byte)(k << 4 | l);
            abyte[j] = b0;
         }

         return new UnihexProvider.ByteContents(abyte);
      }

      @Override
      public int bitWidth() {
         return 8;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Definition implements GlyphProviderDefinition {
      public static final MapCodec<UnihexProvider.Definition> CODEC = RecordCodecBuilder.mapCodec(
         p_447996_ -> p_447996_.group(
               Identifier.CODEC.fieldOf("hex_file").forGetter(p_447995_ -> p_447995_.hexFile),
               UnihexProvider.OverrideRange.CODEC.listOf().optionalFieldOf("size_overrides", List.of()).forGetter(p_286528_ -> p_286528_.sizeOverrides)
            )
            .apply(p_447996_, UnihexProvider.Definition::new)
      );
      private final Identifier hexFile;
      private final List<UnihexProvider.OverrideRange> sizeOverrides;

      private Definition(Identifier p_458200_, List<UnihexProvider.OverrideRange> p_286770_) {
         this.hexFile = p_458200_;
         this.sizeOverrides = p_286770_;
      }

      @Override
      public GlyphProviderType type() {
         return GlyphProviderType.UNIHEX;
      }

      @Override
      public Either<GlyphProviderDefinition.Loader, GlyphProviderDefinition.Reference> unpack() {
         return Either.left(this::load);
      }

      private GlyphProvider load(ResourceManager p_286472_) throws IOException {
         try (InputStream inputstream = p_286472_.open(this.hexFile)) {
            return this.loadData(inputstream);
         }
      }

      private UnihexProvider loadData(InputStream p_286795_) throws IOException {
         CodepointMap<UnihexProvider.LineData> codepointmap = new CodepointMap<>(UnihexProvider.LineData[]::new, UnihexProvider.LineData[][]::new);
         UnihexProvider.ReaderOutput unihexprovider$readeroutput = codepointmap::put;

         try (ZipInputStream zipinputstream = new ZipInputStream(p_286795_)) {
            ZipEntry zipentry;
            while ((zipentry = zipinputstream.getNextEntry()) != null) {
               String s = zipentry.getName();
               if (s.endsWith(".hex")) {
                  UnihexProvider.LOGGER.info("Found {}, loading", s);
                  UnihexProvider.readFromStream(new FastBufferedInputStream(zipinputstream), unihexprovider$readeroutput);
               }
            }

            CodepointMap<UnihexProvider.Glyph> codepointmap1 = new CodepointMap<>(UnihexProvider.Glyph[]::new, UnihexProvider.Glyph[][]::new);

            for (UnihexProvider.OverrideRange unihexprovider$overriderange : this.sizeOverrides) {
               int i = unihexprovider$overriderange.from;
               int j = unihexprovider$overriderange.to;
               UnihexProvider.Dimensions unihexprovider$dimensions = unihexprovider$overriderange.dimensions;

               for (int k = i; k <= j; k++) {
                  UnihexProvider.LineData unihexprovider$linedata = codepointmap.remove(k);
                  if (unihexprovider$linedata != null) {
                     codepointmap1.put(k, new UnihexProvider.Glyph(unihexprovider$linedata, unihexprovider$dimensions.left, unihexprovider$dimensions.right));
                  }
               }
            }

            codepointmap.forEach((p_286721_, p_286722_) -> {
               int l = p_286722_.calculateWidth();
               int i1 = UnihexProvider.Dimensions.left(l);
               int j1 = UnihexProvider.Dimensions.right(l);
               codepointmap1.put(p_286721_, new UnihexProvider.Glyph(p_286722_, i1, j1));
            });
            return new UnihexProvider(codepointmap1);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record Dimensions(int left, int right) {
      public static final MapCodec<UnihexProvider.Dimensions> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_285497_ -> p_285497_.group(
               Codec.INT.fieldOf("left").forGetter(UnihexProvider.Dimensions::left), Codec.INT.fieldOf("right").forGetter(UnihexProvider.Dimensions::right)
            )
            .apply(p_285497_, UnihexProvider.Dimensions::new)
      );
      public static final Codec<UnihexProvider.Dimensions> CODEC = MAP_CODEC.codec();

      public int pack() {
         return pack(this.left, this.right);
      }

      public static int pack(int p_285339_, int p_285120_) {
         return (p_285339_ & 0xFF) << 8 | p_285120_ & 0xFF;
      }

      public static int left(int p_285195_) {
         return (byte)(p_285195_ >> 8);
      }

      public static int right(int p_285419_) {
         return (byte)p_285419_;
      }
   }

   @OnlyIn(Dist.CLIENT)
   record Glyph(UnihexProvider.LineData contents, int left, int right) implements UnbakedGlyph {
      public int width() {
         return this.right - this.left + 1;
      }

      @Override
      public GlyphInfo info() {
         return new GlyphInfo() {
            @Override
            public float getAdvance() {
               return Glyph.this.width() / 2 + 1;
            }

            @Override
            public float getShadowOffset() {
               return 0.5F;
            }

            @Override
            public float getBoldOffset() {
               return 0.5F;
            }
         };
      }

      @Override
      public BakedGlyph bake(UnbakedGlyph.Stitcher p_427952_) {
         return p_427952_.stitch(
            this.info(),
            new GlyphBitmap() {
               @Override
               public float getOversample() {
                  return 2.0F;
               }

               @Override
               public int getPixelWidth() {
                  return Glyph.this.width();
               }

               @Override
               public int getPixelHeight() {
                  return 16;
               }

               @Override
               public void upload(int p_429615_, int p_426879_, GpuTexture p_431541_) {
                  IntBuffer intbuffer = MemoryUtil.memAllocInt(Glyph.this.width() * 16);
                  UnihexProvider.unpackBitsToBytes(intbuffer, Glyph.this.contents, Glyph.this.left, Glyph.this.right);
                  intbuffer.rewind();
                  RenderSystem.getDevice()
                     .createCommandEncoder()
                     .writeToTexture(
                        p_431541_, MemoryUtil.memByteBuffer(intbuffer), NativeImage.Format.RGBA, 0, 0, p_429615_, p_426879_, Glyph.this.width(), 16
                     );
                  MemoryUtil.memFree(intbuffer);
               }

               @Override
               public boolean isColored() {
                  return true;
               }
            }
         );
      }
   }

   @OnlyIn(Dist.CLIENT)
   record IntContents(int[] contents, int bitWidth) implements UnihexProvider.LineData {
      private static final int SIZE_24 = 24;

      @Override
      public int line(int p_285172_) {
         return this.contents[p_285172_];
      }

      static UnihexProvider.LineData read24(int p_285362_, ByteList p_285123_) {
         int[] aint = new int[16];
         int i = 0;
         int j = 0;

         for (int k = 0; k < 16; k++) {
            int l = UnihexProvider.decodeHex(p_285362_, p_285123_, j++);
            int i1 = UnihexProvider.decodeHex(p_285362_, p_285123_, j++);
            int j1 = UnihexProvider.decodeHex(p_285362_, p_285123_, j++);
            int k1 = UnihexProvider.decodeHex(p_285362_, p_285123_, j++);
            int l1 = UnihexProvider.decodeHex(p_285362_, p_285123_, j++);
            int i2 = UnihexProvider.decodeHex(p_285362_, p_285123_, j++);
            int j2 = l << 20 | i1 << 16 | j1 << 12 | k1 << 8 | l1 << 4 | i2;
            aint[k] = j2 << 8;
            i |= j2;
         }

         return new UnihexProvider.IntContents(aint, 24);
      }

      public static UnihexProvider.LineData read32(int p_285222_, ByteList p_285346_) {
         int[] aint = new int[16];
         int i = 0;
         int j = 0;

         for (int k = 0; k < 16; k++) {
            int l = UnihexProvider.decodeHex(p_285222_, p_285346_, j++);
            int i1 = UnihexProvider.decodeHex(p_285222_, p_285346_, j++);
            int j1 = UnihexProvider.decodeHex(p_285222_, p_285346_, j++);
            int k1 = UnihexProvider.decodeHex(p_285222_, p_285346_, j++);
            int l1 = UnihexProvider.decodeHex(p_285222_, p_285346_, j++);
            int i2 = UnihexProvider.decodeHex(p_285222_, p_285346_, j++);
            int j2 = UnihexProvider.decodeHex(p_285222_, p_285346_, j++);
            int k2 = UnihexProvider.decodeHex(p_285222_, p_285346_, j++);
            int l2 = l << 28 | i1 << 24 | j1 << 20 | k1 << 16 | l1 << 12 | i2 << 8 | j2 << 4 | k2;
            aint[k] = l2;
            i |= l2;
         }

         return new UnihexProvider.IntContents(aint, 32);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public interface LineData {
      int line(int var1);

      int bitWidth();

      default int mask() {
         int i = 0;

         for (int j = 0; j < 16; j++) {
            i |= this.line(j);
         }

         return i;
      }

      default int calculateWidth() {
         int i = this.mask();
         int j = this.bitWidth();
         int k;
         int l;
         if (i == 0) {
            k = 0;
            l = j;
         } else {
            k = Integer.numberOfLeadingZeros(i);
            l = 32 - Integer.numberOfTrailingZeros(i) - 1;
         }

         return UnihexProvider.Dimensions.pack(k, l);
      }
   }

   @OnlyIn(Dist.CLIENT)
   record OverrideRange(int from, int to, UnihexProvider.Dimensions dimensions) {
      private static final Codec<UnihexProvider.OverrideRange> RAW_CODEC = RecordCodecBuilder.create(
         p_285088_ -> p_285088_.group(
               ExtraCodecs.CODEPOINT.fieldOf("from").forGetter(UnihexProvider.OverrideRange::from),
               ExtraCodecs.CODEPOINT.fieldOf("to").forGetter(UnihexProvider.OverrideRange::to),
               UnihexProvider.Dimensions.MAP_CODEC.forGetter(UnihexProvider.OverrideRange::dimensions)
            )
            .apply(p_285088_, UnihexProvider.OverrideRange::new)
      );
      public static final Codec<UnihexProvider.OverrideRange> CODEC = RAW_CODEC.validate(
         p_285215_ -> p_285215_.from >= p_285215_.to
            ? DataResult.error(() -> "Invalid range: [" + p_285215_.from + ";" + p_285215_.to + "]")
            : DataResult.success(p_285215_)
      );
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   public interface ReaderOutput {
      void accept(int var1, UnihexProvider.LineData var2);
   }

   @OnlyIn(Dist.CLIENT)
   record ShortContents(short[] contents) implements UnihexProvider.LineData {
      @Override
      public int line(int p_285158_) {
         return this.contents[p_285158_] << 16;
      }

      static UnihexProvider.LineData read(int p_285528_, ByteList p_284958_) {
         short[] ashort = new short[16];
         int i = 0;

         for (int j = 0; j < 16; j++) {
            int k = UnihexProvider.decodeHex(p_285528_, p_284958_, i++);
            int l = UnihexProvider.decodeHex(p_285528_, p_284958_, i++);
            int i1 = UnihexProvider.decodeHex(p_285528_, p_284958_, i++);
            int j1 = UnihexProvider.decodeHex(p_285528_, p_284958_, i++);
            short short1 = (short)(k << 12 | l << 8 | i1 << 4 | j1);
            ashort[j] = short1;
         }

         return new UnihexProvider.ShortContents(ashort);
      }

      @Override
      public int bitWidth() {
         return 16;
      }
   }
}
