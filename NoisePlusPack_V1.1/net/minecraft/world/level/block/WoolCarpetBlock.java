package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class WoolCarpetBlock extends CarpetBlock {
   public static final MapCodec<WoolCarpetBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_422149_ -> p_422149_.group(DyeColor.CODEC.fieldOf("color").forGetter(WoolCarpetBlock::getColor), propertiesCodec())
         .apply(p_422149_, WoolCarpetBlock::new)
   );
   private final DyeColor color;

   @Override
   public MapCodec<WoolCarpetBlock> codec() {
      return CODEC;
   }

   protected WoolCarpetBlock(DyeColor p_58291_, BlockBehaviour.Properties p_58292_) {
      super(p_58292_);
      this.color = p_58291_;
   }

   public DyeColor getColor() {
      return this.color;
   }
}
