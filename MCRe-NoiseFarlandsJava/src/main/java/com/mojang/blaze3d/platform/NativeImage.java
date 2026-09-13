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

    public NativeImage(final int width, final int height, final boolean zero) {
        this(NativeImage.Format.RGBA, width, height, zero);
    }

    public NativeImage(final NativeImage.Format format, final int width, final int height, final boolean zero) {
        if (width > 0 && height > 0) {
            this.format = format;
            this.width = width;
            this.height = height;
            this.size = (long)width * height * format.components();
            this.useStbFree = false;
            if (zero) {
                this.pixels = MemoryUtil.nmemCalloc(1L, this.size);
            } else {
                this.pixels = MemoryUtil.nmemAlloc(this.size);
            }

            MEMORY_POOL.malloc(this.pixels, (int)this.size);
            if (this.pixels == 0L) {
                throw new IllegalStateException("Unable to allocate texture of size " + width + "x" + height + " (" + format.components() + " channels)");
            }
        } else {
            throw new IllegalArgumentException("Invalid texture size: " + width + "x" + height);
        }
    }

    public NativeImage(final NativeImage.Format format, final int width, final int height, final boolean useStbFree, final long pixels) {
        if (width > 0 && height > 0) {
            this.format = format;
            this.width = width;
            this.height = height;
            this.useStbFree = useStbFree;
            this.pixels = pixels;
            this.size = (long)width * height * format.components();
        } else {
            throw new IllegalArgumentException("Invalid texture size: " + width + "x" + height);
        }
    }

    @Override
    public String toString() {
        return "NativeImage[" + this.format + " " + this.width + "x" + this.height + "@" + this.pixels + (this.useStbFree ? "S" : "N") + "]";
    }

    private boolean isOutsideBounds(final int x, final int y) {
        return x < 0 || x >= this.width || y < 0 || y >= this.height;
    }

    public static NativeImage read(final InputStream inputStream) throws IOException {
        return read(NativeImage.Format.RGBA, inputStream);
    }

    public static NativeImage read(final NativeImage.@Nullable Format format, final InputStream inputStream) throws IOException {
        ByteBuffer file = null;

        try {
            file = TextureUtil.readResource(inputStream);
            return read(format, file);
        } finally {
            MemoryUtil.memFree(file);
            IOUtils.closeQuietly(inputStream);
        }
    }

    public static NativeImage read(final ByteBuffer bytes) throws IOException {
        return read(NativeImage.Format.RGBA, bytes);
    }

    public static NativeImage read(final byte[] bytes) throws IOException {
        MemoryStack memoryStack = MemoryStack.stackGet();
        int bytesAvailable = memoryStack.getPointer();
        if (bytesAvailable < bytes.length) {
            ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);

            try {
                return putAndRead(buffer, bytes);
            } finally {
                MemoryUtil.memFree(buffer);
            }
        } else {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                ByteBuffer buffer = stack.malloc(bytes.length);
                return putAndRead(buffer, bytes);
            }
        }
    }

    private static NativeImage putAndRead(final ByteBuffer nativeBuffer, final byte[] bytes) throws IOException {
        nativeBuffer.put(bytes);
        nativeBuffer.rewind();
        return read(nativeBuffer);
    }

    public static NativeImage read(final NativeImage.@Nullable Format format, final ByteBuffer bytes) throws IOException {
        if (format != null && !format.supportedByStb()) {
            throw new UnsupportedOperationException("Don't know how to read format " + format);
        }

        if (MemoryUtil.memAddress(bytes) == 0L) {
            throw new IllegalArgumentException("Invalid buffer");
        }

        PngInfo.validateHeader(bytes);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer comp = stack.mallocInt(1);
            ByteBuffer pixels = STBImage.stbi_load_from_memory(bytes, w, h, comp, format == null ? 0 : format.components);
            if (pixels == null) {
                throw new IOException("Could not load image: " + STBImage.stbi_failure_reason());
            }

            long address = MemoryUtil.memAddress(pixels);
            MEMORY_POOL.malloc(address, pixels.limit());
            return new NativeImage(format == null ? NativeImage.Format.getStbFormat(comp.get(0)) : format, w.get(0), h.get(0), true, address);
        }
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

    public boolean isClosed() {
        return this.pixels == 0L;
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

    private int getPixelABGR(final int x, final int y) {
        if (this.format != NativeImage.Format.RGBA) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "getPixelRGBA only works on RGBA images; have %s", this.format));
        }

        if (this.isOutsideBounds(x, y)) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "(%s, %s) outside of image bounds (%s, %s)", x, y, this.width, this.height));
        }

        this.checkAllocated();
        long offset = (x + (long)y * this.width) * 4L;
        return MemoryUtil.memGetInt(this.pixels + offset);
    }

    public int getPixel(final int x, final int y) {
        return ARGB.fromABGR(this.getPixelABGR(x, y));
    }

    public void setPixelABGR(final int x, final int y, final int pixel) {
        if (this.format != NativeImage.Format.RGBA) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "setPixelRGBA only works on RGBA images; have %s", this.format));
        }

        if (this.isOutsideBounds(x, y)) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "(%s, %s) outside of image bounds (%s, %s)", x, y, this.width, this.height));
        }

        this.checkAllocated();
        long offset = (x + (long)y * this.width) * 4L;
        MemoryUtil.memPutInt(this.pixels + offset, pixel);
    }

    public void setPixel(final int x, final int y, final int pixel) {
        this.setPixelABGR(x, y, ARGB.toABGR(pixel));
    }

    public NativeImage mappedCopy(final IntUnaryOperator function) {
        if (this.format != NativeImage.Format.RGBA) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "function application only works on RGBA images; have %s", this.format));
        }

        this.checkAllocated();
        NativeImage result = new NativeImage(this.width, this.height, false);
        int pixelCount = this.width * this.height;
        IntBuffer sourceBuffer = MemoryUtil.memIntBuffer(this.pixels, pixelCount);
        IntBuffer targetBuffer = MemoryUtil.memIntBuffer(result.pixels, pixelCount);

        for (int i = 0; i < pixelCount; i++) {
            int pixel = ARGB.fromABGR(sourceBuffer.get(i));
            int modified = function.applyAsInt(pixel);
            targetBuffer.put(i, ARGB.toABGR(modified));
        }

        return result;
    }

    public int[] getPixelsABGR() {
        if (this.format != NativeImage.Format.RGBA) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "getPixels only works on RGBA images; have %s", this.format));
        }

        this.checkAllocated();
        int[] result = new int[this.width * this.height];
        MemoryUtil.memIntBuffer(this.pixels, this.width * this.height).get(result);
        return result;
    }

    public int[] getPixels() {
        int[] result = this.getPixelsABGR();

        for (int i = 0; i < result.length; i++) {
            result[i] = ARGB.fromABGR(result[i]);
        }

        return result;
    }

    public byte getLuminanceOrAlpha(final int x, final int y) {
        if (!this.format.hasLuminanceOrAlpha()) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "no luminance or alpha in %s", this.format));
        }

        if (this.isOutsideBounds(x, y)) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "(%s, %s) outside of image bounds (%s, %s)", x, y, this.width, this.height));
        }

        int offset = (x + y * this.width) * this.format.components() + this.format.luminanceOrAlphaOffset() / 8;
        return MemoryUtil.memGetByte(this.pixels + offset);
    }

    @Deprecated
    public int[] makePixelArray() {
        if (this.format != NativeImage.Format.RGBA) {
            throw new UnsupportedOperationException("can only call makePixelArray for RGBA images.");
        }

        this.checkAllocated();
        int[] pixels = new int[this.getWidth() * this.getHeight()];

        for (int y = 0; y < this.getHeight(); y++) {
            for (int x = 0; x < this.getWidth(); x++) {
                pixels[x + y * this.getWidth()] = this.getPixel(x, y);
            }
        }

        return pixels;
    }

    public void writeToFile(final File file) throws IOException {
        this.writeToFile(file.toPath());
    }

    public boolean copyFromFont(final FT_Face face, final int index) {
        if (this.format.components() != 1) {
            throw new IllegalArgumentException("Can only write fonts into 1-component images.");
        } else if (FreeTypeUtil.checkError(FreeType.FT_Load_Glyph(face, index, 4), "Loading glyph")) {
            return false;
        } else {
            FT_GlyphSlot glyph = Objects.requireNonNull(face.glyph(), "Glyph not initialized");
            FT_Bitmap bitmap = glyph.bitmap();
            if (bitmap.pixel_mode() != 2) {
                throw new IllegalStateException("Rendered glyph was not 8-bit grayscale");
            } else if (bitmap.width() == this.getWidth() && bitmap.rows() == this.getHeight()) {
                int bufferSize = bitmap.width() * bitmap.rows();
                ByteBuffer buffer = Objects.requireNonNull(bitmap.buffer(bufferSize), "Glyph has no bitmap");
                MemoryUtil.memCopy(MemoryUtil.memAddress(buffer), this.pixels, bufferSize);
                return true;
            } else {
                throw new IllegalArgumentException(
                    String.format(
                        Locale.ROOT,
                        "Glyph bitmap of size %sx%s does not match image of size: %sx%s",
                        bitmap.width(),
                        bitmap.rows(),
                        this.getWidth(),
                        this.getHeight()
                    )
                );
            }
        }
    }

    public void writeToFile(final Path file) throws IOException {
        if (!this.format.supportedByStb()) {
            throw new UnsupportedOperationException("Don't know how to write format " + this.format);
        }

        this.checkAllocated();

        try (WritableByteChannel out = Files.newByteChannel(file, OPEN_OPTIONS)) {
            if (!this.writeToChannel(out)) {
                throw new IOException("Could not write image to the PNG file \"" + file.toAbsolutePath() + "\": " + STBImage.stbi_failure_reason());
            }
        }
    }

    private boolean writeToChannel(final WritableByteChannel output) throws IOException {
        NativeImage.WriteCallback writer = new NativeImage.WriteCallback(output);

        try {
            int height = Math.min(this.getHeight(), Integer.MAX_VALUE / this.getWidth() / this.format.components());
            if (height < this.getHeight()) {
                LOGGER.warn("Dropping image height from {} to {} to fit the size into 32-bit signed int", this.getHeight(), height);
            }

            if (STBImageWrite.nstbi_write_png_to_func(writer.address(), 0L, this.getWidth(), height, this.format.components(), this.pixels, 0) == 0) {
                return false;
            }

            writer.throwIfException();
            return true;
        } finally {
            writer.free();
        }
    }

    public void copyFrom(final NativeImage from) {
        if (from.format() != this.format) {
            throw new UnsupportedOperationException("Image formats don't match.");
        }

        int components = this.format.components();
        this.checkAllocated();
        from.checkAllocated();
        if (this.width == from.width) {
            MemoryUtil.memCopy(from.pixels, this.pixels, Math.min(this.size, from.size));
        } else {
            int minWidth = Math.min(this.getWidth(), from.getWidth());
            int minHeight = Math.min(this.getHeight(), from.getHeight());

            for (int y = 0; y < minHeight; y++) {
                int fromOffset = y * from.getWidth() * components;
                int toOffset = y * this.getWidth() * components;
                MemoryUtil.memCopy(from.pixels + fromOffset, this.pixels + toOffset, minWidth);
            }
        }
    }

    public void fillRect(final int xs, final int ys, final int width, final int height, final int pixel) {
        for (int y = ys; y < ys + height; y++) {
            for (int x = xs; x < xs + width; x++) {
                this.setPixel(x, y, pixel);
            }
        }
    }

    public void copyRect(
        final int startX, final int startY, final int offsetX, final int offsetY, final int sizeX, final int sizeY, final boolean swapX, final boolean swapY
    ) {
        this.copyRect(this, startX, startY, startX + offsetX, startY + offsetY, sizeX, sizeY, swapX, swapY);
    }

    public void copyRect(
        final NativeImage target,
        final int sourceX,
        final int sourceY,
        final int targetX,
        final int targetY,
        final int sizeX,
        final int sizeY,
        final boolean swapX,
        final boolean swapY
    ) {
        for (int y = 0; y < sizeY; y++) {
            for (int x = 0; x < sizeX; x++) {
                int dx = swapX ? sizeX - 1 - x : x;
                int dy = swapY ? sizeY - 1 - y : y;
                int source = this.getPixelABGR(sourceX + x, sourceY + y);
                target.setPixelABGR(targetX + dx, targetY + dy, source);
            }
        }
    }

    public void resizeSubRectTo(final int sourceX, final int sourceY, final int sizeX, final int sizeY, final NativeImage to) {
        this.checkAllocated();
        if (to.format() != this.format) {
            throw new UnsupportedOperationException("resizeSubRectTo only works for images of the same format.");
        }

        int components = this.format.components();
        STBImageResize.nstbir_resize_uint8_linear(
            this.pixels + (sourceX + sourceY * this.getWidth()) * components,
            sizeX,
            sizeY,
            this.getWidth() * components,
            to.pixels,
            to.getWidth(),
            to.getHeight(),
            0,
            components
        );
    }

    public void untrack() {
        DebugMemoryUntracker.untrack(this.pixels);
    }

    public long getPointer() {
        return this.pixels;
    }

    public ByteBuffer getPixelBytes() {
        return MemoryUtil.memByteBuffer(this.pixels, Math.toIntExact(this.size));
    }

    public Transparency computeTransparency(final int x0, final int y0, final int x1, final int y1) {
        this.checkAllocated();
        if (this.format != NativeImage.Format.RGBA) {
            return Transparency.NONE;
        }

        if (x0 >= 0 && y0 >= 0 && x1 <= this.width && y1 <= this.height) {
            if ((long)this.width * this.height * 4L > 2147483647L) {
                throw new IllegalArgumentException("Image of size " + this.width + "x" + this.height + " is too large to compute translucency");
            }

            boolean hasTransparentPixel = false;
            boolean hasTranslucentPixel = false;
            IntBuffer buffer = MemoryUtil.memIntBuffer(this.pixels, this.width * this.height);

            for (int y = y0; y < y1; y++) {
                for (int x = x0; x < x1; x++) {
                    int alpha = ARGB.alpha(buffer.get(x + y * this.width));
                    if (alpha == 0) {
                        hasTransparentPixel = true;
                    } else if (alpha != 255) {
                        hasTranslucentPixel = true;
                    }
                }
            }

            return Transparency.of(hasTransparentPixel, hasTranslucentPixel);
        } else {
            throw new IllegalArgumentException(
                "Cannot compute translucency out of bounds: [" + x0 + ", " + y0 + ", " + x1 + ", " + y1 + "] in " + this.width + "x" + this.height + " image"
            );
        }
    }

    public Transparency computeTransparency() {
        return this.computeTransparency(0, 0, this.width, this.height);
    }

    @OnlyIn(Dist.CLIENT)
    public enum Format {
        RGBA(4, true, true, true, false, true, 0, 8, 16, 255, 24, true),
        RGB(3, true, true, true, false, false, 0, 8, 16, 255, 255, true),
        LUMINANCE_ALPHA(2, false, false, false, true, true, 255, 255, 255, 0, 8, true),
        LUMINANCE(1, false, false, false, true, false, 0, 0, 0, 0, 255, true);

        private final int components;
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
            final int components,
            final boolean hasRed,
            final boolean hasGreen,
            final boolean hasBlue,
            final boolean hasLuminance,
            final boolean hasAlpha,
            final int redOffset,
            final int greenOffset,
            final int blueOffset,
            final int luminanceOffset,
            final int alphaOffset,
            final boolean supportedByStb
        ) {
            this.components = components;
            this.hasRed = hasRed;
            this.hasGreen = hasGreen;
            this.hasBlue = hasBlue;
            this.hasLuminance = hasLuminance;
            this.hasAlpha = hasAlpha;
            this.redOffset = redOffset;
            this.greenOffset = greenOffset;
            this.blueOffset = blueOffset;
            this.luminanceOffset = luminanceOffset;
            this.alphaOffset = alphaOffset;
            this.supportedByStb = supportedByStb;
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

        private static NativeImage.Format getStbFormat(final int i) {
            switch (i) {
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
    private static class WriteCallback extends STBIWriteCallback {
        private final WritableByteChannel output;
        private @Nullable IOException exception;

        private WriteCallback(final WritableByteChannel output) {
            this.output = output;
        }

        @Override
        public void invoke(final long context, final long data, final int size) {
            ByteBuffer dataBuf = getData(data, size);

            try {
                this.output.write(dataBuf);
            } catch (IOException e) {
                this.exception = e;
            }
        }

        public void throwIfException() throws IOException {
            if (this.exception != null) {
                throw this.exception;
            }
        }
    }
}