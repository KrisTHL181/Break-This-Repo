package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ColorRGBA;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.sounds.AmbientDesertBlockSoundsPlayer;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class SandBlock extends ColoredFallingBlock {
   public static final MapCodec<SandBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_422121_ -> p_422121_.group(ColorRGBA.CODEC.fieldOf("falling_dust_color").forGetter(p_395369_ -> p_395369_.dustColor), propertiesCodec())
         .apply(p_422121_, SandBlock::new)
   );

   @Override
   public MapCodec<SandBlock> codec() {
      return CODEC;
   }

   public SandBlock(ColorRGBA p_395563_, BlockBehaviour.Properties p_393836_) {
      super(p_395563_, p_393836_);
   }

   @Override
   public void animateTick(BlockState p_393155_, Level p_394075_, BlockPos p_394564_, RandomSource p_392109_) {
      super.animateTick(p_393155_, p_394075_, p_394564_, p_392109_);
      AmbientDesertBlockSoundsPlayer.playAmbientSandSounds(p_394075_, p_394564_, p_392109_);
   }
}
