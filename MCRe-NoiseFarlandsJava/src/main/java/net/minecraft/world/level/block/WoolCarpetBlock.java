package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class WoolCarpetBlock extends CarpetBlock {
    public static final MapCodec<WoolCarpetBlock> CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(DyeColor.CODEC.fieldOf("color").forGetter(WoolCarpetBlock::getColor), propertiesCodec()).apply(i, WoolCarpetBlock::new)
    );
    private final DyeColor color;

    @Override
    public MapCodec<WoolCarpetBlock> codec() {
        return CODEC;
    }

    protected WoolCarpetBlock(final DyeColor color, final BlockBehaviour.Properties properties) {
        super(properties);
        this.color = color;
    }

    public DyeColor getColor() {
        return this.color;
    }
}