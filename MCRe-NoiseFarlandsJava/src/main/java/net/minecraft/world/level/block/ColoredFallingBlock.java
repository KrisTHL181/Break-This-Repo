package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class ColoredFallingBlock extends FallingBlock {
    public static final MapCodec<ColoredFallingBlock> CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(ColorRGBA.CODEC.fieldOf("falling_dust_color").forGetter(b -> b.dustColor), propertiesCodec()).apply(i, ColoredFallingBlock::new)
    );
    protected final ColorRGBA dustColor;

    @Override
    public MapCodec<? extends ColoredFallingBlock> codec() {
        return CODEC;
    }

    public ColoredFallingBlock(final ColorRGBA dustColor, final BlockBehaviour.Properties properties) {
        super(properties);
        this.dustColor = dustColor;
    }

    @Override
    public int getDustColor(final BlockState blockState, final BlockGetter level, final BlockPos pos) {
        return this.dustColor.rgba();
    }
}