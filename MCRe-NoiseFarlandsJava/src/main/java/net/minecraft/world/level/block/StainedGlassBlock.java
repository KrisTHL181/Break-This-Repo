package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class StainedGlassBlock extends TransparentBlock implements BeaconBeamBlock {
    public static final MapCodec<StainedGlassBlock> CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(DyeColor.CODEC.fieldOf("color").forGetter(StainedGlassBlock::getColor), propertiesCodec()).apply(i, StainedGlassBlock::new)
    );
    private final DyeColor color;

    @Override
    public MapCodec<StainedGlassBlock> codec() {
        return CODEC;
    }

    public StainedGlassBlock(final DyeColor color, final BlockBehaviour.Properties properties) {
        super(properties);
        this.color = color;
    }

    @Override
    public DyeColor getColor() {
        return this.color;
    }
}