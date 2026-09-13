package net.minecraft.world.level.levelgen;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import java.util.function.Function;
import net.minecraft.world.level.dimension.DimensionType;

public interface VerticalAnchor {
    Codec<VerticalAnchor> CODEC = Codec.xor(VerticalAnchor.Absolute.CODEC, Codec.xor(VerticalAnchor.AboveBottom.CODEC, VerticalAnchor.BelowTop.CODEC))
        .xmap(VerticalAnchor::merge, VerticalAnchor::split);
    VerticalAnchor BOTTOM = aboveBottom(0);
    VerticalAnchor TOP = belowTop(0);

    static VerticalAnchor absolute(final int value) {
        return new VerticalAnchor.Absolute(value);
    }

    static VerticalAnchor aboveBottom(final int offset) {
        return new VerticalAnchor.AboveBottom(offset);
    }

    static VerticalAnchor belowTop(final int offset) {
        return new VerticalAnchor.BelowTop(offset);
    }

    static VerticalAnchor bottom() {
        return BOTTOM;
    }

    static VerticalAnchor top() {
        return TOP;
    }

    private static VerticalAnchor merge(final Either<VerticalAnchor.Absolute, Either<VerticalAnchor.AboveBottom, VerticalAnchor.BelowTop>> either) {
        return either.map(Function.identity(), Either::unwrap);
    }

    private static Either<VerticalAnchor.Absolute, Either<VerticalAnchor.AboveBottom, VerticalAnchor.BelowTop>> split(final VerticalAnchor anchor) {
        return anchor instanceof VerticalAnchor.Absolute absolute
            ? Either.left(absolute)
            : Either.right(anchor instanceof VerticalAnchor.AboveBottom aboveBottom ? Either.left(aboveBottom) : Either.right((VerticalAnchor.BelowTop)anchor));
    }

    int resolveY(final WorldGenerationContext heightAccessor);

    record AboveBottom(int offset) implements VerticalAnchor {
        public static final Codec<VerticalAnchor.AboveBottom> CODEC = Codec.intRange(DimensionType.MIN_Y, DimensionType.MAX_Y)
            .fieldOf("above_bottom")
            .xmap(VerticalAnchor.AboveBottom::new, VerticalAnchor.AboveBottom::offset)
            .codec();

        @Override
        public int resolveY(final WorldGenerationContext heightAccessor) {
            return heightAccessor.getMinGenY() + this.offset;
        }

        @Override
        public String toString() {
            return this.offset + " above bottom";
        }
    }

    record Absolute(int y) implements VerticalAnchor {
        public static final Codec<VerticalAnchor.Absolute> CODEC = Codec.intRange(DimensionType.MIN_Y, DimensionType.MAX_Y)
            .fieldOf("absolute")
            .xmap(VerticalAnchor.Absolute::new, VerticalAnchor.Absolute::y)
            .codec();

        @Override
        public int resolveY(final WorldGenerationContext heightAccessor) {
            return this.y;
        }

        @Override
        public String toString() {
            return this.y + " absolute";
        }
    }

    record BelowTop(int offset) implements VerticalAnchor {
        public static final Codec<VerticalAnchor.BelowTop> CODEC = Codec.intRange(DimensionType.MIN_Y, DimensionType.MAX_Y)
            .fieldOf("below_top")
            .xmap(VerticalAnchor.BelowTop::new, VerticalAnchor.BelowTop::offset)
            .codec();

        @Override
        public int resolveY(final WorldGenerationContext heightAccessor) {
            return heightAccessor.getGenDepth() - 1 + heightAccessor.getMinGenY() - this.offset;
        }

        @Override
        public String toString() {
            return this.offset + " below top";
        }
    }
}