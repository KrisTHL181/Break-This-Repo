package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class NoiseThresholdProvider extends NoiseBasedStateProvider {
   public static final MapCodec<NoiseThresholdProvider> CODEC = RecordCodecBuilder.mapCodec(
      p_191486_ -> noiseCodec(p_191486_)
         .and(
            p_191486_.group(
               Codec.floatRange(-1.0F, 1.0F).fieldOf("threshold").forGetter(p_191494_ -> p_191494_.threshold),
               Codec.floatRange(0.0F, 1.0F).fieldOf("high_chance").forGetter(p_191492_ -> p_191492_.highChance),
               BlockState.CODEC.fieldOf("default_state").forGetter(p_191490_ -> p_191490_.defaultState),
               ExtraCodecs.nonEmptyList(BlockState.CODEC.listOf()).fieldOf("low_states").forGetter(p_191488_ -> p_191488_.lowStates),
               ExtraCodecs.nonEmptyList(BlockState.CODEC.listOf()).fieldOf("high_states").forGetter(p_191481_ -> p_191481_.highStates)
            )
         )
         .apply(p_191486_, NoiseThresholdProvider::new)
   );
   private final float threshold;
   private final float highChance;
   private final BlockState defaultState;
   private final List<BlockState> lowStates;
   private final List<BlockState> highStates;

   public NoiseThresholdProvider(
      long p_191471_,
      NormalNoise.NoiseParameters p_191472_,
      float p_191473_,
      float p_191474_,
      float p_191475_,
      BlockState p_191476_,
      List<BlockState> p_191477_,
      List<BlockState> p_191478_
   ) {
      super(p_191471_, p_191472_, p_191473_);
      this.threshold = p_191474_;
      this.highChance = p_191475_;
      this.defaultState = p_191476_;
      this.lowStates = p_191477_;
      this.highStates = p_191478_;
   }

   @Override
   protected BlockStateProviderType<?> type() {
      return BlockStateProviderType.NOISE_THRESHOLD_PROVIDER;
   }

   @Override
   public BlockState getState(RandomSource p_225916_, BlockPos p_225917_) {
      double d0 = this.getNoiseValue(p_225917_, this.scale);
      if (d0 < this.threshold) {
         return Util.getRandom(this.lowStates, p_225916_);
      } else {
         return p_225916_.nextFloat() < this.highChance ? Util.getRandom(this.highStates, p_225916_) : this.defaultState;
      }
   }
}
