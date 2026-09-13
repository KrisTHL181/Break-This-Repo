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
      p_422097_ -> p_422097_.group(ColorRGBA.CODEC.fieldOf("falling_dust_color").forGetter(p_309656_ -> p_309656_.dustColor), propertiesCodec())
         .apply(p_422097_, ColoredFallingBlock::new)
   );
   protected final ColorRGBA dustColor;

   @Override
   public MapCodec<? extends ColoredFallingBlock> codec() {
      return CODEC;
   }

   public ColoredFallingBlock(ColorRGBA p_310631_, BlockBehaviour.Properties p_312848_) {
      super(p_312848_);
      this.dustColor = p_310631_;
   }

   @Override
   public int getDustColor(BlockState p_309534_, BlockGetter p_310029_, BlockPos p_312470_) {
      return this.dustColor.rgba();
   }
}
