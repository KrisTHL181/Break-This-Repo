package net.minecraft.client.gui.font.providers;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.blaze3d.font.GlyphBitmap;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.UnbakedGlyph;
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
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int GLYPH_HEIGHT = 16;
    private static final int DIGITS_PER_BYTE = 2;
    private static final int DIGITS_FOR_WIDTH_8 = 32;
    private static final int DIGITS_FOR_WIDTH_16 = 64;
    private static final int DIGITS_FOR_WIDTH_24 = 96;
    private static final int DIGITS_FOR_WIDTH_32 = 128;
    private final CodepointMap<UnihexProvider.Glyph> glyphs;

    private UnihexProvider(final CodepointMap<UnihexProvider.Glyph> glyphs) {
        this.glyphs = glyphs;
    }

    @Override
    public @Nullable UnbakedGlyph getGlyph(final int codepoint) {
        return this.glyphs.get(codepoint);
    }

    @Override
    public IntSet getSupportedGlyphs() {
        return this.glyphs.keySet();
    }

    @VisibleForTesting
    static void unpackBitsToBytes(final IntBuffer output, final int value, final int left, final int right) {
        int startBit = 32 - left - 1;
        int endBit = 32 - right - 1;

        for (int i = startBit; i >= endBit; i--) {
            if (i < 32 && i >= 0) {
                boolean isSet = (value >> i & 1) != 0;
                output.put(isSet ? -1 : 0);
            } else {
                output.put(0);
            }
        }
    }

    private static void unpackBitsToBytes(final IntBuffer output, final UnihexProvider.LineData data, final int left, final int right) {
        for (int i = 0; i < 16; i++) {
            int line = data.line(i);
            unpackBitsToBytes(output, line, left, right);
        }
    }

    @VisibleForTesting
    static void readFromStream(final InputStream input, final UnihexProvider.ReaderOutput output) throws IOException {
        int line = 0;
        ByteList buffer = new ByteArrayList(128);

        while (true) {
            boolean foundColon = copyUntil(input, buffer, 58);
            int codepointDigitCount = buffer.size();
            if (codepointDigitCount == 0 && !foundColon) {
                return;
            }

            if (!foundColon || codepointDigitCount != 4 && codepointDigitCount != 5 && codepointDigitCount != 6) {
                throw new IllegalArgumentException("Invalid entry at line " + line + ": expected 4, 5 or 6 hex digits followed by a colon");
            }

            int codepoint = 0;

            for (int i = 0; i < codepointDigitCount; i++) {
                codepoint = codepoint << 4 | decodeHex(line, buffer.getByte(i));
            }

            buffer.clear();
            copyUntil(input, buffer, 10);
            int dataDigitCount = buffer.size();

            UnihexProvider.LineData contents = switch (dataDigitCount) {
                case 32 -> UnihexProvider.ByteContents.read(line, buffer);
                case 64 -> UnihexProvider.ShortContents.read(line, buffer);
                case 96 -> UnihexProvider.IntContents.read24(line, buffer);
                case 128 -> UnihexProvider.IntContents.read32(line, buffer);
                default -> throw new IllegalArgumentException(
                    "Invalid entry at line " + line + ": expected hex number describing (8,16,24,32) x 16 bitmap, followed by a new line"
                );
            };
            output.accept(codepoint, contents);
            line++;
            buffer.clear();
        }
    }

    private static int decodeHex(final int line, final ByteList input, final int index) {
        return decodeHex(line, input.getByte(index));
    }

    private static int decodeHex(final int line, final byte b) {
        return switch (b) {
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
            default -> throw new IllegalArgumentException("Invalid entry at line " + line + ": expected hex digit, got " + (char)b);
            case 65 -> 10;
            case 66 -> 11;
            case 67 -> 12;
            case 68 -> 13;
            case 69 -> 14;
            case 70 -> 15;
        };
    }

    private static boolean copyUntil(final InputStream input, final ByteList output, final int delimiter) throws IOException {
        while (true) {
            int b = input.read();
            if (b == -1) {
                return false;
            }

            if (b == delimiter) {
                return true;
            }

            output.add((byte)b);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private record ByteContents(byte[] contents) implements UnihexProvider.LineData {
        @Override
        public int line(final int index) {
            return this.contents[index] << 24;
        }

        private static UnihexProvider.LineData read(final int line, final ByteList input) {
            byte[] content = new byte[16];
            int pos = 0;

            for (int i = 0; i < 16; i++) {
                int n1 = UnihexProvider.decodeHex(line, input, pos++);
                int n0 = UnihexProvider.decodeHex(line, input, pos++);
                byte v = (byte)(n1 << 4 | n0);
                content[i] = v;
            }

            return new UnihexProvider.ByteContents(content);
        }

        @Override
        public int bitWidth() {
            return 8;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Definition implements GlyphProviderDefinition {
        public static final MapCodec<UnihexProvider.Definition> CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                    Identifier.CODEC.fieldOf("hex_file").forGetter(o -> o.hexFile),
                    UnihexProvider.OverrideRange.CODEC.listOf().optionalFieldOf("size_overrides", List.of()).forGetter(o -> o.sizeOverrides)
                )
                .apply(i, UnihexProvider.Definition::new)
        );
        private final Identifier hexFile;
        private final List<UnihexProvider.OverrideRange> sizeOverrides;

        private Definition(final Identifier hexFile, final List<UnihexProvider.OverrideRange> sizeOverrides) {
            this.hexFile = hexFile;
            this.sizeOverrides = sizeOverrides;
        }

        @Override
        public GlyphProviderType type() {
            return GlyphProviderType.UNIHEX;
        }

        @Override
        public Either<GlyphProviderDefinition.Loader, GlyphProviderDefinition.Reference> unpack() {
            return Either.left(this::load);
        }

        private GlyphProvider load(final ResourceManager resourceManager) throws IOException {
            try (InputStream raw = resourceManager.open(this.hexFile)) {
                return this.loadData(raw);
            }
        }

        private UnihexProvider loadData(final InputStream zipFile) throws IOException {
            CodepointMap<UnihexProvider.LineData> bits = new CodepointMap<>(UnihexProvider.LineData[]::new, UnihexProvider.LineData[][]::new);
            UnihexProvider.ReaderOutput output = bits::put;

            try (ZipInputStream zis = new ZipInputStream(zipFile)) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    String name = entry.getName();
                    if (name.endsWith(".hex")) {
                        UnihexProvider.LOGGER.info("Found {}, loading", name);
                        UnihexProvider.readFromStream(new FastBufferedInputStream(zis), output);
                    }
                }

                CodepointMap<UnihexProvider.Glyph> glyphs = new CodepointMap<>(UnihexProvider.Glyph[]::new, UnihexProvider.Glyph[][]::new);

                for (UnihexProvider.OverrideRange sizeOverride : this.sizeOverrides) {
                    int from = sizeOverride.from;
                    int to = sizeOverride.to;
                    UnihexProvider.Dimensions size = sizeOverride.dimensions;

                    for (int c = from; c <= to; c++) {
                        UnihexProvider.LineData codepointBits = bits.remove(c);
                        if (codepointBits != null) {
                            glyphs.put(c, new UnihexProvider.Glyph(codepointBits, size.left, size.right));
                        }
                    }
                }

                bits.forEach((codepoint, glyphBits) -> {
                    int packedSize = glyphBits.calculateWidth();
                    int left = UnihexProvider.Dimensions.left(packedSize);
                    int right = UnihexProvider.Dimensions.right(packedSize);
                    glyphs.put(codepoint, new UnihexProvider.Glyph(glyphBits, left, right));
                });
                return new UnihexProvider(glyphs);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record Dimensions(int left, int right) {
        public static final MapCodec<UnihexProvider.Dimensions> MAP_CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                    Codec.INT.fieldOf("left").forGetter(UnihexProvider.Dimensions::left),
                    Codec.INT.fieldOf("right").forGetter(UnihexProvider.Dimensions::right)
                )
                .apply(i, UnihexProvider.Dimensions::new)
        );
        public static final Codec<UnihexProvider.Dimensions> CODEC = MAP_CODEC.codec();

        public int pack() {
            return pack(this.left, this.right);
        }

        public static int pack(final int left, final int right) {
            return (left & 0xFF) << 8 | right & 0xFF;
        }

        public static int left(final int packed) {
            return (byte)(packed >> 8);
        }

        public static int right(final int packed) {
            return (byte)packed;
        }
    }

    @OnlyIn(Dist.CLIENT)
    private record Glyph(UnihexProvider.LineData contents, int left, int right) implements UnbakedGlyph {
        public int width() {
            return this.right - this.left + 1;
        }

        @Override
        public GlyphInfo info() {
            return new GlyphInfo() {
                @Override
                public float getAdvance() {
                    return Glyph.this.width() / 2.0F + 1.0F;
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
        public BakedGlyph bake(final UnbakedGlyph.Stitcher stitcher) {
            return stitcher.stitch(
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
                    public void upload(final int x, final int y, final GpuTexture texture) {
                        IntBuffer targetBuffer = MemoryUtil.memAllocInt(Glyph.this.width() * 16);
                        UnihexProvider.unpackBitsToBytes(targetBuffer, Glyph.this.contents, Glyph.this.left, Glyph.this.right);
                        targetBuffer.rewind();
                        RenderSystem.getDevice()
                            .createCommandEncoder()
                            .writeToTexture(texture, MemoryUtil.memByteBuffer(targetBuffer), 0, 0, x, y, Glyph.this.width(), 16);
                        MemoryUtil.memFree(targetBuffer);
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
    private record IntContents(int[] contents, int bitWidth) implements UnihexProvider.LineData {
        private static final int SIZE_24 = 24;

        @Override
        public int line(final int index) {
            return this.contents[index];
        }

        private static UnihexProvider.LineData read24(final int line, final ByteList input) {
            int[] content = new int[16];
            int mask = 0;
            int pos = 0;

            for (int i = 0; i < 16; i++) {
                int n5 = UnihexProvider.decodeHex(line, input, pos++);
                int n4 = UnihexProvider.decodeHex(line, input, pos++);
                int n3 = UnihexProvider.decodeHex(line, input, pos++);
                int n2 = UnihexProvider.decodeHex(line, input, pos++);
                int n1 = UnihexProvider.decodeHex(line, input, pos++);
                int n0 = UnihexProvider.decodeHex(line, input, pos++);
                int v = n5 << 20 | n4 << 16 | n3 << 12 | n2 << 8 | n1 << 4 | n0;
                content[i] = v << 8;
                mask |= v;
            }

            return new UnihexProvider.IntContents(content, 24);
        }

        public static UnihexProvider.LineData read32(final int line, final ByteList input) {
            int[] content = new int[16];
            int mask = 0;
            int pos = 0;

            for (int i = 0; i < 16; i++) {
                int n7 = UnihexProvider.decodeHex(line, input, pos++);
                int n6 = UnihexProvider.decodeHex(line, input, pos++);
                int n5 = UnihexProvider.decodeHex(line, input, pos++);
                int n4 = UnihexProvider.decodeHex(line, input, pos++);
                int n3 = UnihexProvider.decodeHex(line, input, pos++);
                int n2 = UnihexProvider.decodeHex(line, input, pos++);
                int n1 = UnihexProvider.decodeHex(line, input, pos++);
                int n0 = UnihexProvider.decodeHex(line, input, pos++);
                int v = n7 << 28 | n6 << 24 | n5 << 20 | n4 << 16 | n3 << 12 | n2 << 8 | n1 << 4 | n0;
                content[i] = v;
                mask |= v;
            }

            return new UnihexProvider.IntContents(content, 32);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public interface LineData {
        int line(int index);

        int bitWidth();

        default int mask() {
            int mask = 0;

            for (int i = 0; i < 16; i++) {
                mask |= this.line(i);
            }

            return mask;
        }

        default int calculateWidth() {
            int mask = this.mask();
            int bitWidth = this.bitWidth();
            int left;
            int right;
            if (mask == 0) {
                left = 0;
                right = bitWidth;
            } else {
                left = Integer.numberOfLeadingZeros(mask);
                right = 32 - Integer.numberOfTrailingZeros(mask) - 1;
            }

            return UnihexProvider.Dimensions.pack(left, right);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private record OverrideRange(int from, int to, UnihexProvider.Dimensions dimensions) {
        private static final Codec<UnihexProvider.OverrideRange> RAW_CODEC = RecordCodecBuilder.create(
            i -> i.group(
                    ExtraCodecs.CODEPOINT.fieldOf("from").forGetter(UnihexProvider.OverrideRange::from),
                    ExtraCodecs.CODEPOINT.fieldOf("to").forGetter(UnihexProvider.OverrideRange::to),
                    UnihexProvider.Dimensions.MAP_CODEC.forGetter(UnihexProvider.OverrideRange::dimensions)
                )
                .apply(i, UnihexProvider.OverrideRange::new)
        );
        public static final Codec<UnihexProvider.OverrideRange> CODEC = RAW_CODEC.validate(
            o -> o.from >= o.to ? DataResult.error(() -> "Invalid range: [" + o.from + ";" + o.to + "]") : DataResult.success(o)
        );
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    public interface ReaderOutput {
        void accept(int codepoint, UnihexProvider.LineData glyph);
    }

    @OnlyIn(Dist.CLIENT)
    private record ShortContents(short[] contents) implements UnihexProvider.LineData {
        @Override
        public int line(final int index) {
            return this.contents[index] << 16;
        }

        private static UnihexProvider.LineData read(final int line, final ByteList input) {
            short[] content = new short[16];
            int pos = 0;

            for (int i = 0; i < 16; i++) {
                int n3 = UnihexProvider.decodeHex(line, input, pos++);
                int n2 = UnihexProvider.decodeHex(line, input, pos++);
                int n1 = UnihexProvider.decodeHex(line, input, pos++);
                int n0 = UnihexProvider.decodeHex(line, input, pos++);
                short v = (short)(n3 << 12 | n2 << 8 | n1 << 4 | n0);
                content[i] = v;
            }

            return new UnihexProvider.ShortContents(content);
        }

        @Override
        public int bitWidth() {
            return 16;
        }
    }
}