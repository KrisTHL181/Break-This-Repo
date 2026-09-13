package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class StainedGlassPaneBlock extends IronBarsBlock implements BeaconBeamBlock {
    public static final MapCodec<StainedGlassPaneBlock> CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(DyeColor.CODEC.fieldOf("color").forGetter(StainedGlassPaneBlock::getColor), propertiesCodec()).apply(i, StainedGlassPaneBlock::new)
    );
    private final DyeColor color;

    @Override
    public MapCodec<StainedGlassPaneBlock> codec() {
        return CODEC;
    }

    public StainedGlassPaneBlock(final DyeColor color, final BlockBehaviour.Properties properties) {
        super(properties);
        this.color = color;
        this.registerDefaultState(
            this.stateDefinition.any().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false).setValue(WATERLOGGED, false)
        );
    }

    @Override
    public DyeColor getColor() {
        return this.color;
    }
}