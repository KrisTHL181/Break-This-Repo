package com.mojang.blaze3d.platform;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;
import net.minecraft.util.ARGB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class TextureUtil {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int MIN_MIPMAP_LEVEL = 0;
    private static final int DEFAULT_IMAGE_BUFFER_SIZE = 8192;
    private static final int[][] DIRECTIONS = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    public static ByteBuffer readResource(final InputStream inputStream) throws IOException {
        ReadableByteChannel channel = Channels.newChannel(inputStream);
        return channel instanceof SeekableByteChannel seekableChannel ? readResource(channel, (int)seekableChannel.size() + 1) : readResource(channel, 8192);
    }

    private static ByteBuffer readResource(final ReadableByteChannel channel, final int expectedSize) throws IOException {
        ByteBuffer buffer = MemoryUtil.memAlloc(expectedSize);

        try {
            while (channel.read(buffer) != -1) {
                if (!buffer.hasRemaining()) {
                    buffer = MemoryUtil.memRealloc(buffer, buffer.capacity() * 2);
                }
            }

            buffer.flip();
            return buffer;
        } catch (IOException e) {
            MemoryUtil.memFree(buffer);
            throw e;
        }
    }

    public static void writeAsPNG(final Path dir, final String prefix, final GpuTexture texture, final int maxMipLevel, final IntUnaryOperator pixelModifier) {
        RenderSystem.assertOnRenderThread();
        long bufferLength = 0L;

        for (int i = 0; i <= maxMipLevel; i++) {
            bufferLength += (long)texture.getFormat().blockSize() * texture.getWidth(i) * texture.getHeight(i);
        }

        if (bufferLength > 2147483647L) {
            throw new IllegalArgumentException("Exporting textures larger than 2GB is not supported");
        }

        if (texture.getFormat() != GpuFormat.RGBA8_UNORM) {
            throw new IllegalArgumentException("Exporting textures other than RGBA8_UNORM is not supported");
        }

        GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> "Texture output buffer", 9, bufferLength);
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        Runnable onCopyComplete = () -> {
            try (GpuBufferSlice.MappedView read = buffer.map(true, false)) {
                ByteBuffer data = read.data();
                IntUnaryOperator decodeTexel = data::getInt;
                int offsetx = 0;

                for (int ix = 0; ix <= maxMipLevel; ix++) {
                    int mipWidth = texture.getWidth(ix);
                    int mipHeight = texture.getHeight(ix);

                    try (NativeImage image = new NativeImage(mipWidth, mipHeight, false)) {
                        for (int y = 0; y < mipHeight; y++) {
                            for (int x = 0; x < mipWidth; x++) {
                                int argb = decodeTexel.applyAsInt(offsetx + (x + y * mipWidth) * texture.getFormat().blockSize());
                                image.setPixelABGR(x, y, pixelModifier.applyAsInt(argb));
                            }
                        }

                        Path target = dir.resolve(prefix + "_" + ix + ".png");
                        image.writeToFile(target);
                        LOGGER.debug("Exported png to: {}", target.toAbsolutePath());
                    } catch (IOException e) {
                        LOGGER.debug("Unable to write: ", e);
                    }

                    offsetx += texture.getFormat().blockSize() * mipWidth * mipHeight;
                }
            }

            buffer.close();
        };
        AtomicInteger completedCopies = new AtomicInteger();
        int offset = 0;

        for (int i = 0; i <= maxMipLevel; i++) {
            commandEncoder.copyTextureToBuffer(texture, buffer, offset, () -> {
                if (completedCopies.getAndIncrement() == maxMipLevel) {
                    onCopyComplete.run();
                }
            }, i);
            offset += texture.getFormat().blockSize() * texture.getWidth(i) * texture.getHeight(i);
        }
    }

    public static Path getDebugTexturePath(final Path root) {
        return root.resolve("screenshots").resolve("debug");
    }

    public static Path getDebugTexturePath() {
        return getDebugTexturePath(Path.of("."));
    }

    public static void solidify(final NativeImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] nearestColor = new int[width * height];
        int[] distances = new int[width * height];
        Arrays.fill(distances, Integer.MAX_VALUE);
        IntArrayFIFOQueue queue = new IntArrayFIFOQueue();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int color = image.getPixel(x, y);
                if (ARGB.alpha(color) != 0) {
                    int packedCoordinates = pack(x, y, width);
                    distances[packedCoordinates] = 0;
                    nearestColor[packedCoordinates] = color;
                    queue.enqueue(packedCoordinates);
                }
            }
        }

        while (!queue.isEmpty()) {
            int packedCoordinates = queue.dequeueInt();
            int x = x(packedCoordinates, width);
            int y = y(packedCoordinates, width);

            for (int[] direction : DIRECTIONS) {
                int neighborX = x + direction[0];
                int neighborY = y + direction[1];
                int packedNeighborCoordinates = pack(neighborX, neighborY, width);
                if (neighborX >= 0
                    && neighborY >= 0
                    && neighborX < width
                    && neighborY < height
                    && distances[packedNeighborCoordinates] > distances[packedCoordinates] + 1) {
                    distances[packedNeighborCoordinates] = distances[packedCoordinates] + 1;
                    nearestColor[packedNeighborCoordinates] = nearestColor[packedCoordinates];
                    queue.enqueue(packedNeighborCoordinates);
                }
            }
        }

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int color = image.getPixel(x, y);
                if (ARGB.alpha(color) == 0) {
                    image.setPixel(x, y, ARGB.color(0, nearestColor[pack(x, y, width)]));
                } else {
                    image.setPixel(x, y, color);
                }
            }
        }
    }

    public static void fillEmptyAreasWithDarkColor(final NativeImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int darkestColor = -1;
        int minBrightness = Integer.MAX_VALUE;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int color = image.getPixel(x, y);
                int alpha = ARGB.alpha(color);
                if (alpha != 0) {
                    int red = ARGB.red(color);
                    int green = ARGB.green(color);
                    int blue = ARGB.blue(color);
                    int brightness = red + green + blue;
                    if (brightness < minBrightness) {
                        minBrightness = brightness;
                        darkestColor = color;
                    }
                }
            }
        }

        int darkRed = 3 * ARGB.red(darkestColor) / 4;
        int darkGreen = 3 * ARGB.green(darkestColor) / 4;
        int darkBlue = 3 * ARGB.blue(darkestColor) / 4;
        int darkenedColor = ARGB.color(0, darkRed, darkGreen, darkBlue);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int color = image.getPixel(x, y);
                if (ARGB.alpha(color) == 0) {
                    image.setPixel(x, y, darkenedColor);
                }
            }
        }
    }

    private static int pack(final int x, final int y, final int width) {
        return x + y * width;
    }

    private static int x(final int packed, final int width) {
        return packed % width;
    }

    private static int y(final int packed, final int width) {
        return packed / width;
    }
}