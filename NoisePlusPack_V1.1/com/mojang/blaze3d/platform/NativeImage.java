package com.mojang.blaze3d.platform;

import com.mojang.jtracy.MemoryPool;
import com.mojang.jtracy.TracyClient;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntUnaryOperator;
import net.minecraft.client.gui.font.providers.FreeTypeUtil;
import net.minecraft.util.ARGB;
import net.minecraft.util.PngInfo;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.io.IOUtils;
import org.jspecify.annotations.Nullable;
import org.lwjgl.stb.STBIWriteCallback;
import org.lwjgl.stb.STBImage;
import org.lwjgl.stb.STBImageResize;
import org.lwjgl.stb.STBImageWrite;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Bitmap;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FT_GlyphSlot;
import org.lwjgl.util.freetype.FreeType;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public final class NativeImage implements AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final MemoryPool MEMORY_POOL = TracyClient.createMemoryPool("NativeImage");
   private static final Set<StandardOpenOption> OPEN_OPTIONS = EnumSet.of(
      StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
   );
   private final NativeImage.Format format;
   private final int width;
   private final int height;
   private final boolean useStbFree;
   private long pixels;
   private final long size;

   public NativeImage(int p_84968_, int p_84969_, boolean p_84970_) {
      this(NativeImage.Format.RGBA, p_84968_, p_84969_, p_84970_);
   }

   public NativeImage(NativeImage.Format p_84972_, int p_84973_, int p_84974_, boolean p_84975_) {
      if (p_84973_ > 0 && p_84974_ > 0) {
         this.format = p_84972_;
         this.width = p_84973_;
         this.height = p_84974_;
         this.size = (long)p_84973_ * p_84974_ * p_84972_.components();
         this.useStbFree = false;
         if (p_84975_) {
            this.pixels = MemoryUtil.nmemCalloc(1L, this.size);
         } else {
            this.pixels = MemoryUtil.nmemAlloc(this.size);
         }

         MEMORY_POOL.malloc(this.pixels, (int)this.size);
         if (this.pixels == 0L) {
            throw new IllegalStateException("Unable to allocate texture of size " + p_84973_ + "x" + p_84974_ + " (" + p_84972_.components() + " channels)");
         }
      } else {
         throw new IllegalArgumentException("Invalid texture size: " + p_84973_ + "x" + p_84974_);
      }
   }

   public NativeImage(NativeImage.Format p_84977_, int p_84978_, int p_84979_, boolean p_84980_, long p_84981_) {
      if (p_84978_ > 0 && p_84979_ > 0) {
         this.format = p_84977_;
         this.width = p_84978_;
         this.height = p_84979_;
         this.useStbFree = p_84980_;
         this.pixels = p_84981_;
         this.size = (long)p_84978_ * p_84979_ * p_84977_.components();
      } else {
         throw new IllegalArgumentException("Invalid texture size: " + p_84978_ + "x" + p_84979_);
      }
   }

   @Override
   public String toString() {
      return "NativeImage[" + this.format + " " + this.width + "x" + this.height + "@" + this.pixels + (this.useStbFree ? "S" : "N") + "]";
   }

   private boolean isOutsideBounds(int p_166423_, int p_166424_) {
      return p_166423_ < 0 || p_166423_ >= this.width || p_166424_ < 0 || p_166424_ >= this.height;
   }

   public static NativeImage read(InputStream p_85059_) throws IOException {
      return read(NativeImage.Format.RGBA, p_85059_);
   }

   public static NativeImage read(NativeImage.@Nullable Format p_85049_, InputStream p_85050_) throws IOException {
      ByteBuffer bytebuffer = null;

      NativeImage nativeimage;
      try {
         bytebuffer = TextureUtil.readResource(p_85050_);
         nativeimage = read(p_85049_, bytebuffer);
      } finally {
         MemoryUtil.memFree(bytebuffer);
         IOUtils.closeQuietly(p_85050_);
      }

      return nativeimage;
   }

   public static NativeImage read(ByteBuffer p_85063_) throws IOException {
      return read(NativeImage.Format.RGBA, p_85063_);
   }

   public static NativeImage read(byte[] p_273041_) throws IOException {
      MemoryStack memorystack = MemoryStack.stackGet();
      int i = memorystack.getPointer();
      if (i < p_273041_.length) {
         ByteBuffer bytebuffer1 = MemoryUtil.memAlloc(p_273041_.length);

         NativeImage nativeimage1;
         try {
            nativeimage1 = putAndRead(bytebuffer1, p_273041_);
         } finally {
            MemoryUtil.memFree(bytebuffer1);
         }

         return nativeimage1;
      } else {
         MemoryStack memorystack1 = MemoryStack.stackPush();

         NativeImage nativeimage;
         try {
            ByteBuffer bytebuffer = memorystack1.malloc(p_273041_.length);
            nativeimage = putAndRead(bytebuffer, p_273041_);
         } catch (Throwable var12) {
            if (memorystack1 != null) {
               try {
                  memorystack1.close();
               } catch (Throwable var10) {
                  var12.addSuppressed(var10);
               }
            }

            throw var12;
         }

         if (memorystack1 != null) {
            memorystack1.close();
         }

         return nativeimage;
      }
   }

   private static NativeImage putAndRead(ByteBuffer p_378245_, byte[] p_377207_) throws IOException {
      p_378245_.put(p_377207_);
      p_378245_.rewind();
      return read(p_378245_);
   }

   public static NativeImage read(NativeImage.@Nullable Format p_85052_, ByteBuffer p_85053_) throws IOException {
      if (p_85052_ != null && !p_85052_.supportedByStb()) {
         throw new UnsupportedOperationException("Don't know how to read format " + p_85052_);
      }

      if (MemoryUtil.memAddress(p_85053_) == 0L) {
         throw new IllegalArgumentException("Invalid buffer");
      }

      PngInfo.validateHeader(p_85053_);
      MemoryStack memorystack = MemoryStack.stackPush();

      NativeImage nativeimage;
      try {
         IntBuffer intbuffer = memorystack.mallocInt(1);
         IntBuffer intbuffer1 = memorystack.mallocInt(1);
         IntBuffer intbuffer2 = memorystack.mallocInt(1);
         ByteBuffer bytebuffer = STBImage.stbi_load_from_memory(p_85053_, intbuffer, intbuffer1, intbuffer2, p_85052_ == null ? 0 : p_85052_.components);
         if (bytebuffer == null) {
            throw new IOException("Could not load image: " + STBImage.stbi_failure_reason());
         }

         long i = MemoryUtil.memAddress(bytebuffer);
         MEMORY_POOL.malloc(i, bytebuffer.limit());
         nativeimage = new NativeImage(
            p_85052_ == null ? NativeImage.Format.getStbFormat(intbuffer2.get(0)) : p_85052_, intbuffer.get(0), intbuffer1.get(0), true, i
         );
      } catch (Throwable var11) {
         if (memorystack != null) {
            try {
               memorystack.close();
            } catch (Throwable var10) {
               var11.addSuppressed(var10);
            }
         }

         throw var11;
      }

      if (memorystack != null) {
         memorystack.close();
      }

      return nativeimage;
   }

   private void checkAllocated() {
      if (this.pixels == 0L) {
         throw new IllegalStateException("Image is not allocated.");
      }
   }

   @Override
   public void close() {
      if (this.pixels != 0L) {
         if (this.useStbFree) {
            STBImage.nstbi_image_free(this.pixels);
         } else {
            MemoryUtil.nmemFree(this.pixels);
         }

         MEMORY_POOL.free(this.pixels);
      }

      this.pixels = 0L;
   }

   public int getWidth() {
      return this.width;
   }

   public int getHeight() {
      return this.height;
   }

   public NativeImage.Format format() {
      return this.format;
   }

   private int getPixelABGR(int p_366605_, int p_368577_) {
      if (this.format != NativeImage.Format.RGBA) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "getPixelRGBA only works on RGBA images; have %s", this.format));
      }

      if (this.isOutsideBounds(p_366605_, p_368577_)) {
         throw new IllegalArgumentException(
            String.format(Locale.ROOT, "(%s, %s) outside of image bounds (%s, %s)", p_366605_, p_368577_, this.width, this.height)
         );
      }

      this.checkAllocated();
      long i = (p_366605_ + (long)p_368577_ * this.width) * 4L;
      return MemoryUtil.memGetInt(this.pixels + i);
   }

   public int getPixel(int p_364178_, int p_364265_) {
      return ARGB.fromABGR(this.getPixelABGR(p_364178_, p_364265_));
   }

   public void setPixelABGR(int p_366486_, int p_360988_, int p_364498_) {
      if (this.format != NativeImage.Format.RGBA) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "setPixelRGBA only works on RGBA images; have %s", this.format));
      }

      if (this.isOutsideBounds(p_366486_, p_360988_)) {
         throw new IllegalArgumentException(
            String.format(Locale.ROOT, "(%s, %s) outside of image bounds (%s, %s)", p_366486_, p_360988_, this.width, this.height)
         );
      }

      this.checkAllocated();
      long i = (p_366486_ + (long)p_360988_ * this.width) * 4L;
      MemoryUtil.memPutInt(this.pixels + i, p_364498_);
   }

   public void setPixel(int p_364494_, int p_368505_, int p_361991_) {
      this.setPixelABGR(p_364494_, p_368505_, ARGB.toABGR(p_361991_));
   }

   public NativeImage mappedCopy(IntUnaryOperator p_267084_) {
      if (this.format != NativeImage.Format.RGBA) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "function application only works on RGBA images; have %s", this.format));
      }

      this.checkAllocated();
      NativeImage nativeimage = new NativeImage(this.width, this.height, false);
      int i = this.width * this.height;
      IntBuffer intbuffer = MemoryUtil.memIntBuffer(this.pixels, i);
      IntBuffer intbuffer1 = MemoryUtil.memIntBuffer(nativeimage.pixels, i);

      for (int j = 0; j < i; j++) {
         int k = ARGB.fromABGR(intbuffer.get(j));
         int l = p_267084_.applyAsInt(k);
         intbuffer1.put(j, ARGB.toABGR(l));
      }

      return nativeimage;
   }

   public int[] getPixelsABGR() {
      if (this.format != NativeImage.Format.RGBA) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "getPixels only works on RGBA images; have %s", this.format));
      }

      this.checkAllocated();
      int[] aint = new int[this.width * this.height];
      MemoryUtil.memIntBuffer(this.pixels, this.width * this.height).get(aint);
      return aint;
   }

   public int[] getPixels() {
      int[] aint = this.getPixelsABGR();

      for (int i = 0; i < aint.length; i++) {
         aint[i] = ARGB.fromABGR(aint[i]);
      }

      return aint;
   }

   public byte getLuminanceOrAlpha(int p_85088_, int p_85089_) {
      if (!this.format.hasLuminanceOrAlpha()) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "no luminance or alpha in %s", this.format));
      }

      if (this.isOutsideBounds(p_85088_, p_85089_)) {
         throw new IllegalArgumentException(
            String.format(Locale.ROOT, "(%s, %s) outside of image bounds (%s, %s)", p_85088_, p_85089_, this.width, this.height)
         );
      }

      int i = (p_85088_ + p_85089_ * this.width) * this.format.components() + this.format.luminanceOrAlphaOffset() / 8;
      return MemoryUtil.memGetByte(this.pixels + i);
   }

   @Deprecated
   public int[] makePixelArray() {
      if (this.format != NativeImage.Format.RGBA) {
         throw new UnsupportedOperationException("can only call makePixelArray for RGBA images.");
      }

      this.checkAllocated();
      int[] aint = new int[this.getWidth() * this.getHeight()];

      for (int i = 0; i < this.getHeight(); i++) {
         for (int j = 0; j < this.getWidth(); j++) {
            aint[j + i * this.getWidth()] = this.getPixel(j, i);
         }
      }

      return aint;
   }

   public void writeToFile(File p_85057_) throws IOException {
      this.writeToFile(p_85057_.toPath());
   }

   public boolean copyFromFont(FT_Face p_334818_, int p_85070_) {
      if (this.format.components() != 1) {
         throw new IllegalArgumentException("Can only write fonts into 1-component images.");
      } else if (FreeTypeUtil.checkError(FreeType.FT_Load_Glyph(p_334818_, p_85070_, 4), "Loading glyph")) {
         return false;
      } else {
         FT_GlyphSlot ft_glyphslot = Objects.requireNonNull(p_334818_.glyph(), "Glyph not initialized");
         FT_Bitmap ft_bitmap = ft_glyphslot.bitmap();
         if (ft_bitmap.pixel_mode() != 2) {
            throw new IllegalStateException("Rendered glyph was not 8-bit grayscale");
         } else if (ft_bitmap.width() == this.getWidth() && ft_bitmap.rows() == this.getHeight()) {
            int i = ft_bitmap.width() * ft_bitmap.rows();
            ByteBuffer bytebuffer = Objects.requireNonNull(ft_bitmap.buffer(i), "Glyph has no bitmap");
            MemoryUtil.memCopy(MemoryUtil.memAddress(bytebuffer), this.pixels, i);
            return true;
         } else {
            throw new IllegalArgumentException(
               String.format(
                  Locale.ROOT,
                  "Glyph bitmap of size %sx%s does not match image of size: %sx%s",
                  ft_bitmap.width(),
                  ft_bitmap.rows(),
                  this.getWidth(),
                  this.getHeight()
               )
            );
         }
      }
   }

   public void writeToFile(Path p_85067_) throws IOException {
      if (!this.format.supportedByStb()) {
         throw new UnsupportedOperationException("Don't know how to write format " + this.format);
      }

      this.checkAllocated();

      try (WritableByteChannel writablebytechannel = Files.newByteChannel(p_85067_, OPEN_OPTIONS)) {
         if (!this.writeToChannel(writablebytechannel)) {
            throw new IOException("Could not write image to the PNG file \"" + p_85067_.toAbsolutePath() + "\": " + STBImage.stbi_failure_reason());
         }
      }
   }

   private boolean writeToChannel(WritableByteChannel p_85065_) throws IOException {
      NativeImage.WriteCallback nativeimage$writecallback = new NativeImage.WriteCallback(p_85065_);

      boolean flag;
      try {
         int i = Math.min(this.getHeight(), Integer.MAX_VALUE / this.getWidth() / this.format.components());
         if (i < this.getHeight()) {
            LOGGER.warn("Dropping image height from {} to {} to fit the size into 32-bit signed int", this.getHeight(), i);
         }

         if (STBImageWrite.nstbi_write_png_to_func(nativeimage$writecallback.address(), 0L, this.getWidth(), i, this.format.components(), this.pixels, 0) != 0) {
            nativeimage$writecallback.throwIfException();
            return true;
         }

         flag = false;
      } finally {
         nativeimage$writecallback.free();
      }

      return flag;
   }

   public void copyFrom(NativeImage p_85055_) {
      if (p_85055_.format() != this.format) {
         throw new UnsupportedOperationException("Image formats don't match.");
      }

      int i = this.format.components();
      this.checkAllocated();
      p_85055_.checkAllocated();
      if (this.width == p_85055_.width) {
         MemoryUtil.memCopy(p_85055_.pixels, this.pixels, Math.min(this.size, p_85055_.size));
      } else {
         int j = Math.min(this.getWidth(), p_85055_.getWidth());
         int k = Math.min(this.getHeight(), p_85055_.getHeight());

         for (int l = 0; l < k; l++) {
            int i1 = l * p_85055_.getWidth() * i;
            int j1 = l * this.getWidth() * i;
            MemoryUtil.memCopy(p_85055_.pixels + i1, this.pixels + j1, j);
         }
      }
   }

   public void fillRect(int p_84998_, int p_84999_, int p_85000_, int p_85001_, int p_85002_) {
      for (int i = p_84999_; i < p_84999_ + p_85001_; i++) {
         for (int j = p_84998_; j < p_84998_ + p_85000_; j++) {
            this.setPixel(j, i, p_85002_);
         }
      }
   }

   public void copyRect(int p_85026_, int p_85027_, int p_85028_, int p_85029_, int p_85030_, int p_85031_, boolean p_85032_, boolean p_85033_) {
      this.copyRect(this, p_85026_, p_85027_, p_85026_ + p_85028_, p_85027_ + p_85029_, p_85030_, p_85031_, p_85032_, p_85033_);
   }

   public void copyRect(
      NativeImage p_261644_, int p_262056_, int p_261490_, int p_261959_, int p_262110_, int p_261522_, int p_261505_, boolean p_261480_, boolean p_261622_
   ) {
      for (int i = 0; i < p_261505_; i++) {
         for (int j = 0; j < p_261522_; j++) {
            int k = p_261480_ ? p_261522_ - 1 - j : j;
            int l = p_261622_ ? p_261505_ - 1 - i : i;
            int i1 = this.getPixelABGR(p_262056_ + j, p_261490_ + i);
            p_261644_.setPixelABGR(p_261959_ + k, p_262110_ + l, i1);
         }
      }
   }

   public void resizeSubRectTo(int p_85035_, int p_85036_, int p_85037_, int p_85038_, NativeImage p_85039_) {
      this.checkAllocated();
      if (p_85039_.format() != this.format) {
         throw new UnsupportedOperationException("resizeSubRectTo only works for images of the same format.");
      }

      int i = this.format.components();
      STBImageResize.nstbir_resize_uint8(
         this.pixels + (p_85035_ + p_85036_ * this.getWidth()) * i,
         p_85037_,
         p_85038_,
         this.getWidth() * i,
         p_85039_.pixels,
         p_85039_.getWidth(),
         p_85039_.getHeight(),
         0,
         i
      );
   }

   public void untrack() {
      DebugMemoryUntracker.untrack(this.pixels);
   }

   public long getPointer() {
      return this.pixels;
   }

   @OnlyIn(Dist.CLIENT)
   public enum Format {
      RGBA(4, true, true, true, false, true, 0, 8, 16, 255, 24, true),
      RGB(3, true, true, true, false, false, 0, 8, 16, 255, 255, true),
      LUMINANCE_ALPHA(2, false, false, false, true, true, 255, 255, 255, 0, 8, true),
      LUMINANCE(1, false, false, false, true, false, 0, 0, 0, 0, 255, true);

      final int components;
      private final boolean hasRed;
      private final boolean hasGreen;
      private final boolean hasBlue;
      private final boolean hasLuminance;
      private final boolean hasAlpha;
      private final int redOffset;
      private final int greenOffset;
      private final int blueOffset;
      private final int luminanceOffset;
      private final int alphaOffset;
      private final boolean supportedByStb;

      Format(
         final int p_85148_,
         final boolean p_85150_,
         final boolean p_85151_,
         final boolean p_85152_,
         final boolean p_85153_,
         final boolean p_85154_,
         final int p_85149_,
         final int p_85155_,
         final int p_85156_,
         final int p_85157_,
         final int p_85158_,
         final boolean p_85160_
      ) {
         this.components = p_85148_;
         this.hasRed = p_85150_;
         this.hasGreen = p_85151_;
         this.hasBlue = p_85152_;
         this.hasLuminance = p_85153_;
         this.hasAlpha = p_85154_;
         this.redOffset = p_85149_;
         this.greenOffset = p_85155_;
         this.blueOffset = p_85156_;
         this.luminanceOffset = p_85157_;
         this.alphaOffset = p_85158_;
         this.supportedByStb = p_85160_;
      }

      public int components() {
         return this.components;
      }

      public boolean hasRed() {
         return this.hasRed;
      }

      public boolean hasGreen() {
         return this.hasGreen;
      }

      public boolean hasBlue() {
         return this.hasBlue;
      }

      public boolean hasLuminance() {
         return this.hasLuminance;
      }

      public boolean hasAlpha() {
         return this.hasAlpha;
      }

      public int redOffset() {
         return this.redOffset;
      }

      public int greenOffset() {
         return this.greenOffset;
      }

      public int blueOffset() {
         return this.blueOffset;
      }

      public int luminanceOffset() {
         return this.luminanceOffset;
      }

      public int alphaOffset() {
         return this.alphaOffset;
      }

      public boolean hasLuminanceOrRed() {
         return this.hasLuminance || this.hasRed;
      }

      public boolean hasLuminanceOrGreen() {
         return this.hasLuminance || this.hasGreen;
      }

      public boolean hasLuminanceOrBlue() {
         return this.hasLuminance || this.hasBlue;
      }

      public boolean hasLuminanceOrAlpha() {
         return this.hasLuminance || this.hasAlpha;
      }

      public int luminanceOrRedOffset() {
         return this.hasLuminance ? this.luminanceOffset : this.redOffset;
      }

      public int luminanceOrGreenOffset() {
         return this.hasLuminance ? this.luminanceOffset : this.greenOffset;
      }

      public int luminanceOrBlueOffset() {
         return this.hasLuminance ? this.luminanceOffset : this.blueOffset;
      }

      public int luminanceOrAlphaOffset() {
         return this.hasLuminance ? this.luminanceOffset : this.alphaOffset;
      }

      public boolean supportedByStb() {
         return this.supportedByStb;
      }

      static NativeImage.Format getStbFormat(int p_85168_) {
         switch (p_85168_) {
            case 1:
               return LUMINANCE;
            case 2:
               return LUMINANCE_ALPHA;
            case 3:
               return RGB;
            case 4:
            default:
               return RGBA;
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class WriteCallback extends STBIWriteCallback {
      private final WritableByteChannel output;
      private @Nullable IOException exception;

      WriteCallback(WritableByteChannel p_85198_) {
         this.output = p_85198_;
      }

      public void invoke(long p_85204_, long p_85205_, int p_85206_) {
         ByteBuffer bytebuffer = getData(p_85205_, p_85206_);

         try {
            this.output.write(bytebuffer);
         } catch (IOException ioexception) {
            this.exception = ioexception;
         }
      }

      public void throwIfException() throws IOException {
         if (this.exception != null) {
            throw this.exception;
         }
      }
   }
}
